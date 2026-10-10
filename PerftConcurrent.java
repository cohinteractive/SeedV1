public class PerftConcurrent {

    public static void main(String[] args) throws InterruptedException {
        runAll();
    }

    private final long[][] movesBuffer = new long[64][128];
    private final long[][] boardBuffer = new long[64][6];

    private static final record Position(String id, String name, String fen, int depth, long expectedNodes) {};

    private static final Position[] POSITIONS = {
        new Position(
            "initial-position", "1. Initial position",
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            6, 119060324L
        ),
        new Position(
            "kiwipete", "2. Kiwipete",
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",
            6, 8031647685L
        ),
        new Position(
            "rook-and-pawn-endgame", "3.",
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            7, 178633661L
        ),
        new Position(
            "promotion-and-castling-tactics", "4.",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            6, 706045033L
        ),
        new Position(
            "knight-check-and-castling", "5.",
            "rnbqkb1r/pp1p1ppp/2p5/4P3/2B5/8/PPP1NnPP/RNBQK2R w KQkq - 0 6",
            3, 53392L
        ),
        new Position(
            "complex-middlegame", "6.",
            "r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - - 0 10",
            6, 6923051137L
        ),
        new Position(
            "illegal-en-passant-discovered-check", "7.",
            "8/5bk1/8/2Pp4/8/1K6/8/8 w - d6 0 1",
            6, 824064L
        ),
        new Position(
            "en-passant-capture-gives-check", "8. En passant capture gives check",
            "8/8/1k6/2b5/2pP4/8/5K2/8 b - d3 0 1",
            6, 1440467L
        ),
        new Position(
            "short-castling-gives-check", "9. Short castling gives check",
            "5k2/8/8/8/8/8/8/4K2R w K - 0 1",
            6, 661072L
        ),
        new Position(
            "long-castling-gives-check", "10. Long castling gives check",
            "3k4/8/8/8/8/8/8/R3K3 w Q - 0 1",
            6, 803711L
        ),
        new Position(
            "castling", "11. Castling",
            "r3k2r/1b4bq/8/8/8/8/7B/R3K2R w KQkq - 0 1",
            4, 1274206L
        ),
        new Position(
            "castling-prevented", "12. Castling prevented",
            "r3k2r/8/3Q4/8/8/5q2/8/R3K2R b KQkq - 0 1",
            4, 1720476L
        ),
        new Position(
            "promote-out-of-check", "13. Promote out of check",
            "2K2r2/4P3/8/8/8/8/8/3k4 w - - 0 1",
            6, 3821001L
        ),
        new Position(
            "discovered-check", "14. Discovered check",
            "8/8/1P2K3/8/2n5/1q6/8/5k2 b - - 0 1",
            5, 1004658L
        ),
        new Position(
            "promotion-gives-check", "15. Promotion gives check",
            "4k3/1P6/8/8/8/8/K7/8 w - - 0 1",
            6, 217342L
        ),
        new Position(
            "underpromotion-gives-check", "16. Underpromotion gives check",
            "8/P1k5/K7/8/8/8/8/8 w - - 0 1",
            6, 92683L
        ),
        new Position(
            "self-stalemate", "17. Self stalemate",
            "K1k5/8/P7/8/8/8/8/8 w - - 0 1",
            6, 2217L
        ),
        new Position(
            "stalemate-checkmate", "18. Stalemate/Checkmate",
            "8/k1P5/8/1K6/8/8/8/8 w - - 0 1",
            7, 567584L
        ),
        new Position(
            "double-check", "19. Double check",
            "8/8/2k5/5q2/5n2/8/5K2/8 b - - 0 1",
            4, 23527L
        ),
        new Position(
            "new-position", "20. New position",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            5, 89941194L
        )
    };

    private static void runAll() throws InterruptedException {
        for(int i = 0; i < POSITIONS.length; i ++) {
            Position position = POSITIONS[i];
            System.out.println("\n" + position.name + "\n\"" + position.fen + "\"");
            runPosition(position);    
        }
    }

    private static void runPosition(Position position) throws InterruptedException {
        final PerftConcurrent serial = new PerftConcurrent();
        long totalTime = 0L;
        long totalNodes = 0L;
        for(int depth = 1; depth <= position.depth; depth ++) {
            System.out.print("Depth " + depth + "/" + position.depth + ": ");
            final long[] board = Board.fromFen(position.fen);
            long time = System.nanoTime();
            long nodes = 0L;
            if(depth == position.depth && depth > 1) {
                nodes = perftParallel(board, depth);
            } else {
                nodes = serial.perft(board[0], board[1], board[2], board[3], (int) board[4], depth, 0);
            }
            time = (System.nanoTime() - time);
            long ms = time / 1_000_000L;
            System.out.println(nodes + " nodes in " + ms + " ms");
            totalTime += time;
            totalNodes += nodes;
        }
        final long nps = totalTime == 0 ? 0 : (long) (totalNodes / (totalTime / 1_000_000_000.0));
        System.out.println("Nodes per second: " + nps);
    }
    
    private long perft(long board0, long board1, long board2, long board3, int status, int depth, int ply) {
        if(depth == 0) return 1L;
        final long[] moves = movesBuffer[ply];
        if(depth == 1) return Gen.genTactical(board0, board1, board2, board3, status, moves) +
            Gen.genQuiet(board0, board1, board2, board3, status, moves);
        long nodes = 0L;
        final int newPly = ply + 1;
        final int newDepth = depth - 1;
        final long[] board = boardBuffer[ply];
        for(int i = 0; i < Gen.genTactical(board0, board1, board2, board3, status, moves); i ++) {
            Board.makeMoveIntoFast(board0, board1, board2, board3, status, moves[i], board);
            nodes += perft(board[0], board[1], board[2], board[3], (int) board[4], newDepth, newPly);
        }
        for(int i = 0; i < Gen.genQuiet(board0, board1, board2, board3, status, moves); i ++) {
            Board.makeMoveIntoFast(board0, board1, board2, board3, status, moves[i], board);
            nodes += perft(board[0], board[1], board[2], board[3], (int) board[4], newDepth, newPly);
        }
        return nodes;
    }

    private static long perftParallel(long[] root, int depth) throws InterruptedException {
        final long[] tactical = new long[128];
        final long[] quiet = new long[128];

        final int tacticalCount = Gen.genTactical(root[0], root[1], root[2], root[3], (int) root[4], tactical);
        final int quietCount = Gen.genQuiet(root[0], root[1], root[2], root[3], (int) root[4], quiet);
        final int moveCount = tacticalCount + quietCount;
        if(moveCount == 0) return 0L;
        final long[] moves = new long[moveCount];
        System.arraycopy(tactical, 0, moves, 0, tacticalCount);
        System.arraycopy(quiet, 0, moves, tacticalCount, quietCount);
        final int count = Math.min(6, moveCount);
        final PerftWorker[] workers = new PerftWorker[count];
        for(int i = 0; i < count; i ++) {
            workers[i] = new PerftWorker(root, moves, depth, i, count);
        }
        for(int i = 0; i < count; i ++) {
            workers[i].start();
        }
        for(int i = 0; i < count; i ++) {
            workers[i].join();
        }
        long nodes = 0L;
        for(int i = 0; i < count; i ++) {
            if(workers[i].failure != null) throw new IllegalStateException("Perft worker " + i + " failed", workers[i].failure);
            nodes += workers[i].nodes;
        }
        return nodes;
    }

    private static final class PerftWorker extends Thread {
        private final PerftConcurrent engine = new PerftConcurrent();
        private final long[] root;
        private final long[] moves;
        private final int depth;
        private final int id;
        private final int count;
        private long nodes = 0L;
        private Throwable failure;
        private PerftWorker(long[] root, long[] moves, int depth, int id, int count) {
            this.root = root;
            this.moves = moves;
            this.depth = depth;
            this.id = id;
            this.count = count;
        }

        @Override
        public void run() {
            try {
                final long[] board = new long[6];
                for(int i = id; i < moves.length; i += count) {
                    Board.makeMoveIntoFast(root[0], root[1], root[2], root[3], (int) root[4], moves[i], board);
                    nodes += engine.perft(board[0], board[1], board[2], board[3], (int) board[4], depth - 1, 0);
                }
            } catch (Throwable t) {
                failure = t;
            }
        }
    }

}

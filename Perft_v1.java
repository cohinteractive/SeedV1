/**
 * Standalone SeedV1 perft suite (JDK 21); no arguments run the full SeedV6 suite.
 * Compile from the repository root, explicitly selecting the required sources:
 *   javac -d <classes> Perft_v1.java Board.java Gen.java Fen.java Pext.java Zobrist.java
 *   java -cp <classes> Perft_v1
 *   java -cp <classes> Perft_v1 0 6 --warmup 5 --repeat 5
 *   java -cp <classes> Perft_v1 1 4 --divide
 *
 * Suite data and console presentation follow SeedV6 tools/perft/Perft and
 * DefaultPerftPositionLibrary. Array indices are 0..19; the original display
 * labels remain 1..20. Each imported position has one configured expectation.
 *
 * Traversal is a single-threaded, recursive depth-first search using SeedV1's
 * legal tactical/quiet generators. Each ply owns a reusable Frame, move buffer
 * and child board; traversal state lives on the call stack. No per-node allocation,
 * list copying, unmake, hash cache, evaluation or draw adjudication is used.
 * Gen handles checks/pins itself; no SeedV6 legality machinery is needed.
 * Board.makeMoveIntoFast writes slots 0..4; its unused key slot is never read.
 * At depth one, generated legal moves are counted without making leaf moves.
 *
 * Depth timings exclude workspace allocation and output, as in SeedV6. The
 * first suite run includes class/table initialization and JIT effects; use
 * --warmup and --repeat on a selected position for steady-state measurements.
 * Counts must fit signed long; custom FENs must be valid orthodox positions.
 */
public final class Perft_v1 {
    private static final int MAX_DEPTH = 64;
    private static final int MAX_MOVES = 512;

    private static final class Position {
        final String id;
        final String name;
        final String fen;
        final int depth;
        final long expectedNodes;

        Position(String id, String name, String fen, int depth, long expectedNodes) {
            this.id = id;
            this.name = name;
            this.fen = fen;
            this.depth = depth;
            this.expectedNodes = expectedNodes;
        }
    }

    // Exact order, IDs, labels, FENs, default depths and expectations from SeedV6.
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

    private static final class Frame {
        final long[] moves = new long[MAX_MOVES];
        final long[] child = new long[Board.MAX_BITBOARDS];
    }

    private static final class Workspace {
        final Frame[] frames;

        Workspace(int depth) {
            frames = new Frame[Math.max(1, depth)];
            for(int ply = 0; ply < frames.length; ply ++) frames[ply] = new Frame();
        }
    }

    private static int countLegalMoves(long[] board, long[] moves) {
        final long b0 = board[0];
        final long b1 = board[1];
        final long b2 = board[2];
        final long b3 = board[3];
        final int status = (int) board[Board.STATUS];
        // Both generators write from index zero. No moves need retaining at a leaf.
        return Gen.genTactical(b0, b1, b2, b3, status, moves)
            + Gen.genQuiet(b0, b1, b2, b3, status, moves);
    }

    private static long count(long[] board, int depth, Workspace workspace) {
        return count(board, depth, workspace.frames, 0);
    }

    private static long count(long[] board, int depth, Frame[] frames, int ply) {
        if(depth == 0) return 1L;
        final Frame frame = frames[ply];
        if(depth == 1) return countLegalMoves(board, frame.moves);

        final long b0 = board[0];
        final long b1 = board[1];
        final long b2 = board[2];
        final long b3 = board[3];
        final int status = (int) board[Board.STATUS];
        long nodes = 0L;
        for(int phase = 0; phase < 2; phase ++) {
            // Finish tactical children before reusing this ply's buffer for quiet moves.
            final int size = phase == 0
                ? Gen.genTactical(b0, b1, b2, b3, status, frame.moves)
                : Gen.genQuiet(b0, b1, b2, b3, status, frame.moves);
            for(int i = 0; i < size; i ++) {
                Board.makeMoveIntoFast(b0, b1, b2, b3, status, frame.moves[i], frame.child);
                nodes += count(frame.child, depth - 1, frames, ply + 1);
            }
        }
        return nodes;
    }

    // Root buffers are separate from the reused count workspace, which starts at ply 0.
    // All child subtrees use the same recursive count; no traversal calls back here.
    private static long divide(long[] board, int depth) {
        if(depth == 0) {
            System.out.println("Total: 1 (depth zero; no root moves)");
            return 1L;
        }
        final long b0 = board[0];
        final long b1 = board[1];
        final long b2 = board[2];
        final long b3 = board[3];
        final int status = (int) board[Board.STATUS];
        final long[] moves = new long[MAX_MOVES];
        final long[] child = new long[Board.MAX_BITBOARDS];
        final Workspace workspace = new Workspace(depth - 1);
        long total = 0L;
        for(int phase = 0; phase < 2; phase ++) {
            int size = phase == 0
                ? Gen.genTactical(b0, b1, b2, b3, status, moves)
                : Gen.genQuiet(b0, b1, b2, b3, status, moves);
            for(int i = 0; i < size; i ++) {
                long move = moves[i];
                Board.makeMoveIntoFast(b0, b1, b2, b3, status, move, child);
                long nodes = count(child, depth - 1, workspace);
                System.out.println(moveText(move) + ": " + nodes);
                total += nodes;
            }
        }
        System.out.println("Total: " + total);
        return total;
    }

    private static String moveText(long move) {
        // SeedV1: from[0..5], to[6..11], promotion[12..15], piece color bit 3.
        int from = (int) move & 63;
        int to = (int) (move >>> 6) & 63;
        int promotion = (int) (move >>> 12) & 7;
        String text = "" + (char) ('a' + (from & 7)) + (char) ('1' + (from >>> 3))
            + (char) ('a' + (to & 7)) + (char) ('1' + (to >>> 3));
        return promotion == 0 ? text : text + " kqrbnp".charAt(promotion);
    }

    private static long runBoard(long[] board, int maxDepth) {
        Workspace workspace = new Workspace(maxDepth);
        long lastNodes = 0L;
        long lastElapsedNs = 0L;
        for(int depth = maxDepth == 0 ? 0 : 1; depth <= maxDepth; depth ++) {
            long start = System.nanoTime();
            long nodes = count(board, depth, workspace);
            long elapsedNs = System.nanoTime() - start;
            long elapsedMs = elapsedNs / 1_000_000L;
            lastNodes = nodes;
            lastElapsedNs = elapsedNs;
            System.out.println(
                "Depth " + depth + "/" + maxDepth + ": " +
                format(nodes) + " nodes in " + elapsedMs + " ms"
            );
        }
        // Floating-point scaling avoids overflowing long for large optional runs.
        long nps = lastElapsedNs > 0L
            ? (long) (lastNodes * 1_000_000_000.0 / lastElapsedNs) : lastNodes;
        System.out.println("Nodes per second: " + format(nps));
        return lastNodes;
    }

    private static long expectedAt(Position position, int depth) {
        return depth == 0 ? 1L : depth == position.depth ? position.expectedNodes : -1L;
    }

    private static void validateResult(long nodes, long expectedNodes) {
        if(expectedNodes < 0L) {
            System.out.println("Result: UNVERIFIED (no reference count for this depth/FEN)");
            return;
        }
        if(nodes == expectedNodes) {
            System.out.println("Result: PASSED");
            return;
        }
        long diff = nodes - expectedNodes;
        System.out.println(
            "Result: FAILED expected " + format(expectedNodes) +
            ", got " + format(nodes) + ", diff " + format(diff)
        );
        // Preserve SeedV6's failure line, but do not silently continue after a mismatch.
        throw new IllegalStateException("Perft mismatch; use --divide to diagnose this position/depth.");
    }

    private static long runCase(Position position, int depth) {
        System.out.println(position.name);
        System.out.println("\"" + position.fen + "\"");
        long[] board = Board.fromFen(position.fen);
        long start = System.nanoTime();
        long nodes = runBoard(board, depth);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        validateResult(nodes, expectedAt(position, depth));
        return elapsedMs;
    }

    private static void summary(long elapsedMs) {
        System.out.println("All positions complete.");
        System.out.println("Total elapsed: " + format(elapsedMs) + " ms");
    }

    private static void runSuite() {
        long totalElapsedMs = 0L;
        for(int i = 0; i < POSITIONS.length; i ++) {
            Position position = POSITIONS[i];
            totalElapsedMs += runCase(position, position.depth);
            if(i + 1 < POSITIONS.length) System.out.println();
        }
        summary(totalElapsedMs);
    }

    private static void warmUp(Position position, int depth, int repetitions) {
        long[] board = Board.fromFen(position.fen);
        Workspace workspace = new Workspace(depth);
        long expected = expectedAt(position, depth);
        for(int i = 0; i < repetitions; i ++) {
            long nodes = count(board, depth, workspace);
            if(expected >= 0L && nodes != expected) validateResult(nodes, expected);
        }
    }

    public static void main(String[] args) {
        try {
            run(args);
        } catch(IllegalArgumentException | IllegalStateException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if(args.length == 0 || (args.length == 1 && args[0].equals("--verify"))) {
            runSuite();
            return;
        }
        if(args.length == 1 && args[0].equals("--help")) {
            usage();
            return;
        }
        if(args.length == 1 && args[0].equals("--list")) {
            for(int i = 0; i < POSITIONS.length; i ++) {
                Position p = POSITIONS[i];
                System.out.println(i + " | " + p.id + " | " + p.name);
                System.out.println("\"" + p.fen + "\"");
                System.out.println("Depth " + p.depth + ": expected " + format(p.expectedNodes));
            }
            return;
        }

        int index = 0;
        Position position;
        int depth;
        if(args[index].equals("--fen")) {
            if(args.length < 3) throw new IllegalArgumentException("--fen requires a quoted FEN and depth");
            position = new Position("custom", "Custom position", args[1], -1, -1L);
            depth = integer(args[2], "depth", 0, MAX_DEPTH);
            index = 3;
        } else {
            int selection = integer(args[index ++], "position index", 0, POSITIONS.length - 1);
            position = POSITIONS[selection];
            depth = position.depth;
            if(index < args.length && !args[index].startsWith("--")) {
                depth = integer(args[index ++], "depth", 0, MAX_DEPTH);
            }
        }
        int warmup = 0;
        int repeat = 1;
        boolean divide = false;
        while(index < args.length) {
            String option = args[index ++];
            if(option.equals("--divide")) {
                divide = true;
            } else if(option.equals("--warmup") || option.equals("--repeat")) {
                if(index >= args.length) throw new IllegalArgumentException("Missing value for " + option);
                if(option.equals("--warmup")) warmup = integer(args[index ++], option, 0, Integer.MAX_VALUE);
                else repeat = integer(args[index ++], option, 1, Integer.MAX_VALUE);
            } else {
                throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        if(warmup > 0) warmUp(position, depth, warmup);
        long totalElapsedMs = 0L;
        for(int i = 0; i < repeat; i ++) {
            totalElapsedMs += runCase(position, depth);
            if(i + 1 < repeat) System.out.println();
        }
        summary(totalElapsedMs);
        if(divide) {
            System.out.println("Divide (separate, untimed traversal):");
            long[] board = Board.fromFen(position.fen);
            long total = divide(board, depth);
            long expected = expectedAt(position, depth);
            if(expected < 0L) expected = count(board, depth, new Workspace(depth));
            if(total != expected) validateResult(total, expected);
            System.out.println("Divide total matches ordinary perft.");
        }
    }

    private static int integer(String value, String label, int min, int max) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch(NumberFormatException exception) {
            throw new IllegalArgumentException(label + " must be an integer");
        }
        if(parsed < min || parsed > max) {
            throw new IllegalArgumentException(label + " must be in " + min + ".." + max);
        }
        return parsed;
    }

    private static String format(long value) {
        return String.format("%,d", value);
    }

    private static void usage() {
        System.out.println("Perft_v1                  Run all 20 SeedV6 positions in their original order.");
        System.out.println("Perft_v1 --verify         Same full suite, checking every configured expectation.");
        System.out.println("Perft_v1 --list           Show zero-based indices, original labels, FENs and expectations.");
        System.out.println("Perft_v1 INDEX [DEPTH] [--warmup N] [--repeat N] [--divide]");
        System.out.println("Perft_v1 --fen \"<six-field FEN>\" DEPTH [same options]");
        System.out.println("Defaults: configured depth, no warm-up, one run. Depth overrides allow 0..64.");
        System.out.println("Warm-ups are silent full-depth runs; repetitions print SeedV6-style results.");
        System.out.println("Overrides/custom FENs without an expectation are explicitly UNVERIFIED.");
        System.out.println("Counts must fit signed long. Custom FENs must describe legal orthodox positions.");
    }

    private Perft_v1() {}
}

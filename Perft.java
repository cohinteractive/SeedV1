public class Perft {

    public static void main(String[] args) {
        long[][] movesBuffer = new long[64][128];
        long[][] boardBuffer = new long[64][6];
        long[] board = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        long time = System.nanoTime();
        long count = perft(board[0], board[1], board[2], board[3], (int) board[4], 6, movesBuffer, boardBuffer, 0);
        time = (System.nanoTime() - time) / 1000000;
        long nps = count / time / 1000;
        System.out.println(count + " " + time + " " + nps);
    }
    
    private static long perft(long board0, long board1, long board2, long board3, int status, int depth, long[][] movesBuffer, long[][] boardBuffer, int ply) {
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
            nodes += perft(board[0], board[1], board[2], board[3], (int) board[4], newDepth, movesBuffer, boardBuffer, newPly);
        }
        for(int i = 0; i < Gen.genQuiet(board0, board1, board2, board3, status, moves); i ++) {
            Board.makeMoveIntoFast(board0, board1, board2, board3, status, moves[i], board);
            nodes += perft(board[0], board[1], board[2], board[3], (int) board[4], newDepth, movesBuffer, boardBuffer, newPly);
        }
        return nodes;
    }

}

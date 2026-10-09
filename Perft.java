public class Perft {
    
    private static long perft(long board0, long board1, long board2, long board3, int status, int depth, long[] moves) {
        if(depth == 0) return 1L;
        if(depth == 1) return Gen.genTactical(board0, board1, board2, board3, status, moves) +
            Gen.genQuiet(board0, board1, board2, board3, status, moves);
        final int tacticalMoves = Gen.genTactical(board0, board1, board2, board3, status, moves);
        for(int i = 0; i < tacticalMoves; i ++) {

        }
        return 0L;
    }

}

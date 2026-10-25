import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

public class Eval {

    public Eval(int maxPly) {
        try { nnue = new Nnue(); } catch (IOException e) { throw new UncheckedIOException("Failed to load NNUE model", e); }
        acc = new int[maxPly + 1][512];
        placement = new long[maxPly + 1][4];
        ready = new boolean[maxPly + 1];
    }

    public int evaluate(long board0, long board1, long board2, long board3, int status) {
        return nnue.evaluate(board0, board1, board2, board3, status);
    }

    public void root(long board0, long board1, long board2, long board3) {
        Arrays.fill(ready, false);
        nnue.rebuild(board0, board1, board2, board3, acc[0]);
        remember(board0, board1, board2, board3, 0);
    }

    public void child(long pboard0, long pboard1, long pboard2, long pboard3, long cboard0, long cboard1, long cboard2, long cboard3, int ply) {
        check(pboard0, pboard1, pboard2, pboard3, ply);
        nnue.update(pboard0, pboard1, pboard2, pboard3, cboard0, cboard1, cboard2, cboard3, acc[ply], acc[ply + 1]);
        remember(cboard0, cboard1, cboard2, cboard3, ply + 1);
    }

    public int evaluate(long board0, long board1, long board2, long board3, int status, int ply) {
        check(board0, board1, board2, board3, ply);
        return nnue.score(board0, board1, board2, board3, status, acc[ply]);
    }

    private final Nnue nnue;
    private final int[][] acc;
    private final long[][] placement;
    private final boolean[] ready;

    private void remember(long board0, long board1, long board2, long board3, int ply) {
        placement[ply][0] = board0;
        placement[ply][1] = board1;
        placement[ply][2] = board2;
        placement[ply][3] = board3;
        ready[ply] = true;
    }

    private void check(long board0, long board1, long board2, long board3, int ply) {
        if(!ready[ply])
            throw new IllegalStateException("Accumulator not prepared");
        if(placement[ply][0] != board0 || placement[ply][1] != board1 || placement[ply][2] != board2 || placement[ply][3] != board3)
            throw new IllegalStateException("Accumulator position mismatch");
    }

}
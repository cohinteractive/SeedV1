
import java.util.Arrays;

public class NnueLifecycleParity {

    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static final String E4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
    static final String E5 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2";
    static final String NF3 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq - 1 2";
    static final String NC3 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/2N5/PPPP1PPP/R1BQKBNR b KQkq - 1 2";
    static final String D4 = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1";
    static final String D5 = "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 2";

    static void step(Nnue nnue, Eval eval, int[][] acc,
                     long[] parent, long[] child, int ply, String name) {

        int[] saved = acc[ply].clone();
        int parentScore = eval.evaluate(
            parent[0], parent[1], parent[2], parent[3],
            (int) parent[4], ply);

        nnue.update(
            parent[0], parent[1], parent[2], parent[3],
            child[0], child[1], child[2], child[3],
            acc[ply], acc[ply + 1]);

        int[] rebuilt = new int[512];
        nnue.rebuild(child[0], child[1], child[2], child[3], rebuilt);

        if(!Arrays.equals(acc[ply + 1], rebuilt))
            throw new AssertionError(name + ": accumulator mismatch");

        if(!Arrays.equals(acc[ply], saved))
            throw new AssertionError(name + ": parent accumulator modified");

        eval.child(
            parent[0], parent[1], parent[2], parent[3],
            child[0], child[1], child[2], child[3], ply);

        int actual = eval.evaluate(
            child[0], child[1], child[2], child[3],
            (int) child[4], ply + 1);

        int expected = nnue.score(
            child[0], child[1], child[2], child[3],
            (int) child[4], rebuilt);

        if(actual != expected)
            throw new AssertionError(name + ": score mismatch");

        if(eval.evaluate(parent[0], parent[1], parent[2],
                         parent[3], (int) parent[4], ply) != parentScore)
            throw new AssertionError(name + ": parent score changed");

        System.out.println("PASS: " + name);
    }

    public static void main(String[] args) throws Exception {
        Nnue nnue = new Nnue();
        Eval eval = new Eval(8);
        int[][] acc = new int[9][512];

        long[] start = Board.fromFen(START);
        long[] e4 = Board.fromFen(E4);
        long[] e5 = Board.fromFen(E5);
        long[] nf3 = Board.fromFen(NF3);
        long[] nc3 = Board.fromFen(NC3);
        long[] d4 = Board.fromFen(D4);
        long[] d5 = Board.fromFen(D5);

        nnue.rebuild(start[0], start[1], start[2], start[3], acc[0]);
        eval.root(start[0], start[1], start[2], start[3]);

        step(nnue, eval, acc, start, e4, 0, "e4");
        step(nnue, eval, acc, e4, e5, 1, "e5");
        step(nnue, eval, acc, e5, nf3, 2, "Nf3");

        // Return to e5 and overwrite the previous ply-3 accumulator.
        step(nnue, eval, acc, e5, nc3, 2, "Nc3 sibling");

        // Return to the root and reuse ply 1.
        step(nnue, eval, acc, start, d4, 0, "d4 root sibling");
        step(nnue, eval, acc, d4, d5, 1, "d5 after sibling");

        System.out.println("All lifecycle tests passed.");
    }
}

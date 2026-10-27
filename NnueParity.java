
import java.util.Arrays;

public class NnueParity {

    static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    static final String E4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1";
    static final String D4 = "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3 0 1";
    static final String E5 = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6 0 2";

    static final String[][] TESTS = {
        {"Pawn push", START, E4},
        {"Another pawn push", START, D4},
        {"Black pawn push", E4, E5},
        {"Capture", "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1",
                    "4k3/8/8/3P4/8/8/8/4K3 b - - 0 1"},
        {"En passant", "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
                       "4k3/8/3P4/8/8/8/8/4K3 b - - 0 1"},
        {"Castling", "4k3/8/8/8/8/8/8/4K2R w K - 0 1",
                     "4k3/8/8/8/8/8/8/5RK1 b - - 1 1"},
        {"Promotion", "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
                      "Q3k3/8/8/8/8/8/8/4K3 b - - 0 1"},
        {"King same bucket", "4k3/8/8/8/8/8/K7/8 w - - 0 1",
                             "4k3/8/8/8/8/8/1K6/8 b - - 1 1"},
        {"White king mirror", "4k3/8/8/8/8/8/8/3K4 w - - 0 1",
                              "4k3/8/8/8/8/8/8/4K3 b - - 1 1"},
        {"Black king mirror", "4k3/8/8/8/8/8/8/4K3 b - - 0 1",
                              "3k4/8/8/8/8/8/8/4K3 w - - 1 2"},
        {"Null move", "4k3/8/8/8/8/8/8/4K3 w - - 0 1",
                      "4k3/8/8/8/8/8/8/4K3 b - - 0 1"}
    };

    public static void main(String[] args) throws Exception {
        Nnue nnue = new Nnue();
        Eval eval = new Eval(4);

        int[] parent = new int[512];
        int[] saved = new int[512];
        int[] incremental = new int[512];
        int[] rebuilt = new int[512];

        for(String[] test : TESTS) {
            long[] p = Board.fromFen(test[1]);
            long[] c = Board.fromFen(test[2]);

            nnue.rebuild(p[0], p[1], p[2], p[3], parent);
            System.arraycopy(parent, 0, saved, 0, 512);

            nnue.update(p[0], p[1], p[2], p[3],
                        c[0], c[1], c[2], c[3],
                        parent, incremental);

            nnue.rebuild(c[0], c[1], c[2], c[3], rebuilt);

            for(int i = 0; i < 512; i ++)
                if(incremental[i] != rebuilt[i])
                    throw new AssertionError(test[0] + ": accumulator mismatch at " + i
                        + " (" + incremental[i] + " vs " + rebuilt[i] + ")");

            if(!Arrays.equals(parent, saved))
                throw new AssertionError(test[0] + ": parent accumulator modified");

            eval.root(p[0], p[1], p[2], p[3]);
            eval.child(p[0], p[1], p[2], p[3],
                       c[0], c[1], c[2], c[3], 0);

            int actual = eval.evaluate(c[0], c[1], c[2], c[3], (int)c[4], 1);
            int expected = nnue.score(c[0], c[1], c[2], c[3], (int)c[4], rebuilt);

            if(actual != expected)
                throw new AssertionError(test[0] + ": score mismatch");

            System.out.println("PASS: " + test[0]);
        }

        System.out.println("All " + TESTS.length + " NNUE parity tests passed.");
    }
}

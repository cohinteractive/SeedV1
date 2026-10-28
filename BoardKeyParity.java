
import java.util.Arrays;
import java.util.Random;

public class BoardKeyParity {

    static int square(String s) {
        return ((s.charAt(1) - '1') << 3) + s.charAt(0) - 'a';
    }

    static void verifyKey(long[] b) {
        int[] pieces = new int[64];
        long occupied = b[0] | b[1] | b[2];

        for(int sq = 0; sq < 64; sq ++) {
            if(((occupied >>> sq) & 1L) == 0L) continue;
            pieces[sq] = (int) (
                ((b[0] >>> sq) & 1L) |
                (((b[1] >>> sq) & 1L) << 1) |
                (((b[2] >>> sq) & 1L) << 2) |
                (((b[3] >>> sq) & 1L) << 3));
        }

        int status = (int) b[Board.STATUS];
        long expected = Zobrist.getKey(
            pieces, status & 1, (status >>> 1) & 15,
            (status >>> Board.ESQUARE_SHIFT) & 63);

        if(b[Board.KEY] != expected)
            throw new AssertionError("Incremental Zobrist mismatch");
    }

    static long[] execute(long[] parent, long move) {
        long[] saved = parent.clone();
        long[] child = new long[6];

        Board.makeMoveInto(
            parent[0], parent[1], parent[2], parent[3],
            (int) parent[4], parent[5], move, child);

        if(!Arrays.equals(saved, parent))
            throw new AssertionError("Parent board modified");

        verifyKey(child);
        return child;
    }

    static void test(String name, String before, String after,
                     String from, String to, int type) {

        long[] parent = Board.fromFen(before);
        long[] expected = Board.fromFen(after);
        verifyKey(parent);

        long[] moves = new long[512];
        long selected = 0;
        boolean found = false;

        for(int phase = 0; phase < 2 && !found; phase ++) {
            int count = phase == 0
                ? Gen.genTactical(parent[0], parent[1], parent[2],
                                  parent[3], (int)parent[4], moves)
                : Gen.genQuiet(parent[0], parent[1], parent[2],
                               parent[3], (int)parent[4], moves);

            for(int i = 0; i < count; i ++) {
                long m = moves[i];
                if((m & 63) != square(from)) continue;
                if(((m >>> 6) & 63) != square(to)) continue;
                if(((m >>> 24) & 7) != type) continue;
                if((type == 6 || type == 7) &&
                   ((m >>> 12) & 7) != 2) continue;

                selected = m;
                found = true;
                break;
            }
        }

        if(!found)
            throw new AssertionError(name + ": legal move not found");

        long[] actual = execute(parent, selected);

        for(int i = 0; i < 6; i ++)
            if(actual[i] != expected[i])
                throw new AssertionError(name + ": board field " + i
                    + " mismatch, actual=" + Long.toHexString(actual[i])
                    + " expected=" + Long.toHexString(expected[i]));

        System.out.println("PASS: " + name);
    }

    public static void main(String[] args) {
        test("Quiet knight",
            "4k3/8/8/8/8/8/8/4K1N1 w - - 2 7",
            "4k3/8/8/8/8/5N2/8/4K3 b - - 3 7",
            "g1", "f3", 0);

        test("Pawn single",
            "4k3/8/8/8/8/8/4P3/4K3 w - - 9 5",
            "4k3/8/8/8/8/4P3/8/4K3 b - - 0 5",
            "e2", "e3", 1);

        test("Pawn double",
            "4k3/8/8/8/8/8/4P3/4K3 w - - 9 5",
            "4k3/8/8/8/4P3/8/8/4K3 b - e3 0 5",
            "e2", "e4", 2);

        test("White kingside castle",
            "4k3/8/8/8/8/8/8/4K2R w K - 9 5",
            "4k3/8/8/8/8/8/8/5RK1 b - - 10 5",
            "e1", "g1", 3);

        test("White queenside castle",
            "r3k3/8/8/8/8/8/8/R3K3 w Qq - 0 1",
            "r3k3/8/8/8/8/8/8/2KR4 b q - 1 1",
            "e1", "c1", 3);

        test("Black kingside castle",
            "4k2r/8/8/8/8/8/8/4K3 b k - 3 8",
            "5rk1/8/8/8/8/8/8/4K3 w - - 4 9",
            "e8", "g8", 3);

        test("Capture",
            "4k3/8/8/8/8/5p2/8/4K1N1 w - - 4 5",
            "4k3/8/8/8/8/5N2/8/4K3 b - - 0 5",
            "g1", "f3", 4);

        test("En passant",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 5",
            "4k3/8/3P4/8/8/8/8/4K3 b - - 0 5",
            "e5", "d6", 5);

        test("Promotion",
            "4k3/P7/8/8/8/8/8/4K3 w - - 8 5",
            "Q3k3/8/8/8/8/8/8/4K3 b - - 0 5",
            "a7", "a8", 6);

        test("Capture promotion",
            "1r2k3/P7/8/8/8/8/8/4K3 w - - 9 5",
            "1Q2k3/8/8/8/8/8/8/4K3 b - - 0 5",
            "a7", "b8", 7);

        test("Rook loses castling right",
            "4k3/8/8/8/8/8/8/4K2R w K - 2 7",
            "4k3/8/8/8/8/8/7R/4K3 b - - 3 7",
            "h1", "h2", 0);

        test("Capture removes both castling rights",
            "4k2r/8/8/8/8/8/8/4K2R w Kk - 2 7",
            "4k2R/8/8/8/8/8/8/4K3 b - - 0 7",
            "h1", "h8", 4);

        test("Expire legal en passant",
            "4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 5",
            "3k4/8/8/8/3pP3/8/8/4K3 w - - 1 6",
            "e8", "d8", 0);

        test("Black pawn double and fullmove",
            "4k3/4p3/8/8/8/8/8/4K3 b - - 4 5",
            "4k3/8/8/4p3/8/8/8/4K3 w - e6 0 6",
            "e7", "e5", 2);

        // Deterministic multi-ply smoke tests using legal moves.
        Random rng = new Random(0x5EEDL);
        long[] tactical = new long[512];
        long[] quiet = new long[512];
        int transitions = 0;

        for(int game = 0; game < 20; game ++) {
            long[] board = Board.startingPosition();

            for(int ply = 0; ply < 100; ply ++) {
                int t = Gen.genTactical(
                    board[0], board[1], board[2], board[3],
                    (int)board[4], tactical);

                int q = Gen.genQuiet(
                    board[0], board[1], board[2], board[3],
                    (int)board[4], quiet);

                if(t + q == 0) break;

                int index = rng.nextInt(t + q);
                long move = index < t ? tactical[index] : quiet[index - t];

                board = execute(board, move);
                transitions++;
            }
        }

        System.out.println("PASS: " + transitions
            + " deterministic multi-ply transitions");
        System.out.println("All Board/Zobrist tests passed.");
    }
}


public class RepetitionTest {

    static void check(boolean condition, String message) {
        if(!condition) throw new AssertionError(message);
    }

    static void verifyZobrist(long[] board) {
        int[] pieces = new int[64];
        long occupied = board[0] | board[1] | board[2];

        for(int sq = 0; sq < 64; sq ++) {
            if(((occupied >>> sq) & 1) == 0) continue;

            pieces[sq] = (int) (
                ((board[0] >>> sq) & 1) |
                (((board[1] >>> sq) & 1) << 1) |
                (((board[2] >>> sq) & 1) << 2) |
                (((board[3] >>> sq) & 1) << 3));
        }

        int status = (int) board[Board.STATUS];
        long expected = Zobrist.getKey(
            pieces, status & 1, (status >>> 1) & 15,
            (status >>> Board.ESQUARE_SHIFT) & 63);

        check(board[Board.KEY] == expected, "Zobrist mismatch");
    }

    static long[] move(long[] board, int from, int to) {
        long[] moves = new long[512];
        int count = Gen.genQuiet(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], moves);

        for(int i = 0; i < count; i ++) {
            long m = moves[i];

            if((m & 63) == from && ((m >>> 6) & 63) == to) {
                long[] child = new long[6];
                Board.makeMoveInto(
                    board[0], board[1], board[2], board[3],
                    (int) board[Board.STATUS], board[Board.KEY],
                    m, child);
                verifyZobrist(child);
                return child;
            }
        }

        throw new AssertionError("Move not found: " + from + "-" + to);
    }

    public static void main(String[] args) {
        Repetition rep = new Repetition(512);

        long[] root = Board.fromFen(
            "6nk/8/8/8/8/8/8/KN6 w - - 0 1");

        rep.reset(root);
        verifyZobrist(root);

        // Two full cycles restore the original position three times.
        int[] from = {1, 62, 16, 47, 1, 62, 16, 47};
        int[] to   = {16, 47, 1, 62, 16, 47, 1, 62};

        long[] board = root;

        for(int i = 0; i < from.length; i ++) {
            board = move(board, from[i], to[i]);
            rep.push(board[0], board[1], board[2], board[3], (int) board[4], board[5]);

            int clock = (int) ((board[Board.STATUS]
                >>> Board.HALF_MOVE_CLOCK_SHIFT) & 255);

            check(rep.threefold(clock) == (i == 7),
                "Repetition result incorrect after move " + (i + 1));
        }

        System.out.println("PASS: Threefold repetition and keyed knight moves");

        long[] noEp = Board.fromFen(
            "4k3/8/8/8/4P3/8/8/4K3 b - - 0 1");
        long[] ghostEp = Board.fromFen(
            "4k3/8/8/8/4P3/8/8/4K3 b - e3 0 1");

        check(rep.key(noEp[0], noEp[1], noEp[2], noEp[3], (int) noEp[4], noEp[5]) == rep.key(ghostEp[0], ghostEp[1], ghostEp[2], ghostEp[3], (int) ghostEp[4], ghostEp[5]),
            "Unavailable EP should be excluded");

        System.out.println("PASS: Unavailable EP");

        long[] legalEp = Board.fromFen(
            "4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1");
        long[] legalNoEp = Board.fromFen(
            "4k3/8/8/8/3pP3/8/8/4K3 b - - 0 1");

        check(rep.key(legalEp[0], legalEp[1], legalEp[2], legalEp[3], (int) legalEp[4], legalEp[5]) != rep.key(legalNoEp[0], legalNoEp[1], legalNoEp[2], legalNoEp[3], (int) legalNoEp[4], legalNoEp[5]),
            "Legal EP must affect repetition identity");

        System.out.println("PASS: Legal EP");

        long[] pinnedEp = Board.fromFen(
            "k7/8/8/4KPpr/8/8/8/8 w - g6 0 1");
        long[] pinnedNoEp = Board.fromFen(
            "k7/8/8/4KPpr/8/8/8/8 w - - 0 1");

        check(rep.key(pinnedEp[0], pinnedEp[1], pinnedEp[2], pinnedEp[3], (int) pinnedEp[4], pinnedEp[5]) == rep.key(pinnedNoEp[0], pinnedNoEp[1], pinnedNoEp[2], pinnedNoEp[3], (int) pinnedNoEp[4], pinnedNoEp[5]),
            "Illegal pinned EP should be excluded");

        System.out.println("PASS: King-exposing EP excluded");
        System.out.println("All initial repetition tests passed.");
    }
}


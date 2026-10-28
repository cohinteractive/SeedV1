
public class SearchKeyTest {

    static void check(boolean condition, String message) {
        if(!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Repetition rep = new Repetition(32);

        long[] root = Board.fromFen(
            "6nk/8/8/8/8/8/8/KN6 w - - 0 1");

        long[] child = Board.fromFen(
            "6nk/8/8/8/8/N7/8/K7 b - - 1 1");

        rep.reset(root);
        long rootFingerprint = SearchKey.append(SearchKey.SEED, rep.key(root[0], root[1], root[2], root[3], (int) root[4], root[5]));
        long rootKey = rep.searchKey((int) root[4], root[5]);

        check(rootKey == SearchKey.get(root[5], root[4], rootFingerprint),
            "Root fingerprint mismatch");

        rep.push(child[0], child[1], child[2], child[3], (int) child[4], child[5]);
        long childFingerprint = SearchKey.append(rootFingerprint, rep.key(child[0], child[1], child[2], child[3], (int) child[4], child[5]));

        check(rep.searchKey((int) child[4], child[5]) == SearchKey.get(child[5], child[4], childFingerprint),
            "Child fingerprint mismatch");

        rep.pop();
        check(rep.searchKey((int) root[4], root[5]) == rootKey, "Pop did not restore root identity");
        System.out.println("PASS: Root, child and pop");

        // Different reversible histories should affect TT identity.
        long[] reversible = Board.fromFen(
            "6nk/8/8/8/8/8/4P3/KN6 w - - 2 2");
        long[] boardA = Board.fromFen("6nk/8/8/8/8/8/4P3/K1N5 w - - 0 1");
        long a = rep.key(boardA[0], boardA[1], boardA[2], boardA[3], (int) boardA[4], boardA[5]);
        long[] boardB = Board.fromFen("6nk/8/8/8/8/8/4P3/KN6 b - - 0 1");
        long b = rep.key(boardB[0], boardB[1], boardB[2], boardB[3], (int) boardB[4], boardB[5]);
        long common = rep.key(root[0], root[1], root[2], root[3], (int) root[4], root[5]);

        Repetition first = new Repetition(32);
        Repetition second = new Repetition(32);

        first.reset(reversible, new long[]{a, common}, 2);
        second.reset(reversible, new long[]{b, common}, 2);

        check(first.searchKey((int) reversible[4], reversible[5]) != second.searchKey((int) reversible[4], reversible[5]),
            "Different histories produced identical keys");

        System.out.println("PASS: History-sensitive identity");

        // An irreversible pawn move resets fingerprint history.
        long[] pawnMove = Board.fromFen(
            "6nk/8/8/8/4P3/8/8/KN6 b - e3 0 2");

        first.push(pawnMove[0], pawnMove[1], pawnMove[2], pawnMove[3], (int) pawnMove[4], pawnMove[5]);
        second.push(pawnMove[0], pawnMove[1], pawnMove[2], pawnMove[3], (int) pawnMove[4], pawnMove[5]);

        check(first.searchKey((int) pawnMove[4], pawnMove[5]) == second.searchKey((int) pawnMove[4], pawnMove[5]),
            "Irreversible move did not reset fingerprint");

        System.out.println("PASS: Irreversible reset");

        // Null subtree must not inherit the real ancestor fingerprint.
        rep.reset(root);
        rootKey = rep.searchKey((int) root[4], root[5]);

        int oldBarrier = rep.enterNull();

        long[] nullChild = Board.fromFen(
            "7k/8/5n2/8/8/8/8/KN6 w - - 1 2");

        rep.push(nullChild[0], nullChild[1], nullChild[2], nullChild[3], (int) nullChild[4], nullChild[5]);
        long isolated = SearchKey.append(SearchKey.SEED, rep.key(nullChild[0], nullChild[1], nullChild[2], nullChild[3], (int) nullChild[4], nullChild[5]));

        check(rep.searchKey((int) nullChild[4], nullChild[5]) ==
              SearchKey.get(nullChild[5], nullChild[4], isolated),
              "Null subtree inherited real history");

        rep.pop();
        rep.exitNull(oldBarrier);

        check(rep.searchKey((int) root[4], root[5]) == rootKey,
            "Null subtree changed parent identity");

        System.out.println("PASS: Null fingerprint isolation");

        // Threefold state must also be isolated and then restored.
        long[] oBoard = Board.fromFen("6nk/8/8/8/8/8/8/KN6 b - - 0 1");
        long opposite = rep.key(oBoard[0], oBoard[1], oBoard[2], oBoard[3], (int) oBoard[4], oBoard[5]);

        long current = rep.key(root[0], root[1], root[2], root[3], (int) root[4], root[5]);
        rep.reset(root, new long[]{current, opposite, current, opposite}, 4);

        check(rep.threefold(8), "Expected threefold before null");

        oldBarrier = rep.enterNull();
        rep.push(root[0], root[1], root[2], root[3], (int) root[4], root[5]);
        check(!rep.threefold(8), "Null barrier leaked earlier repetitions");

        rep.pop();
        rep.exitNull(oldBarrier);
        check(rep.threefold(8), "Threefold not restored after null");

        System.out.println("PASS: Null repetition barrier");
        System.out.println("All SearchKey tests passed.");
    }
}

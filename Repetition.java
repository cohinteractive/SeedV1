public final class Repetition {
    
    public Repetition(int capacity) {
        keys = new long[capacity];
        fingerprints = new long[capacity];
    }

    public void reset(long[] root) {
        reset(root, null, 0);
    }

    
public void reset(long[] root, long[] previousKeys, int count) {
    if(count < 0 || count >= keys.length ||
       (count > 0 && (previousKeys == null || count > previousKeys.length)))
        throw new IllegalArgumentException("Invalid game history");

    if(count > 0)
        System.arraycopy(previousKeys, 0, keys, 0, count);

    size = count;
    barrier = 0;
    keys[size++] = key(root[0], root[1], root[2], root[3], (int) root[4], root[5]);
    rootSize = size;

    int halfmove = (int) (root[Board.STATUS] >>> Board.HALF_MOVE_CLOCK_SHIFT)
                 & Board.HALF_MOVE_CLOCK_BITS;

    // SeedV6 treats a saturated 127 clock as including all history.
    int start = halfmove >= 127 ? 0 : Math.max(0, size - 1 - halfmove);

    long fingerprint = SearchKey.SEED;
    for(int i = start; i < size; i ++)
        fingerprint = SearchKey.append(fingerprint, keys[i]);

    fingerprints[size - 1] = fingerprint;
}


    public long key(long board0, long board1, long board2, long board3, int status, long key) {
        long result = key;
        int ep = (int) ((status >>> Board.ESQUARE_SHIFT) & 63);
        if(ep == 0) return result;
        int count = Gen.genTactical(board0, board1, board2, board3, status, epMoves);
        for(int i = 0; i < count; i ++) {
            if(((epMoves[i] >>> 24) & 7) == 5) return result;
        }
        return result ^ Zobrist.ENPASSANT_FILE[ep & 7];
    }

    
public void push(long board0, long board1, long board2, long board3, int status, long key) {
    if(size == keys.length)
        throw new IllegalStateException("Repetition capacity exceeded");

    long key2 = key(board0, board1, board2, board3, status, key);
    int halfmove = (status >>> Board.HALF_MOVE_CLOCK_SHIFT)
                 & Board.HALF_MOVE_CLOCK_BITS;

    long fingerprint = (halfmove == 0 || size == barrier)
                     ? SearchKey.SEED
                     : fingerprints[size - 1];

    keys[size] = key2;
    fingerprints[size] = SearchKey.append(fingerprint, key2);
    size++;
}


    public void pop() {
        if(size <= rootSize)
            throw new IllegalStateException("Cannot pop game history");
        size --;
    }

    public boolean threefold(int halfMoveClock) {
        long current = keys[size - 1];
        int earliest = Math.max(barrier, size - 1 - halfMoveClock);
        int occurrences = 1;
        for(int i = size - 3; i >= earliest; i -= 2)
            if(keys[i] == current && ++ occurrences ==3) return true;
        return false;
    }

    public int enterNull() {
        int previous = barrier;
        barrier = size;
        return previous;
    }

    public void exitNull(int previousBarrier) {
        barrier = previousBarrier;
    }

    public long searchKey(int status, long key) {
        if(size == barrier)
            throw new IllegalStateException("Synthetic null position has no real-history fingerprint");
        return SearchKey.get(key, status, fingerprints[size - 1]);
    }

    private final long[] keys;
    private final long[] fingerprints;
    private final long[] epMoves = new long[512];
    private int size;
    private int rootSize;
    private int barrier;

}

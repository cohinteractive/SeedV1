public final class SearchKey {
    
    static final long SEED = 0x243f6a8885a308d3L;
    static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    static long append(long fingerprint, long repetitionKey) {
        return mix(fingerprint ^ mix(repetitionKey));
    }

    static long get(long zobrist, long status, long fingerprint) {
        return mix(zobrist ^ Long.rotateLeft(fingerprint, 23) ^ mix(status));
    }

    private SearchKey() {}

}

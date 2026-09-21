public class Pext {
    
    public static long rookMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) Long.compress(allOccupancy, ROOK_MOVEMENT[square])];
	}

	public static long bishopMoves(int square, long allOccupancy) {
		return BISHOP_MOVES[square][(int) Long.compress(allOccupancy, BISHOP_MOVEMENT[square])];
	}

	public static long queenMoves(int square, long allOccupancy) {
		return ROOK_MOVES[square][(int) Long.compress(allOccupancy, ROOK_MOVEMENT[square])]
		| BISHOP_MOVES[square][(int) Long.compress(allOccupancy, BISHOP_MOVEMENT[square])];
	}

	public static void init() {}

	private final static long OUTER = 0xff818181818181ffL;
    private final static long[] RANK = {
        0x00000000000000ffL, 0x000000000000ff00L, 0x0000000000ff0000L, 0x00000000ff000000L,
        0x000000ff00000000L, 0x0000ff0000000000L, 0x00ff000000000000L, 0xff00000000000000L
    };
    private final static long[] FILE = {
        0x0101010101010101L, 0x0202020202020202L, 0x0404040404040404L, 0x0808080808080808L,
        0x1010101010101010L, 0x2020202020202020L, 0x4040404040404040L, 0x8080808080808080L
    };
    private final static long[] FORWARD_DIAGONAL = {
        0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L, 0x0000000000008040L, 0x0000000000000080L,
        0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L, 0x0000000000008040L,
        0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L, 0x0000000000804020L,
        0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L, 0x0000000080402010L,
        0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L, 0x0000008040201008L,
        0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L, 0x0000804020100804L,
        0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L, 0x0080402010080402L,
        0x0100000000000000L, 0x0201000000000000L, 0x0402010000000000L, 0x0804020100000000L, 0x1008040201000000L, 0x2010080402010000L, 0x4020100804020100L, 0x8040201008040201L
    };
    private final static long[] BACKWARD_DIAGONAL = {
        0x0000000000000001L, 0x0000000000000102L, 0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L,
        0x0000000000000102L, 0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L,
        0x0000000000010204L, 0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L,
        0x0000000001020408L, 0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L,
        0x0000000102040810L, 0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L,
        0x0000010204081020L, 0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L,
        0x0001020408102040L, 0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L,
        0x0102040810204080L, 0x0204081020408000L, 0x0408102040800000L, 0x0810204080000000L, 0x1020408000000000L, 0x2040800000000000L, 0x4080000000000000L, 0x8000000000000000L
    };

	public final static long[][] ROOK_MOVES = new long[64][];
	public final static long[] ROOK_MOVEMENT = new long[64];
	public final static long[][] BISHOP_MOVES = new long[64][];
	public final static long[] BISHOP_MOVEMENT = new long[64];

	static {
		for (int square = 0; square < 64; square ++) {
			int rank = square >>> 3;
			int file = square & 7;
			ROOK_MOVEMENT[square] = (FILE[file] | RANK[rank]) & ~((file == 0 ? 0 : FILE[0]) | (file == 7 ? 0 : FILE[7]) | (rank == 0 ? 0 : RANK[0]) | (rank == 7 ? 0 : RANK[7]) | (1L << square));
			ROOK_MOVES[square] = generateRookMoves(square);
			BISHOP_MOVEMENT[square] = ((FORWARD_DIAGONAL[square] | BACKWARD_DIAGONAL[square]) ^ (1L << square)) & ~OUTER;
			BISHOP_MOVES[square] = generateBishopMoves(square);
		}
	}

	private Pext() {}

	private static long[] generateRookMoves(int square) {
		long movement = ROOK_MOVEMENT[square];
		int entryCount = 1 << Long.bitCount(movement);
		long[] rookMoves = new long[entryCount];
		for (int index = 0; index < entryCount; index ++) {
			long blockers = Long.expand(index, movement);
			rookMoves[index] = slowRookMoves(square, blockers);
		}
		return rookMoves;
	}

	private static long[] generateBishopMoves(int square) {
		long movement = BISHOP_MOVEMENT[square];
		int entryCount = 1 << Long.bitCount(movement);
		long[] bishopMoves = new long[entryCount];
		for (int index = 0; index < entryCount; index ++) {
			long blockers = Long.expand(index, movement);
			bishopMoves[index] = slowBishopMoves(square, blockers);
		}
		return bishopMoves;
	}

	private static long slowRookMoves(int square, long occupancy) {
		long moves = 0L;
		for (int target = square + 8; target < 64; target += 8) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 8; target >= 0; target -= 8) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square + 1; target < 64 && target % 8 != 0; target ++) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 1; target >= 0 && target % 8 != 7; target --) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		return moves;
	}

	private static long slowBishopMoves(int square, long occupancy) {
		long moves = 0L;
		for (int target = square + 7; target < 64 && target % 8 != 7; target += 7) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square + 9; target < 64 && target % 8 != 0; target += 9) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 9; target >= 0 && target % 8 != 7; target -= 9) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		for (int target = square - 7; target >= 0 && target % 8 != 0; target -= 7) {
			moves |= 1L << target;
			if ((occupancy & (1L << target)) != 0L) {
				break;
			}
		}
		return moves;
	}

}

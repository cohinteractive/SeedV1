public class Board {

    public static final int MAX_BITBOARDS = 6;
    public static final int STATUS = 4;
    public static final int KEY = 5;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;
    public static final int EMPTY_SQUARE = 0;
    public static final int HALF_MOVE_CLOCK_BITS = 0b1111111;
    public static final int FULL_MOVE_NUMBER_BITS = 0b1111111111;
    public static final int ESQUARE_SHIFT = 5;
    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int FULL_MOVE_NUMBER_SHIFT = 18;
    public static final int FILE = 7;
    public static final int FILE_C = 2;
    public static final int FILE_G = 6;
    
    public static long[] fromFen(String fen) {
        long[] board = new long[MAX_BITBOARDS];
        int[] pieces = Fen.getPieces(fen);
        long board0 = 0;
        long board1 = 0;
        long board2 = 0;
        long board3 = 0;
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            int piece = pieces[square];
            if(piece != EMPTY_SQUARE) {
                long squareBit = 1L << square;
                board0 |= -(piece & 1) & squareBit;
                board1 |= -(piece >>> 1 & 1) & squareBit;
                board2 |= -(piece >>> 2 & 1) & squareBit;
                board3 |= -(piece >>> 3 & 1) & squareBit;
            }
        }
        board[0] = board0;
        board[1] = board1;
        board[2] = board2;
        board[3] = board3;
        int playerToMove = Fen.getWhiteToMove(fen) ? 0 : 1;
        int castling = Fen.getCastling(fen);
        long status = playerToMove ^ castling << CASTLING_SHIFT;
        int eSquare = Fen.getEnPassantSquare(fen);
        if(eSquare != -1) {
            status ^= eSquare << ESQUARE_SHIFT;
        } else {
            eSquare = 0; // Must be 0 here; Zobrist.getKey relies on it for branchless eSquare handling.
        }
        board[STATUS] = status ^ Fen.getHalfMoveClock(fen) << HALF_MOVE_CLOCK_SHIFT ^ Fen.getFullMoveNumber(fen) << FULL_MOVE_NUMBER_SHIFT;
        board[KEY] = Zobrist.getKey(pieces, playerToMove, castling, eSquare);
        return board;
    }

    public static void makeMoveIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        switch(((int) move) >>> 24 & 7) {
            case 0: makeQuietIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 1: makePawnPushIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 2: makePawnDoublePushIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 3: makeCastleIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 4: makeCaptureIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 5: makeEnPassantIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 6: makePromotionIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            case 7: makeCapturePromotionIntoFast(board0, board1, board2, board3, status, move, newBoard); return;
            default: return;
        }
    }

    private static final int PLAYER_BIT = 1;
    private static final int SQUARE_BITS = 0b111111;
    private static final int PIECE_BITS = 0b1111;
    private static final int TARGET_SQUARE_SHIFT = 6;
    private static final int START_PIECE_SHIFT = 16;
    private static final int TARGET_PIECE_SHIFT = 20;
    private static final int CASTLING_CHANGE_SHIFT = 28;
    private static final int CASTLING_SHIFT = 1;
    private static final int CASTLING_BITS = 0b1111;

    private static void makeQuietIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board0 ^= -(startPiece & 1) & pieceMoveBits;
        board1 ^= -(startPiece >>> 1 & 1) & pieceMoveBits;
        board2 ^= -(startPiece >>> 2 & 1) & pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1) << HALF_MOVE_CLOCK_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makePawnPushIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makePawnDoublePushIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int eSquare = (startSquare + targetSquare) >>> 1;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (eSquare << ESQUARE_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makeCastleIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long kingMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int rookStartSquare;
        final int rookTargetSquare;
        if((targetSquare & FILE) == FILE_G) {
            rookStartSquare = targetSquare + 1;
            rookTargetSquare = targetSquare - 1;
        } else {
            rookStartSquare = targetSquare - 2;
            rookTargetSquare = targetSquare + 1;
        }
        final long rookMoveBits = (1L << rookStartSquare) | (1L << rookTargetSquare);
        final long bothPieceMoveBits = kingMoveBits | rookMoveBits;
        board0 ^= bothPieceMoveBits;
        board1 ^= rookMoveBits;
        board3 ^= -player & (bothPieceMoveBits);
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1) << HALF_MOVE_CLOCK_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makeCaptureIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        final int pieceChange = startPiece ^ targetPiece;
        board0 ^= (-(startPiece & 1) & startSquareBit) | (-(pieceChange & 1) & targetSquareBit);
        board1 ^= (-(startPiece >>> 1 & 1) & startSquareBit) | (-(pieceChange >>> 1 & 1) & targetSquareBit);
        board2 ^= (-(startPiece >>> 2 & 1) & startSquareBit) | (-(pieceChange >>> 2 & 1) & targetSquareBit);
        board3 ^= targetSquareBit | (-player & startSquareBit);
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makeEnPassantIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int captureSquare = targetSquare + (player << 4) - 8;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final long captureSquareBit = 1L << captureSquare;
        board1 ^= pieceMoveBits | captureSquareBit;
        board2 ^= pieceMoveBits | captureSquareBit;
        board3 ^= (-player & pieceMoveBits) | (~(-player) & captureSquareBit);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (status & 0b11110) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makePromotionIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int promotePiece = (int) move >>> 12 & PIECE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        board0 ^= -(promotePiece & 1) & targetSquareBit;
        board1 ^= startSquareBit | (-(promotePiece >>> 1 & 1) & targetSquareBit);
        board2 ^= startSquareBit | (-(promotePiece >>> 2 & 1) & targetSquareBit);
        board3 ^= -player & (startSquareBit | targetSquareBit);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (status & 0b11110) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makeCapturePromotionIntoFast(long board0, long board1, long board2, long board3, int status, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int promotePiece = (int) move >>> 12 & PIECE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        final int pieceChange = targetPiece ^ promotePiece;
        board0 ^= -(pieceChange & 1) & targetSquareBit;
        board1 ^= startSquareBit | (-(pieceChange >>> 1 & 1) & targetSquareBit);
        board2 ^= startSquareBit | (-(pieceChange >>> 2 & 1) & targetSquareBit);
        board3 ^= targetSquareBit | (-player & startSquareBit);
        final int oldCastling = status >>> CASTLING_SHIFT & CASTLING_BITS;
        final int castling = oldCastling & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & 0b1111);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }
}

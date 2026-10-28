public class Board {

    public static final int MAX_BITBOARDS = 6;
    public static final int STATUS = 4;
    public static final int KEY = 5;
    public static final int SQUARE_A1 = 0;
    public static final int SQUARE_A8 = 56;
    public static final int SQUARE_H1 = 7;
    public static final int SQUARE_H8 = 63;
    public static final int EMPTY_SQUARE = 0;
    public static final int HALF_MOVE_CLOCK_BITS = 0b11111111;
    public static final int FULL_MOVE_NUMBER_BITS = 0b111111111111;
    public static final int ESQUARE_SHIFT = 5;
    public static final int HALF_MOVE_CLOCK_SHIFT = 11;
    public static final int FULL_MOVE_NUMBER_SHIFT = 19;
    public static final int FULL_MOVE_NUMBER_BITS_UNSHIFTED = FULL_MOVE_NUMBER_BITS << FULL_MOVE_NUMBER_SHIFT;
    public static final int FILE = 7;
    public static final int FILE_C = 2;
    public static final int FILE_G = 6;
    
    /*
     * bit  0       : player
     * bits 1-4     : castling
     * bits 5-10    : eSquare
     * bits 11-18   : half move clock
     * bits 19-30   : full move clock
     */
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

    public static long[] startingPosition() {
        return fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
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

    public static void makeMoveInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        switch(((int) move) >>> 24 & 7) {
            case 0: makeQuietInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 1: makePawnSingleInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 2: makePawnDoubleInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 3: makeCastleInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 4: makeCaptureInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 5: makeEnPassantInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 6: makePromotionInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            case 7: makeCapturePromotionInto(board0, board1, board2, board3, status, key, move, newBoard); return;
            default: return;
        }
    }

    public static void nullMoveInto(long board0, long board1, long board2, long board3, int status, long key, long[] newBoard) {
        final int eSquare = status >>> ESQUARE_SHIFT & SQUARE_BITS;
        if(eSquare > 0) key ^= Zobrist.ENPASSANT_FILE[eSquare & FILE];
        key ^= Zobrist.WHITEMOVE;
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = ((status ^ PLAYER_BIT) & ~FULL_MOVE_NUMBER_BITS_UNSHIFTED | (((status >>> FULL_MOVE_NUMBER_SHIFT & FULL_MOVE_NUMBER_BITS) + (status & PLAYER_BIT)) & FULL_MOVE_NUMBER_BITS) << FULL_MOVE_NUMBER_SHIFT) & ENPASSANT_RESET_BITS;
        newBoard[KEY] = key;
    }

    public static long getCheckersPext(long board0, long board1, long board2, long board3, long colorMask, int player, int kingSquare, long allOccupancy) {
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        return (LEAP_ATTACKS[kingSquare] & (board0 & ~board1 & board2 & ~colorMask)) |
               (PAWN_ATTACKS[player][kingSquare] & (~board0 & board1 & board2 & ~colorMask)) |
               (KING_ATTACKS[kingSquare] & (board0 & ~board1 & ~board2 & ~colorMask)) |
               (Pext.rookMoves(kingSquare, allOccupancy) & (otherQueens | (board0 & board1 & ~board2 & ~colorMask))) |
               (Pext.bishopMoves(kingSquare, allOccupancy) & (otherQueens | (~board0 & ~board1 & board2 & ~colorMask)));
    }

    private static final int PLAYER_BIT = 1;
    private static final int SQUARE_BITS = 0b111111;
    private static final int PIECE_BITS = 0b1111;
    private static final int PIECE_TYPE = 7;
    private static final int TARGET_SQUARE_SHIFT = 6;
    private static final int START_PIECE_SHIFT = 16;
    private static final int TARGET_PIECE_SHIFT = 20;
    private static final int CASTLING_CHANGE_SHIFT = 28;
    private static final int CASTLING_SHIFT = 1;
    private static final int CASTLING_BITS = 0b1111;
    private static final int CASTLING_BITS_SHIFTED = CASTLING_BITS << CASTLING_SHIFT;
    private static final int KING = 1;
    private static final int ROOK = 3;
    private static final int PAWN = 6;
    private static final int ENPASSANT_RESET_BITS = ~(SQUARE_BITS << ESQUARE_SHIFT);
    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long FILE_A_BITBOARD = 0x0101010101010101L;
    private static final long FILE_B_BITBOARD = 0x0202020202020202L;
    private static final long FILE_G_BITBOARD = 0x4040404040404040L;
    private static final long FILE_H_BITBOARD = 0x8080808080808080L;
    static {
        for(int square = SQUARE_A1; square <= SQUARE_H8; square ++) {
            final long squareBit = 1L << square;
            KING_ATTACKS[square] =
                (squareBit << 9 & ~FILE_A_BITBOARD) |
                (squareBit << 8) |
                (squareBit << 7 & ~FILE_H_BITBOARD) |
                (squareBit << 1 & ~FILE_A_BITBOARD) |
                (squareBit >>> 1 & ~FILE_H_BITBOARD) |
                (squareBit >>> 7 & ~FILE_A_BITBOARD) |
                (squareBit >>> 8) |
                (squareBit >>> 9 & ~FILE_H_BITBOARD)
            ;
            LEAP_ATTACKS[square] =
                (squareBit << 17 & ~FILE_A_BITBOARD) |
                (squareBit << 15 & ~FILE_H_BITBOARD) |
                (squareBit << 10 & ~(FILE_A_BITBOARD | FILE_B_BITBOARD)) |
                (squareBit << 6 & ~(FILE_G_BITBOARD | FILE_H_BITBOARD)) |
                (squareBit >>> 6 & ~(FILE_A_BITBOARD | FILE_B_BITBOARD)) |
                (squareBit >>> 10 & ~(FILE_G_BITBOARD | FILE_H_BITBOARD)) |
                (squareBit >>> 15 & ~FILE_A_BITBOARD) |
                (squareBit >>> 17 & ~FILE_H_BITBOARD)
            ;
            PAWN_ATTACKS[0][square] =
                (squareBit << 9 & ~FILE_A_BITBOARD) |
                (squareBit << 7 & ~FILE_H_BITBOARD)
            ;
            PAWN_ATTACKS[1][square] =
                (squareBit >>> 7 & ~FILE_A_BITBOARD) |
                (squareBit >>> 9 & ~FILE_H_BITBOARD)
            ;
        }
    }

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
        final int castling = (status >>> CASTLING_SHIFT & CASTLING_BITS) & ~((int) (move >>> CASTLING_CHANGE_SHIFT) & CASTLING_BITS);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1) << HALF_MOVE_CLOCK_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
    }

    private static void makeQuietInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int startPieceType = startPiece & PIECE_TYPE;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        board0 ^= -(startPiece & 1) & pieceMoveBits;
        board1 ^= -(startPiece >>> 1 & 1) & pieceMoveBits;
        board2 ^= -(startPiece >>> 2 & 1) & pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        final int castlingChange = (int) (move >>> CASTLING_CHANGE_SHIFT) & CASTLING_BITS;
        int castling = ((status >>> CASTLING_SHIFT) & CASTLING_BITS);
        if(castlingChange != 0) {
            key ^= Zobrist.CASTLING[castling & castlingChange];
            castling &= ~castlingChange;
        }
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1) << HALF_MOVE_CLOCK_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[startPieceType][player][startSquare] ^ Zobrist.PIECE[startPieceType][player][targetSquare];
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

    private static void makePawnSingleInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (status & CASTLING_BITS_SHIFTED) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[PAWN][player][startSquare] ^ Zobrist.PIECE[PAWN][player][targetSquare];
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

    private static void makePawnDoubleInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int newESquare = (startSquare + targetSquare) >>> 1;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        board1 ^= pieceMoveBits;
        board2 ^= pieceMoveBits;
        board3 ^= -player & pieceMoveBits;
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (status & CASTLING_BITS_SHIFTED) | (newESquare << ESQUARE_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ (Zobrist.ENPASSANT_FILE[newESquare & FILE]) ^ Zobrist.PIECE[PAWN][player][startSquare] ^ Zobrist.PIECE[PAWN][player][targetSquare];
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

    private static void makeCastleInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final long kingMoveBits = (1L << startSquare) | (1L << targetSquare);
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
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
        final int castlingChange = (int) (move >>> CASTLING_CHANGE_SHIFT) & CASTLING_BITS;
        int castling = (status >>> CASTLING_SHIFT) & CASTLING_BITS;
        if(castlingChange != 0) {
            key ^= Zobrist.CASTLING[castling & castlingChange];
            castling &= ~castlingChange;
        }
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = (1 ^ player) | (castling << CASTLING_SHIFT) | (((status >>> HALF_MOVE_CLOCK_SHIFT & HALF_MOVE_CLOCK_BITS) + 1) << HALF_MOVE_CLOCK_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[KING][player][startSquare] ^ Zobrist.PIECE[KING][player][targetSquare] ^ Zobrist.PIECE[ROOK][player][rookStartSquare] ^ Zobrist.PIECE[ROOK][player][rookTargetSquare];
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

    private static void makeCaptureInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int startPiece = (int) move >>> START_PIECE_SHIFT & PIECE_BITS;
        final int startPieceType = startPiece & PIECE_TYPE;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int targetPieceType = targetPiece & PIECE_TYPE;
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        final int pieceChange = startPiece ^ targetPiece;
        board0 ^= (-(startPiece & 1) & startSquareBit) | (-(pieceChange & 1) & targetSquareBit);
        board1 ^= (-(startPiece >>> 1 & 1) & startSquareBit) | (-(pieceChange >>> 1 & 1) & targetSquareBit);
        board2 ^= (-(startPiece >>> 2 & 1) & startSquareBit) | (-(pieceChange >>> 2 & 1) & targetSquareBit);
        board3 ^= targetSquareBit | (-player & startSquareBit);
        final int castlingChange = (int) (move >>> CASTLING_CHANGE_SHIFT) & CASTLING_BITS;
        int castling = (status >>> CASTLING_SHIFT) & CASTLING_BITS;
        if(castlingChange != 0) {
            key ^= Zobrist.CASTLING[castling & castlingChange];
            castling &= ~castlingChange;
        }
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = other | (castling << CASTLING_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[startPieceType][player][startSquare] ^ Zobrist.PIECE[startPieceType][player][targetSquare] ^ Zobrist.PIECE[targetPieceType][other][targetSquare];
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

    private static void makeEnPassantInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int captureSquare = targetSquare + (player << 4) - 8;
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        final long pieceMoveBits = (1L << startSquare) | (1L << targetSquare);
        final long captureSquareBit = 1L << captureSquare;
        board1 ^= pieceMoveBits | captureSquareBit;
        board2 ^= pieceMoveBits | captureSquareBit;
        board3 ^= (-player & pieceMoveBits) | (~(-player) & captureSquareBit);
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = other | (status & 0b11110) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[PAWN][player][startSquare] ^ Zobrist.PIECE[PAWN][player][targetSquare] ^ Zobrist.PIECE[PAWN][other][captureSquare];
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

    private static void makePromotionInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int promotePiece = (int) move >>> 12 & PIECE_BITS;
        final int promotePieceType = promotePiece & PIECE_TYPE;
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
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
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[PAWN][player][startSquare] ^ Zobrist.PIECE[promotePieceType][player][targetSquare];
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

    private static void makeCapturePromotionInto(long board0, long board1, long board2, long board3, int status, long key, long move, long[] newBoard) {
        final int player = status & PLAYER_BIT;
        final int other = 1 ^ player;
        final int startSquare = (int) move & SQUARE_BITS;
        final int targetSquare = (int) move >>> TARGET_SQUARE_SHIFT & SQUARE_BITS;
        final int targetPiece = (int) move >>> TARGET_PIECE_SHIFT & PIECE_BITS;
        final int targetPieceType = targetPiece & PIECE_TYPE;
        final int promotePiece = (int) move >>> 12 & PIECE_BITS;
        final int promotePieceType = promotePiece & PIECE_TYPE;
        final int eSquare = (status >>> ESQUARE_SHIFT) & SQUARE_BITS;
        final long startSquareBit = 1L << startSquare;
        final long targetSquareBit = 1L << targetSquare;
        final int pieceChange = targetPiece ^ promotePiece;
        board0 ^= -(pieceChange & 1) & targetSquareBit;
        board1 ^= startSquareBit | (-(pieceChange >>> 1 & 1) & targetSquareBit);
        board2 ^= startSquareBit | (-(pieceChange >>> 2 & 1) & targetSquareBit);
        board3 ^= targetSquareBit | (-player & startSquareBit);
        final int castlingChange = (int) (move >>> CASTLING_CHANGE_SHIFT) & CASTLING_BITS;
        int castling = ((status >>> CASTLING_SHIFT) & CASTLING_BITS);
        if(castlingChange != 0) {
            key ^= Zobrist.CASTLING[castling & castlingChange];
            castling &= ~castlingChange;
        }
        newBoard[0] = board0;
        newBoard[1] = board1;
        newBoard[2] = board2;
        newBoard[3] = board3;
        newBoard[STATUS] = other | (castling << CASTLING_SHIFT) | (((status >>> FULL_MOVE_NUMBER_SHIFT) + player) << FULL_MOVE_NUMBER_SHIFT);
        newBoard[KEY] = key ^ Zobrist.WHITEMOVE ^ (Zobrist.ENPASSANT_FILE[eSquare & FILE] & -((eSquare | -eSquare) >>> 31)) ^ Zobrist.PIECE[PAWN][player][startSquare] ^ Zobrist.PIECE[promotePieceType][player][targetSquare] ^ Zobrist.PIECE[targetPieceType][other][targetSquare];
    }
}

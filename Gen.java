public class Gen {

    //#region Evasions
    public static int genEvasion(long board0, long board1, long board2, long board3, int status, long checkers, long[] movesBuffer) {
        final int player = status & PLAYER_BIT;
        final int playerBit = player << PLAYER_SHIFT;
        final int other = 1 ^ player;
        final long colorMask = ~(-(player) ^ board3);
        final long allOccupancy = board0 | board1 | board2;
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;
        final long playerKing = board0 & ~board1 & ~board2 & colorMask;
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;
        final long otherQueensAndRooks = otherQueens | otherRooks;
        final long otherQueensAndBishops = otherQueens | otherBishops;
        final int[] lsb = LSB;
        final int kingSquare = lsb[(int) ((playerKing * DB) >>> 58)];
        int moveListLength = getKingEvasions(board0, board1, board2, board3, kingSquare, player, playerKing, KING | playerBit, allOccupancy, otherOccupancy, lsb, movesBuffer, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
        if((checkers & (checkers - 1L)) != 0L) return moveListLength;
        long responseMask = ~0L;
        final long[] between = BETWEEN;
        if(checkers != 0L) {
            responseMask = checkers;
            if((checkers & (otherQueensAndRooks | otherBishops)) != 0L) responseMask |= between[kingSquare | ((lsb[(int) ((checkers * DB) >>> 58)]) << 6)];
        }
        final long rookBlockers = Pext.rookMoves(kingSquare, allOccupancy) & playerOccupancy;
        final long bishopBlockers = Pext.bishopMoves(kingSquare, allOccupancy) & playerOccupancy;
        final long rookPinners = Pext.rookMoves(kingSquare, allOccupancy ^ rookBlockers) & (otherQueensAndRooks);
        final long bishopPinners = Pext.bishopMoves(kingSquare, allOccupancy ^ bishopBlockers) & (otherQueensAndBishops);
        long pinned = 0L;
        final long pinners = rookPinners | bishopPinners;
        long pinners2 = pinners;
        while(pinners2 != 0L) {
            final long pinner = pinners2 & -pinners2;
            pinners2 ^= pinner;
            pinned |= between[kingSquare | (lsb[(int) ((pinner * DB) >>> 58)] << 6)] & playerOccupancy;
        }
        moveListLength = getQueenEvasions(board0, board1, board2, board3, ~board0 & board1 & ~board2 & colorMask, QUEEN | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getRookEvasions(board0, board1, board2, board3, board0 & board1 & ~board2 & colorMask, ROOK | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getBishopEvasions(board0, board1, board2, board3, ~board0 & ~board1 & board2 & colorMask, BISHOP | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getKnightEvasions(board0, board1, board2, board3, board0 & ~board1 & board2 & colorMask & ~pinned, KNIGHT | playerBit, allOccupancy, otherOccupancy, responseMask, lsb, movesBuffer, moveListLength);
        final int eSquare = status >>> EN_PASSANT_SQUARE_SHIFT & SQUARE_BITS;
        return getPawnEvasions(board0, board1, board2, board3, ~board0 & board1 & board2 & colorMask, PAWN | playerBit, player, playerBit, (1L << eSquare) & -(long) ((eSquare + 63) >>> 6), eSquare, pinned, pinners, allOccupancy, otherOccupancy, responseMask, checkers, lsb, movesBuffer, moveListLength, kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
    }

    private static int getKingEvasions(long board0, long board1, long board2, long board3, int square, int player, long playerKing, int piece, long allOccupancy, long otherOccupancy, int[] lsb, long[] moves, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        int moveListLength = 0;
        final long kingAttacks = KING_ATTACKS[square];
        final long moveInfo = (long) square
        | ((long) piece << START_PIECE_SHIFT)
        | (long) (WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -(long) player));
        long moveBitboard = kingAttacks & otherOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & (~playerKing) & ~b) | b)) {
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        moveBitboard = kingAttacks & ~allOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & (~playerKing) & ~b) | b))
                moves[moveListLength ++] = moveInfo
                | ((long) targetSquare << TARGET_SQUARE_SHIFT);
        }
        return moveListLength;
    }

    private static int getQueenEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            final long destinations = (Pext.queenMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb));
            final long moveInfo = (long) square | ((long) piece << START_PIECE_SHIFT);
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            final long destinations = (Pext.rookMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb));
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT)
            | rookCastlingChange(square);
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            final long destinations = (Pext.bishopMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb));
            final long moveInfo = (long) square
            | (piece << START_PIECE_SHIFT);
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getKnightEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, int[] lsb, long[] moves, int moveListLength) {
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long destinations = leapAttacks[square] & responseMask;
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, int player, int playerBit, long epBit, int eSquare, long pinned, long pinners, long allOccupancy, long otherOccupancy, long responseMask, long checkers, int[] lsb, long[] moves, int moveListLength, int kingSquare, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        final int promotionRank = 7 & ~(-player);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[][] advanceSingle = PAWN_ADVANCE_SINGLE;
        final long[][] advanceDouble = PAWN_ADVANCE_DOUBLE;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            final long moveInfoPawnPush = moveInfo
            | (long) PAWN_PUSH_BITS;
            final long moveInfoDoublePush = moveInfo
            | (long) PAWN_DOUBLE_PUSH_BITS;
            final long moveInfoPromotion = moveInfo
            | (long) PROMOTION_BITS;
            final long moveInfoCapture = moveInfo
            | (long) CAPTURE_BITS;
            final long moveInfoCapturePromotion = moveInfo
            | (long) CAPTURE_PROMOTION_BITS;
            final long moveInfoEnPassant = moveInfo
            | (long) EN_PASSANT_BITS;
            final long pinMask = -((b & pinned) >>> square);
            final long pinRay = ~pinMask | getPinRay(kingSquare, b, pinners, lsb);
            final long attacks = pawnAttacks[square];
            long moveBitboard = attacks & otherOccupancy & responseMask & pinRay;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                final long targetInfo = ((long) targetSquare << TARGET_SQUARE_SHIFT)
                    | ((long) targetPiece << TARGET_PIECE_SHIFT)
                    | capturedRookCastlingChange(targetPiece, targetSquare);
                if((targetSquare >>> 3) == promotionRank) {
                    final long moveInfoTarget = moveInfoCapturePromotion
                    | targetInfo;
                    moves[moveListLength ++] = moveInfoTarget | ((long) (QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = moveInfoCapture | targetInfo;
                }
            }
            final long epDestination = attacks & epBit & pinRay;
            if(epDestination != 0L) {
                final int capturedSquare = eSquare + (player << 4) - 8;
                final long capturedPawnBit = 1L << capturedSquare;
                final boolean checkResponse = checkers == 0L || (epDestination & responseMask) != 0L || (capturedPawnBit & checkers) != 0L;
                if((otherPawns & capturedPawnBit) != 0L && checkResponse) {
                    final long occupancyAfter = (allOccupancy ^ b ^ capturedPawnBit) | epDestination;
                    if(!isSquareAttackedByPlayer(kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns & ~capturedPawnBit, occupancyAfter)) moves[moveListLength ++] = moveInfoEnPassant | ((long) eSquare << TARGET_SQUARE_SHIFT);
                }
            }
            final long singlePush = advanceSingle[player][square] & ~allOccupancy;
            if(singlePush == 0L) continue;
            final int targetSquare = lsb[(int) ((singlePush * DB) >>> 58)];
            if((targetSquare >>> 3) == promotionRank) {
                if((singlePush & pinRay & responseMask) != 0L) {
                    final long moveInfoTarget = moveInfoPromotion | ((long) targetSquare << TARGET_SQUARE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                }
                continue;
            }
            if((singlePush & pinRay & responseMask) != 0L) moves[moveListLength ++] = moveInfoPawnPush | ((long) targetSquare << TARGET_SQUARE_SHIFT);
            final long doublePush = advanceDouble[player][square] & ~allOccupancy & pinRay & responseMask;
            if(doublePush != 0L) moves[moveListLength ++] = moveInfoDoublePush | ((long) lsb[(int) ((doublePush * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
        }
        return moveListLength;
    }
    //#endregion

    //#region Tacticals
    public static int genTactical(long board0, long board1, long board2, long board3, int status, long[] movesBuffer) {
        final int player = status & PLAYER_BIT;
        final int playerBit = player << PLAYER_SHIFT;
        final int other = 1 ^ player;
        final long colorMask = ~(-(player) ^ board3);
        final long allOccupancy = board0 | board1 | board2;
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;
        final long playerKing = board0 & ~board1 & ~board2 & colorMask;
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;
        final long otherQueensAndRooks = otherQueens | otherRooks;
        final long otherQueensAndBishops = otherQueens | otherBishops;
        final int[] lsb = LSB;
        final int kingSquare = lsb[(int) ((playerKing * DB) >>> 58)];
        final long pextRookMovesFromKing = Pext.rookMoves(kingSquare, allOccupancy);
        final long pextBishopMovesFromKing = Pext.bishopMoves(kingSquare, allOccupancy);
        final long checkers = (LEAP_ATTACKS[kingSquare] & otherKnights)
            | (PAWN_ATTACKS[player][kingSquare] & otherPawns)
            | (KING_ATTACKS[kingSquare] & otherKing)
            | (pextRookMovesFromKing & (otherQueensAndRooks))
            | (pextBishopMovesFromKing & (otherQueensAndBishops));
        int moveListLength = getKingTacticals(board0, board1, board2, board3, kingSquare, playerKing, KING | playerBit, player, allOccupancy, otherOccupancy, lsb, movesBuffer, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
        if((checkers & (checkers - 1L)) != 0L) return moveListLength;
        long responseMask = ~0L;
        final long[] between = BETWEEN;
        if(checkers != 0L) {
            responseMask = checkers;
            if((checkers & (otherQueensAndRooks | otherBishops)) != 0L) responseMask |= between[kingSquare | ((lsb[(int) ((checkers * DB) >>> 58)]) << 6)];
        }
        final long rookBlockers = pextRookMovesFromKing & playerOccupancy;
        final long bishopBlockers = pextBishopMovesFromKing & playerOccupancy;
        final long rookPinners = Pext.rookMoves(kingSquare, allOccupancy ^ rookBlockers) & (otherQueensAndRooks);
        final long bishopPinners = Pext.bishopMoves(kingSquare, allOccupancy ^ bishopBlockers) & (otherQueensAndBishops);
        long pinned = 0L;
        final long pinners = rookPinners | bishopPinners;
        long pinners2 = pinners;
        while(pinners2 != 0L) {
            final long pinner = pinners2 & -pinners2;
            pinners2 ^= pinner;
            pinned |= between[kingSquare | ((lsb[(int) ((pinner * DB) >>> 58)] << 6))] & playerOccupancy;
        }
        moveListLength = getQueenTacticals(board0, board1, board2, board3, ~board0 & board1 & ~board2 & colorMask, QUEEN | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getRookTacticals(board0, board1, board2, board3, board0 & board1 & ~board2 & colorMask, ROOK | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getBishopTacticals(board0, board1, board2, board3, ~board0 & ~board1 & board2 & colorMask, BISHOP | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getKnightTacticals(board0, board1, board2, board3, board0 & ~board1 & board2 & colorMask & ~pinned, KNIGHT | playerBit, otherOccupancy, responseMask, lsb, movesBuffer, moveListLength);
        final int eSquare = status >>> EN_PASSANT_SQUARE_SHIFT & SQUARE_BITS;
        return getPawnTacticals(board0, board1, board2, board3, ~board0 & board1 & board2 & colorMask, PAWN | playerBit, player, playerBit, allOccupancy, (1L << eSquare) & -(long) ((eSquare + 63) >>> 6), eSquare, pinned, pinners, otherOccupancy, responseMask, checkers, lsb, movesBuffer, moveListLength, kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
    }

    private static int getKingTacticals(long board0, long board1, long board2, long board3, int square, long playerKing, int piece, int player, long allOccupancy, long otherOccupancy, int[] lsb, long[] moves, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        int moveListLength = 0;
        final long moveInfo = (long) square
        | ((long) piece << START_PIECE_SHIFT)
        | (WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -(long) player));
        long moveBitboard = KING_ATTACKS[square] & otherOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & (~playerKing) & ~b) | b)) {
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        return moveListLength;
    }

    private static int getQueenTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            long moveBitboard = ((Pext.queenMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb))) & otherOccupancy;
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        return moveListLength;
    }

    private static int getRookTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            long moveBitboard = ((Pext.rookMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb))) & otherOccupancy;
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT)
            | rookCastlingChange(square);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        return moveListLength;
    }

    private static int getBishopTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long pinMask = -((b & pinned) >>> square);
            long moveBitboard = ((Pext.bishopMoves(square, allOccupancy) & responseMask) & (~pinMask | getPinRay(kingSquare, b, pinners, lsb))) & otherOccupancy;
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        return moveListLength;
    }

    private static int getKnightTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long otherOccupancy, long responseMask, int[] lsb, long[] moves, int moveListLength) {
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = leapAttacks[square] & otherOccupancy & responseMask;
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = moveInfo
                | (long) CAPTURE_BITS
                | ((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
            }
        }
        return moveListLength;
    }

    private static int getPawnTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, int player, int playerBit, long allOccupancy, long epBit, int eSquare, long pinned, long pinners, long otherOccupancy, long responseMask, long checkers, int[] lsb, long[] moves, int moveListLength, int kingSquare, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        final int promotionRank = 7 & ~(-player);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] advanceSingle = PAWN_ADVANCE_SINGLE[player];
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT);
            final long moveInfoCapture = moveInfo
            | (long) CAPTURE_BITS;
            final long moveInfoEnPassant = moveInfo
            | (long) EN_PASSANT_BITS;
            final long moveInfoPromotion = moveInfo
            | (long) PROMOTION_BITS;
            final long moveInfoCapturePromotion = moveInfo
            | (long) CAPTURE_PROMOTION_BITS;
            final long pinMask = -((b & pinned) >>> square);
            final long pinRay = ~pinMask | getPinRay(kingSquare, b, pinners, lsb);
            final long attacks = pawnAttacks[square];
            long moveBitboard = attacks & otherOccupancy & responseMask & pinRay;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                final long targetInfo =((long) targetSquare << TARGET_SQUARE_SHIFT)
                | ((long) targetPiece << TARGET_PIECE_SHIFT)
                | capturedRookCastlingChange(targetPiece, targetSquare);
                if((targetSquare >>> 3) == promotionRank) {
                    final long moveInfoTarget = moveInfoCapturePromotion
                    | targetInfo;
                    moves[moveListLength ++] = moveInfoTarget | ((long) (QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                } else moves[moveListLength ++] = moveInfoCapture
                | targetInfo;
            }
            final long epDestination = attacks & epBit & pinRay;
            if(epDestination != 0L) {
                final int capturedSquare = eSquare + (player << 4) - 8;
                final long capturedPawnBit = 1L << capturedSquare;
                final boolean checkResponse = checkers == 0L || (epDestination & responseMask) != 0L || (capturedPawnBit & checkers) != 0L;
                if((otherPawns & capturedPawnBit) != 0L && checkResponse) {
                    final long occupancyAfter = (allOccupancy ^ b ^ capturedPawnBit) | epDestination;
                    if(!isSquareAttackedByPlayer(kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns & ~capturedPawnBit, occupancyAfter)) moves[moveListLength ++] = moveInfoEnPassant | ((long) eSquare << TARGET_SQUARE_SHIFT);
                }
            }
            final long singlePush = advanceSingle[square] & ~allOccupancy;
            if(singlePush != 0L) {
                final int targetSquare = lsb[(int) ((singlePush * DB) >>> 58)];
                if((targetSquare >>> 3) == promotionRank && (singlePush & pinRay & responseMask) != 0L) {
                    final long moveInfoTarget = moveInfoPromotion
                    | ((long) targetSquare << TARGET_SQUARE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((long) (KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    //endregion

    //#region Quiets
    public static int genQuiet(long board0, long board1, long board2, long board3, int status, long[] movesBuffer) {
        final int player = status & PLAYER_BIT;
        final int playerBit = player << PLAYER_SHIFT;
        final int other = 1 ^ player;
        final long colorMask = ~(-(player) ^ board3);
        final long allOccupancy = board0 | board1 | board2;
        final long playerOccupancy = allOccupancy & colorMask;
        final long otherOccupancy = allOccupancy & ~colorMask;
        final long playerKing = board0 & ~board1 & ~board2 & colorMask;
        final long otherKing = board0 & ~board1 & ~board2 & ~colorMask;
        final long otherQueens = ~board0 & board1 & ~board2 & ~colorMask;
        final long otherRooks = board0 & board1 & ~board2 & ~colorMask;
        final long otherBishops = ~board0 & ~board1 & board2 & ~colorMask;
        final long otherKnights = board0 & ~board1 & board2 & ~colorMask;
        final long otherPawns = ~board0 & board1 & board2 & ~colorMask;
        final long otherQueensAndRooks = otherQueens | otherRooks;
        final long otherQueensAndBishops = otherQueens | otherBishops;
        final int[] lsb = LSB;
        final int kingSquare = lsb[(int) ((playerKing * DB) >>> 58)];
        final long pextRookMovesFromKing = Pext.rookMoves(kingSquare, allOccupancy);
        final long pextBishopMovesFromKing = Pext.bishopMoves(kingSquare, allOccupancy);
        final long checkers = (LEAP_ATTACKS[kingSquare] & otherKnights)
        | (PAWN_ATTACKS[player][kingSquare] & otherPawns)
        | (KING_ATTACKS[kingSquare] & otherKing)
        | (pextRookMovesFromKing & (otherQueensAndRooks))
        | (pextBishopMovesFromKing & (otherQueensAndBishops));
        int moveListLength = getKingQuiets(board0, board1, board2, board3, status, kingSquare, KING | playerBit, player, playerBit, playerKing, other, allOccupancy, checkers, lsb, movesBuffer, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
        if((checkers & (checkers - 1L)) != 0L) return moveListLength;
        long responseMask = ~0L;
        final long[] between = BETWEEN;
        if(checkers != 0L) {
            responseMask = checkers;
            if((checkers & (otherQueensAndRooks | otherBishops)) != 0L) responseMask |= between[kingSquare | ((lsb[(int) ((checkers * DB) >>> 58)]) << 6)];
        }
        final long rookBlockers = pextRookMovesFromKing & playerOccupancy;
        final long bishopBlockers = pextBishopMovesFromKing & playerOccupancy;
        final long rookPinners = Pext.rookMoves(kingSquare, allOccupancy ^ rookBlockers) & otherQueensAndRooks;
        final long bishopPinners = Pext.bishopMoves(kingSquare, allOccupancy ^ bishopBlockers) & otherQueensAndBishops;
        long pinned = 0L;
        final long pinners = rookPinners | bishopPinners;
        long pinners2 = pinners;
        while(pinners2 != 0L) {
            final long pinner = pinners2 & -pinners2;
            pinners2 ^= pinner;
            pinned |= between[kingSquare | (lsb[(int) ((pinner * DB) >>> 58)] << 6)] & playerOccupancy;
        }
        moveListLength = getQueenQuiets(QUEEN | playerBit, ~board0 & board1 & ~board2 & colorMask, allOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getRookQuiets(ROOK | playerBit, board0 & board1 & ~board2 & colorMask, allOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getBishopQuiets(BISHOP | playerBit, ~board0 & ~board1 & board2 & colorMask, allOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getKnightQuiets(KNIGHT | playerBit, board0 & ~board1 & board2 & colorMask & ~pinned, allOccupancy, responseMask, lsb, movesBuffer, moveListLength);
        return getPawnQuiets(player, PAWN | playerBit, ~board0 & board1 & board2 & colorMask, allOccupancy, otherOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
    }

    private static int getKingQuiets(long board0, long board1, long board2, long board3, long status, int square, int piece, int player, int playerBit, long playerKing, int other, long allOccupancy, long checkers, int[] lsb, long[] moves, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        int moveListLength = 0;
        final long moveInfo = (long) square
        | ((long) piece << START_PIECE_SHIFT)
        | (long) (WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -(long) player));
        long moveBitboard = KING_ATTACKS[square] & ~allOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & (~playerKing) & ~b) | b)) moves[moveListLength ++] = moveInfo | ((long) targetSquare << TARGET_SQUARE_SHIFT);
        }
        if(checkers == 0L) {
            final int blackMask = -player;
            final int whiteMask = ~blackMask;
            final int backRank = (-player) & 56;
            final int expectedKingSquare = 4 | backRank;
            final int kingSideRookSquare = H1 | backRank;
            final int queenSideRookSquare = A1 | backRank;
            final int rook = ROOK | playerBit;
            final boolean kingSide = square == expectedKingSquare
            && getTargetPiece(board0, board1, board2, board3, kingSideRookSquare) == rook
            && (status & ((WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED & whiteMask)
            | (BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != 0L;
            final boolean queenSide = square == expectedKingSquare
            && getTargetPiece(board0, board1, board2, board3, queenSideRookSquare) == rook
            && (status & ((WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED & whiteMask)
            | (BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != 0L;
            if(kingSide) {
                final long intermediateSquares = (WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask)
                | (BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask);
                if((allOccupancy & intermediateSquares) == 0L
                    && !isSquareAttackedByPlayer(square + 1, 1 ^ player, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy)
                    && !isSquareAttackedByPlayer(square + 2, 1 ^ player, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy)) {
                        moves[moveListLength ++] = moveInfo
                        | (long) CASTLE_BITS
                        | ((long) (square + 2) << TARGET_SQUARE_SHIFT);
                }
            }
            if(queenSide) {
                final long intermediateSquares = (WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask)
                | (BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask);
                if((allOccupancy & intermediateSquares) == 0L
                    && !isSquareAttackedByPlayer(square - 1, 1 ^ player, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy)
                    && !isSquareAttackedByPlayer(square - 2, 1 ^ player, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy)) {
                        moves[moveListLength ++] = moveInfo
                        | (long) CASTLE_BITS
                        | ((long) (square - 2) << TARGET_SQUARE_SHIFT);
                }
            }
        }
        return moveListLength;
    }

    private static int getQueenQuiets(int piece, long pieceBitboard, long allOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.queenMoves(square, allOccupancy) & ~allOccupancy & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            final long moveInfo = (long) square | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getRookQuiets(int piece, long pieceBitboard, long allOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.rookMoves(square, allOccupancy) & ~allOccupancy & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            final long moveInfo = (long) square
            | ((long) piece << START_PIECE_SHIFT)
            | rookCastlingChange(square);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getBishopQuiets(int piece, long pieceBitboard, long allOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.bishopMoves(square, allOccupancy) & ~allOccupancy & responseMask;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            final long moveInfo = (long) square | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getKnightQuiets(int piece, long pieceBitboard, long allOccupancy, long responseMask, int[] lsb, long[] moves, int moveListLength) {
        final long[] leapAttacks = LEAP_ATTACKS;
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = leapAttacks[square] & ~allOccupancy & responseMask;
            final long moveInfo = (long) square | ((long) piece << START_PIECE_SHIFT);
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] = moveInfo
                | ((long) lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
            }
        }
        return moveListLength;
    }

    private static int getPawnQuiets(int player, int piece, long pieceBitboard, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        final int promotionRank = 7 & ~(-player);
        final long[] advanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] advanceDouble = PAWN_ADVANCE_DOUBLE[player];
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long singlePush = advanceSingle[square] & ~allOccupancy;
            if(singlePush == 0L) continue;
            final int targetSquare = lsb[(int) ((singlePush * DB) >>> 58)];
            if((targetSquare >>> 3) == promotionRank) continue;
            final long pinMask = -((b & pinned) >>> square);
            final long pinRay = ~pinMask | getPinRay(kingSquare, b, pinners, lsb);
            final long moveInfo = (long) square | ((long) piece << START_PIECE_SHIFT);
            if((singlePush & pinRay & responseMask) != 0L) moves[moveListLength ++] = moveInfo
            | (long) PAWN_PUSH_BITS
            | ((long) targetSquare << TARGET_SQUARE_SHIFT);
            final long doublePush = advanceDouble[square] & ~allOccupancy & pinRay & responseMask;
            if(doublePush != 0L) moves[moveListLength ++] = moveInfo
            | (long) PAWN_DOUBLE_PUSH_BITS
            | ((long) lsb[(int) ((doublePush * DB) >>> 58)] << TARGET_SQUARE_SHIFT);
        }
        return moveListLength;
    }

    //#endregion

    //#region Data
    private Gen() {}

    private static final int PLAYER_BIT = 0b1;
    private static final int PLAYER_SHIFT = 3;
    private static final int EN_PASSANT_SQUARE_SHIFT = 5;
    private static final int SQUARE_BITS = 0b111111;

    private static final int KING = 1;
    private static final int QUEEN = 2;
    private static final int ROOK = 3;
    private static final int BISHOP = 4;
    private static final int KNIGHT = 5;
    private static final int PAWN = 6;

    private static final int[] LSB = {
        0,  1, 48,  2, 57, 49, 28,  3,
        61, 58, 50, 42, 38, 29, 17,  4,
        62, 55, 59, 36, 53, 51, 43, 22,
        45, 39, 33, 30, 24, 18, 12,  5,
        63, 47, 56, 27, 60, 41, 37, 16,
        54, 35, 52, 21, 44, 32, 23, 11,
        46, 26, 40, 15, 34, 20, 31, 10,
        25, 14, 19,  9, 13,  8,  7,  6
    };

    private static final long DB = 0x03f79d71b4cb0a89L;

    private static final long[] KING_ATTACKS = new long[64];
    private static final long[] LEAP_ATTACKS = new long[64];
    private static final long[][] PAWN_ATTACKS = new long[2][64];
    private static final long[][] PAWN_ADVANCE_SINGLE = new long[2][64];
    private static final long[][] PAWN_ADVANCE_DOUBLE = new long[2][64];
    private static final long[] BETWEEN = new long[64 * 64];
    
    private static final int A1 = 0;
    private static final int H1 = 7;
    private static final int A8 = 56;
    private static final int H8 = 63;
    private static final long FILE_A_BITBOARD = 0x0101010101010101L;
    private static final long FILE_B_BITBOARD = 0x0202020202020202L;
    private static final long FILE_G_BITBOARD = 0x4040404040404040L;
    private static final long FILE_H_BITBOARD = 0x8080808080808080L;
    private static final int TARGET_SQUARE_SHIFT = 6;
    private static final int PROMOTE_PIECE_SHIFT = 12;
    private static final int START_PIECE_SHIFT = 16;
    private static final int TARGET_PIECE_SHIFT = 20;
    private static final int MOVE_TYPE_SHIFT = 24;
    private static final int PAWN_PUSH_BITS = 0b001 << MOVE_TYPE_SHIFT;
    private static final int PAWN_DOUBLE_PUSH_BITS = 0b010 << MOVE_TYPE_SHIFT;
    private static final int CASTLE_BITS = 0b011 << MOVE_TYPE_SHIFT;
    private static final int CAPTURE_BITS = 0b100 << MOVE_TYPE_SHIFT;
    private static final int EN_PASSANT_BITS = 0b101 << MOVE_TYPE_SHIFT;
    private static final int PROMOTION_BITS = 0b110 << MOVE_TYPE_SHIFT;
    private static final int CAPTURE_PROMOTION_BITS = 0b111 << MOVE_TYPE_SHIFT;
    public static final int CASTLING_CHANGE_SHIFT = 28;
    public static final long CASTLING_CHANGE_MASK = 0b1111L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_KINGSIDE_CHANGE_MASK = 0b0001L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_QUEENSIDE_CHANGE_MASK = 0b0010L << CASTLING_CHANGE_SHIFT;
    public static final long BLACK_KINGSIDE_CHANGE_MASK = 0b0100L << CASTLING_CHANGE_SHIFT;
    public static final long BLACK_QUEENSIDE_CHANGE_MASK = 0b1000L << CASTLING_CHANGE_SHIFT;
    public static final long WHITE_CASTLING_CHANGE_MASK = WHITE_KINGSIDE_CHANGE_MASK | WHITE_QUEENSIDE_CHANGE_MASK;
    public static final long BLACK_CASTLING_CHANGE_MASK = BLACK_KINGSIDE_CHANGE_MASK | BLACK_QUEENSIDE_CHANGE_MASK;
    private static final int WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b10;
    private static final int WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b100;
    private static final int BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED = 0b1000;
    private static final int BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED = 0b10000;
    private static final long WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0000000000000060L;
    private static final long WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x000000000000000eL;
    private static final long BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x6000000000000000L;
    private static final long BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES = 0x0e00000000000000L;

    static {
        for(int square = A1; square <= H8; square ++) {
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
            PAWN_ADVANCE_SINGLE[0][square] = squareBit << 8;
            PAWN_ADVANCE_SINGLE[1][square] = squareBit >>> 8;
            if((square >>> 3) == 1) PAWN_ADVANCE_DOUBLE[0][square] = squareBit << 16;
            if((square >>> 3) == 6) PAWN_ADVANCE_DOUBLE[1][square] = squareBit >>> 16;
            if(square > H8 - 2) continue;
            for(int otherSquare = square + 1; otherSquare <= H8; otherSquare ++) {
                final int squareRow = square >>> 3;
                final int squareFile = square & 7;
                final int otherRow = otherSquare >>> 3;
                final int otherFile = otherSquare & 7;
                long betweenBits = 0L;
                if(squareRow == otherRow) for(int betweenSquare = square + 1; betweenSquare < otherSquare; betweenSquare ++) betweenBits |= (1L << betweenSquare);
                if(squareFile == otherFile) for(int betweenSquare = square + 8; betweenSquare < otherSquare; betweenSquare += 8) betweenBits |= (1L << betweenSquare);
                if((otherRow - squareRow) == (otherFile - squareFile)) for(int betweenSquare = square + 9; betweenSquare < otherSquare; betweenSquare += 9) betweenBits |= (1L << betweenSquare);
                if((otherRow - squareRow) == (squareFile - otherFile)) for(int betweenSquare = square + 7; betweenSquare < otherSquare; betweenSquare += 7) betweenBits |= (1L << betweenSquare);
                BETWEEN[square | (otherSquare << 6)] = betweenBits;
                BETWEEN[otherSquare | (square << 6)] = betweenBits;
            }
        }
    }
    //#endregion

    //#region Helpers
    private static boolean isSquareAttackedByPlayer(int square, int player, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns, long allOccupancy) {
        if((LEAP_ATTACKS[square] & otherKnights) !=0L) return true;
        if((PAWN_ATTACKS[1 ^ player][square] & otherPawns) != 0L) return true;
        if((KING_ATTACKS[square] & otherKing) != 0L) return true;
        if((Pext.rookMoves(square, allOccupancy) & (otherQueens | otherRooks)) != 0L) return true;
        return (Pext.bishopMoves(square, allOccupancy) & (otherQueens | otherBishops)) != 0L;
    }

    private static int getTargetPiece(long board0, long board1, long board2, long board3, int square) {
        return (int) (((board3 >>> square & 1) << 3) | ((board2 >>> square & 1) << 2) | ((board1 >>> square & 1) << 1) | (board0 >>> square & 1));
    }

    private static long rookCastlingChange(int square) {
        final int targetSquareA1 = (square ^ A1);
        final int targetSquareH1 = (square ^ H1);
        final int targetSquareA8 = (square ^ A8);
        final int targetSquareH8 = (square ^ H8);
        return (WHITE_QUEENSIDE_CHANGE_MASK & ((targetSquareA1 | -targetSquareA1) >>> 31) -1L
            | WHITE_KINGSIDE_CHANGE_MASK & ((targetSquareH1 | -targetSquareH1) >>> 31) -1L
            | BLACK_QUEENSIDE_CHANGE_MASK & ((targetSquareA8 | -targetSquareA8) >>> 31) -1L
            | BLACK_KINGSIDE_CHANGE_MASK & ((targetSquareH8 | -targetSquareH8) >>> 31) -1L);
    }

    private static long capturedRookCastlingChange(int targetPiece, int targetSquare) {
        final int rook = (targetPiece & ROOK) ^ ROOK;
        final long rookMask = ((rook | -rook) >>> 31) - 1L;
        final int targetSquareA1 = (targetSquare ^ A1);
        final int targetSquareH1 = (targetSquare ^ H1);
        final int targetSquareA8 = (targetSquare ^ A8);
        final int targetSquareH8 = (targetSquare ^ H8);
        return (rookMask
            & (WHITE_QUEENSIDE_CHANGE_MASK & ((targetSquareA1 | -targetSquareA1) >>> 31) -1L
            | WHITE_KINGSIDE_CHANGE_MASK & ((targetSquareH1 | -targetSquareH1) >>> 31) -1L
            | BLACK_QUEENSIDE_CHANGE_MASK & ((targetSquareA8 | -targetSquareA8) >>> 31) -1L
            | BLACK_KINGSIDE_CHANGE_MASK & ((targetSquareH8 | -targetSquareH8) >>> 31) -1L)
        );
    }

    private static long getPinRay(int kingSquare, long pieceBit, long pinners, int[] lsb) {
        while(pinners != 0L) {
            final long pinner = pinners & -pinners;
            pinners ^= pinner;
            final int pinnerSquare = lsb[(int) (((pinner & -pinner) * DB) >>> 58)];;
            final long between = BETWEEN[kingSquare | (pinnerSquare << 6)];
            if((between & pieceBit) != 0L) return between | pinner;
        }
        return 0L;
    }
    //#endregion

}

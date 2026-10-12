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
        moveListLength = getRookEvasions(board0, board1, board2, board3, board0 & board1 & ~board2 & colorMask, ROOK | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, rookPinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getBishopEvasions(board0, board1, board2, board3, ~board0 & ~board1 & board2 & colorMask, BISHOP | playerBit, allOccupancy, otherOccupancy, responseMask, pinned, bishopPinners, lsb, movesBuffer, moveListLength, kingSquare);
        moveListLength = getKnightEvasions(board0, board1, board2, board3, board0 & ~board1 & board2 & colorMask & ~pinned, KNIGHT | playerBit, allOccupancy, otherOccupancy, responseMask, lsb, movesBuffer, moveListLength);
        final int eSquare = status >>> EN_PASSANT_SQUARE_SHIFT & SQUARE_BITS;
        return getPawnEvasions(board0, board1, board2, board3, ~board0 & board1 & board2 & colorMask, PAWN | playerBit, player, playerBit, (1L << eSquare) & -(long) ((eSquare + 63) >>> 6), eSquare, pinned, pinners, allOccupancy, otherOccupancy, responseMask, checkers, lsb, movesBuffer, moveListLength, kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns);
    }

    private static int getKingEvasions(long board0, long board1, long board2, long board3, int square, int player, long playerKing, int piece, long allOccupancy, long otherOccupancy, int[] lsb, long[] moves, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        int moveListLength = 0;
        final long destinations = KING_ATTACKS[square];
        final int moveInfo =
            square |
            (piece << START_PIECE_SHIFT) |
            ((WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -player)) << CASTLING_CHANGE_SHIFT)
        ;
        long moveBitboard = destinations & otherOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy & ~playerKing)) {
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            }
        }
        moveBitboard = destinations & ~allOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & ~playerKing) | b))
                moves[moveListLength ++] =
                    moveInfo |
                    (targetSquare << TARGET_SQUARE_SHIFT)
                ;
        }
        return moveListLength;
    }

    private static int getQueenEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long destinations = Pext.queenMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) destinations &= getPinRay(kingSquare, b, pinners, lsb);
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            }
        }
        return moveListLength;
    }

    private static int getRookEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long destinations = Pext.rookMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) destinations &= getPinRay(kingSquare, b, pinners, lsb);
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT) |
                (rookCastlingChange(square) << CASTLING_CHANGE_SHIFT)
            ;
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            }
        }
        return moveListLength;
    }

    private static int getBishopEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long destinations = Pext.bishopMoves(square, allOccupancy) & responseMask;
            if((b & pinned) != 0L) destinations &= getPinRay(kingSquare, b, pinners, lsb);
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
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
            long destinations = leapAttacks[square] & responseMask;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            long moveBitboard = destinations & otherOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            }
            moveBitboard = destinations & ~allOccupancy;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            }
        }
        return moveListLength;
    }

    private static int getPawnEvasions(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, int player, int playerBit, long epBit, int eSquare, long pinned, long pinners, long allOccupancy, long otherOccupancy, long responseMask, long checkers, int[] lsb, long[] moves, int moveListLength, int kingSquare, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        final int promotionRank = 7 & ~(-player);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] advanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] advanceDouble = PAWN_ADVANCE_DOUBLE[player];
        final int pieceShifted = piece << START_PIECE_SHIFT;
        final int pawnMoveDirectionDelta = 8 - (player << 4);
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final int moveInfo =
                square |
                pieceShifted
            ;
            final int moveInfoPawnPush =
                moveInfo |
                PAWN_PUSH_BITS
            ;
            final int moveInfoDoublePush =
                moveInfo |
                PAWN_DOUBLE_PUSH_BITS
            ;
            final int moveInfoPromotion =
                moveInfo |
                PROMOTION_BITS
            ;
            final int moveInfoCapture =
                moveInfo |
                CAPTURE_BITS
            ;
            final int moveInfoCapturePromotion =
                moveInfo |
                CAPTURE_PROMOTION_BITS
            ;
            final int moveInfoEnPassant =
                moveInfo |
                EN_PASSANT_BITS
            ;
            final long pinRay = (b & pinned) != 0L ? getPinRay(kingSquare, b, pinners, lsb) : ~0L;
            long destinations = pawnAttacks[square] & pinRay;
            long moveBitboard = destinations & otherOccupancy & responseMask;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                final int targetInfo =
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
                if((targetSquare >>> 3) == promotionRank) {
                    final int moveInfoTarget =
                        moveInfoCapturePromotion |
                        targetInfo
                    ;
                    moves[moveListLength ++] = moveInfoTarget | ((QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = moveInfoCapture | targetInfo;
                }
            }
            final long epDestination = destinations & epBit;
            if(epDestination != 0L) {
                final int capturedSquare = eSquare + (player << 4) - 8;
                final long capturedPawnBit = 1L << capturedSquare;
                if((otherPawns & capturedPawnBit) != 0L && (checkers == 0L || (epDestination & responseMask) != 0L || (capturedPawnBit & checkers) != 0L)) {
                    final long occupancyAfter = (allOccupancy ^ b ^ capturedPawnBit) | epDestination;
                    if(!isSquareAttackedByPlayer(kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns & ~capturedPawnBit, occupancyAfter)) moves[moveListLength ++] = moveInfoEnPassant | ((long) eSquare << TARGET_SQUARE_SHIFT);
                }
            }
            final long singlePush = advanceSingle[square] & ~allOccupancy;
            if(singlePush == 0L) continue;
            final int targetSquare = square + pawnMoveDirectionDelta;
            if((singlePush & pinRay & responseMask) != 0L) {
                if((targetSquare >>> 3) == promotionRank) {
                    final int moveInfoTarget =
                        moveInfoPromotion |
                        (targetSquare << TARGET_SQUARE_SHIFT)
                    ;
                    moves[moveListLength ++] = moveInfoTarget | ((QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                } else {
                    moves[moveListLength ++] = moveInfoPawnPush | (targetSquare << TARGET_SQUARE_SHIFT);
                }
            }
            final long doublePush = advanceDouble[square] & ~allOccupancy & pinRay & responseMask;
            if(doublePush != 0L) moves[moveListLength ++] = moveInfoDoublePush | ((square + pawnMoveDirectionDelta + pawnMoveDirectionDelta) << TARGET_SQUARE_SHIFT);
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
        final int moveInfo =
            square |
            (piece << START_PIECE_SHIFT) |
            ((WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -player)) << CASTLING_CHANGE_SHIFT)
        ;
        long moveBitboard = KING_ATTACKS[square] & otherOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy & ~playerKing)) {
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? (capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT) : 0)
                ;
            }
        }
        return moveListLength;
    }

    private static int getQueenTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.queenMoves(square, allOccupancy) & responseMask & otherOccupancy;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] = 
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            } while(moveBitboard != 0L);
        }
        return moveListLength;
    }

    private static int getRookTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.rookMoves(square, allOccupancy) & responseMask & otherOccupancy;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            if(moveBitboard == 0L) continue;
            final int moveInfo = 
                square |
                (piece << START_PIECE_SHIFT) |
                (rookCastlingChange(square) << CASTLING_CHANGE_SHIFT);
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            } while(moveBitboard != 0L);
        }
        return moveListLength;
    }

    private static int getBishopTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, long allOccupancy, long otherOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            long moveBitboard = Pext.bishopMoves(square, allOccupancy) & responseMask & otherOccupancy;
            if((b & pinned) != 0L) moveBitboard &= getPinRay(kingSquare, b, pinners, lsb);
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            } while(moveBitboard != 0L);
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
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                moves[moveListLength ++] =
                    moveInfo |
                    CAPTURE_BITS |
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
            } while(moveBitboard != 0L);
        }
        return moveListLength;
    }

    private static int getPawnTacticals(long board0, long board1, long board2, long board3, long pieceBitboard, int piece, int player, int playerBit, long allOccupancy, long epBit, int eSquare, long pinned, long pinners, long otherOccupancy, long responseMask, long checkers, int[] lsb, long[] moves, int moveListLength, int kingSquare, int other, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        final int promotionRank = 7 & ~(-player);
        final long[] pawnAttacks = PAWN_ATTACKS[player];
        final long[] advanceSingle = PAWN_ADVANCE_SINGLE[player];
        final int pawnMoveDirectionDelta = 8 - (player << 4);
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            final int moveInfoCapture =
                moveInfo | 
                CAPTURE_BITS
            ;
            final int moveInfoEnPassant =
                moveInfo |
                EN_PASSANT_BITS
            ;
            final int moveInfoPromotion =
                moveInfo |
                PROMOTION_BITS
            ;
            final int moveInfoCapturePromotion =
                moveInfo |
                CAPTURE_PROMOTION_BITS
            ;
            final long pinRay = (b & pinned) != 0L ? getPinRay(kingSquare, b, pinners, lsb) : ~0L;
            final long destinations = pawnAttacks[square] & pinRay;
            long moveBitboard = destinations & otherOccupancy & responseMask;
            while(moveBitboard != 0L) {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                final int targetSquare = lsb[(int) ((b2 * DB) >>> 58)];
                final int targetPiece = getTargetPiece(board0, board1, board2, board3, targetSquare);
                final int targetInfo =
                    (targetSquare << TARGET_SQUARE_SHIFT) |
                    (targetPiece << TARGET_PIECE_SHIFT) |
                    ((targetPiece & 7) == ROOK ? capturedRookCastlingChange(targetPiece, targetSquare) << CASTLING_CHANGE_SHIFT : 0)
                ;
                if((targetSquare >>> 3) == promotionRank) {
                    final int moveInfoTarget =
                        moveInfoCapturePromotion |
                        targetInfo
                    ;
                    moves[moveListLength ++] = moveInfoTarget | ((QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
                } else moves[moveListLength ++] =
                    moveInfoCapture |
                    targetInfo
                ;
            }
            final long epDestination = destinations & epBit;
            if(epDestination != 0L) {
                final int capturedSquare = eSquare + (player << 4) - 8;
                final long capturedPawnBit = 1L << capturedSquare;
                if((otherPawns & capturedPawnBit) != 0L && (checkers == 0L || (epDestination & responseMask) != 0L || (capturedPawnBit & checkers) != 0L)) {
                    final long occupancyAfter = (allOccupancy ^ b ^ capturedPawnBit) | epDestination;
                    if(!isSquareAttackedByPlayer(kingSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns & ~capturedPawnBit, occupancyAfter)) moves[moveListLength ++] = moveInfoEnPassant | (eSquare << TARGET_SQUARE_SHIFT);
                }
            }
            final long singlePush = advanceSingle[square] & ~allOccupancy;
            if(singlePush != 0L) {
                final int targetSquare = square + pawnMoveDirectionDelta;
                if((targetSquare >>> 3) == promotionRank && (singlePush & pinRay & responseMask) != 0L) {
                    final long moveInfoTarget =
                        moveInfoPromotion |
                        (targetSquare << TARGET_SQUARE_SHIFT)
                    ;
                    moves[moveListLength ++] = moveInfoTarget | ((QUEEN  | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((ROOK   | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((BISHOP | playerBit) << PROMOTE_PIECE_SHIFT);
                    moves[moveListLength ++] = moveInfoTarget | ((KNIGHT | playerBit) << PROMOTE_PIECE_SHIFT);
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
        return getPawnQuiets(player, PAWN | playerBit, ~board0 & board1 & board2 & colorMask, allOccupancy, responseMask, pinned, pinners, lsb, movesBuffer, moveListLength, kingSquare);
    }

    private static int getKingQuiets(long board0, long board1, long board2, long board3, int status, int square, int piece, int player, int playerBit, long playerKing, int other, long allOccupancy, long checkers, int[] lsb, long[] moves, long otherKing, long otherQueens, long otherRooks, long otherBishops, long otherKnights, long otherPawns) {
        int moveListLength = 0;
        final int moveInfo =
            square |
            (piece << START_PIECE_SHIFT) |
            ((WHITE_CASTLING_CHANGE_MASK ^ ((WHITE_CASTLING_CHANGE_MASK ^ BLACK_CASTLING_CHANGE_MASK) & -player)) << CASTLING_CHANGE_SHIFT)
        ;
        long moveBitboard = KING_ATTACKS[square] & ~allOccupancy;
        while(moveBitboard != 0L) {
            final long b = moveBitboard & -moveBitboard;
            moveBitboard ^= b;
            final int targetSquare = lsb[(int) ((b * DB) >>> 58)];
            if(!isSquareAttackedByPlayer(targetSquare, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, (allOccupancy & ~playerKing) | b)) moves[moveListLength ++] = moveInfo | (targetSquare << TARGET_SQUARE_SHIFT);
        }
        if(checkers == 0L) {
            final int blackMask = -player;
            final int whiteMask = ~blackMask;
            final int backRank = 56 & blackMask;
            final int expectedKingSquare = 4 | backRank;
            final int kingSideRookSquare = H1 | backRank;
            final int queenSideRookSquare = A1 | backRank;
            final int rook = ROOK | playerBit;
            final boolean kingSide =
                square == expectedKingSquare &&
                (status & ((WHITE_KINGSIDE_CASTLING_BIT_UNSHIFTED & whiteMask) |
                (BLACK_KINGSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != 0L &&
                getTargetPiece(board0, board1, board2, board3, kingSideRookSquare) == rook
            ;
            final boolean queenSide =
                square == expectedKingSquare &&
                (status & ((WHITE_QUEENSIDE_CASTLING_BIT_UNSHIFTED & whiteMask) |
                (BLACK_QUEENSIDE_CASTLING_BIT_UNSHIFTED & blackMask))) != 0L &&
                getTargetPiece(board0, board1, board2, board3, queenSideRookSquare) == rook
            ;
            if(kingSide) {
                final long intermediateSquares =
                    (WHITE_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask) |
                    (BLACK_KINGSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask)
                ;
                if((allOccupancy & intermediateSquares) == 0L &&
                    !isSquareAttackedByPlayer(square + 1, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy ^ playerKing) &&
                    !isSquareAttackedByPlayer(square + 2, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy ^ playerKing)) {
                        moves[moveListLength ++] =
                            moveInfo |
                            CASTLE_BITS |
                            ((square + 2) << TARGET_SQUARE_SHIFT)
                    ;
                }
            }
            if(queenSide) {
                final long intermediateSquares =
                    (WHITE_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & whiteMask) |
                    (BLACK_QUEENSIDE_CASTLING_INTERMEDIATE_SQUARES & blackMask)
                ;
                if((allOccupancy & intermediateSquares) == 0L &&
                    !isSquareAttackedByPlayer(square - 1, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy ^ playerKing) &&
                    !isSquareAttackedByPlayer(square - 2, other, otherKing, otherQueens, otherRooks, otherBishops, otherKnights, otherPawns, allOccupancy ^ playerKing)) {
                        moves[moveListLength ++] =
                        moveInfo |
                        CASTLE_BITS |
                        ((square - 2) << TARGET_SQUARE_SHIFT)
                    ;
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
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            } while(moveBitboard != 0L);
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
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT) |
                (rookCastlingChange(square) << CASTLING_CHANGE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            } while(moveBitboard != 0L);
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
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            } while(moveBitboard != 0L);
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
            if(moveBitboard == 0L) continue;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            do {
                final long b2 = moveBitboard & -moveBitboard;
                moveBitboard ^= b2;
                moves[moveListLength ++] =
                    moveInfo |
                    (lsb[(int) ((b2 * DB) >>> 58)] << TARGET_SQUARE_SHIFT)
                ;
            } while(moveBitboard != 0L);
        }
        return moveListLength;
    }

    private static int getPawnQuiets(int player, int piece, long pieceBitboard, long allOccupancy, long responseMask, long pinned, long pinners, int[] lsb, long[] moves, int moveListLength, int kingSquare) {
        final int promotionRank = 7 & ~(-player);
        final long[] advanceSingle = PAWN_ADVANCE_SINGLE[player];
        final long[] advanceDouble = PAWN_ADVANCE_DOUBLE[player];
        final int pawnMoveDirectionDelta = 8 - (player << 4);
        while(pieceBitboard != 0L) {
            final long b = pieceBitboard & -pieceBitboard;
            pieceBitboard ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            final long singlePush = advanceSingle[square] & ~allOccupancy;
            if(singlePush == 0L) continue;
            final int targetSquare = square + pawnMoveDirectionDelta;
            if((targetSquare >>> 3) == promotionRank) continue;
            final long pinRayAndResponseMask = ((b & pinned) != 0L ? getPinRay(kingSquare, b, pinners, lsb) : ~0L) & responseMask;
            final int moveInfo =
                square |
                (piece << START_PIECE_SHIFT)
            ;
            if((singlePush & pinRayAndResponseMask) != 0L) moves[moveListLength ++] = moveInfo | PAWN_PUSH_BITS | (targetSquare << TARGET_SQUARE_SHIFT);
            final long doublePush = advanceDouble[square] & ~allOccupancy & pinRayAndResponseMask;
            if(doublePush != 0L) moves[moveListLength ++] =
                moveInfo |
                PAWN_DOUBLE_PUSH_BITS |
                ((square + pawnMoveDirectionDelta + pawnMoveDirectionDelta) << TARGET_SQUARE_SHIFT)
            ;
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
    public static final int CASTLING_CHANGE_MASK = 0b1111 << CASTLING_CHANGE_SHIFT;
    public static final int WHITE_KINGSIDE_CHANGE_MASK = 0b0001;
    public static final int WHITE_QUEENSIDE_CHANGE_MASK = 0b0010;
    public static final int BLACK_KINGSIDE_CHANGE_MASK = 0b0100;
    public static final int BLACK_QUEENSIDE_CHANGE_MASK = 0b1000;
    public static final int WHITE_CASTLING_CHANGE_MASK = WHITE_KINGSIDE_CHANGE_MASK | WHITE_QUEENSIDE_CHANGE_MASK;
    public static final int BLACK_CASTLING_CHANGE_MASK = BLACK_KINGSIDE_CHANGE_MASK | BLACK_QUEENSIDE_CHANGE_MASK;
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

    private static int rookCastlingChange(int square) {
        final int targetSquareA1 = (square ^ A1);
        final int targetSquareH1 = (square ^ H1);
        final int targetSquareA8 = (square ^ A8);
        final int targetSquareH8 = (square ^ H8);
        return (WHITE_QUEENSIDE_CHANGE_MASK & ((targetSquareA1 | -targetSquareA1) >>> 31) -1
            | WHITE_KINGSIDE_CHANGE_MASK & ((targetSquareH1 | -targetSquareH1) >>> 31) -1
            | BLACK_QUEENSIDE_CHANGE_MASK & ((targetSquareA8 | -targetSquareA8) >>> 31) -1
            | BLACK_KINGSIDE_CHANGE_MASK & ((targetSquareH8 | -targetSquareH8) >>> 31) -1);
    }

    private static int capturedRookCastlingChange(int targetPiece, int targetSquare) {
        final int targetSquareA1 = (targetSquare ^ A1);
        final int targetSquareH1 = (targetSquare ^ H1);
        final int targetSquareA8 = (targetSquare ^ A8);
        final int targetSquareH8 = (targetSquare ^ H8);
        return (
            (WHITE_QUEENSIDE_CHANGE_MASK & ((targetSquareA1 | -targetSquareA1) >>> 31) -1
            | WHITE_KINGSIDE_CHANGE_MASK & ((targetSquareH1 | -targetSquareH1) >>> 31) -1
            | BLACK_QUEENSIDE_CHANGE_MASK & ((targetSquareA8 | -targetSquareA8) >>> 31) -1
            | BLACK_KINGSIDE_CHANGE_MASK & ((targetSquareH8 | -targetSquareH8) >>> 31) -1)
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

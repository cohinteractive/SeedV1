import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Nnue {

    public static void main(String[] args) throws IOException {
        Nnue nnue = new Nnue();
        long[] board = Board.fromFen("8/4K3/6k1/2R5/2R5/8/8/8 w - - 23 1");
        int score = nnue.evaluate(board[0], board[1], board[2], board[3], (int) board[4]);
        System.out.println(score);
    }
    
    public Nnue() throws IOException {
        this(null);
    }

    public Nnue(int[] expectedDescriptor) throws IOException {
        if(expectedDescriptor != null && expectedDescriptor.length != 8)
            throw new IllegalArgumentException("Expected 8 descriptor integers");
        byte[] data;
        try (InputStream stream = Nnue.class.getResourceAsStream("conventional-cp64.cnnue")) {
            if(stream == null)
                throw new FileNotFoundException("conventional-cp64.nnue");
            data = stream.readAllBytes();
        }
        if(data.length != FILE_SIZE)
            throw new IOException("Invalid NNUE file size: " + data.length);
        ByteBuffer buffer = ByteBuffer
            .wrap(data)
            .order(ByteOrder.LITTLE_ENDIAN);
        byte[] magic = new byte[8];
        buffer.get(magic);
        if(!Arrays.equals(magic, MAGIC))
            throw new IOException("Invalid NNUE magic");
        int[] descriptor = new int[8];
        for(int i = 0; i < descriptor.length; i ++)
            descriptor[i] = buffer.getInt();
        if(expectedDescriptor != null && !Arrays.equals(descriptor, expectedDescriptor))
            throw new IOException("Incompatible NNUE network descriptor");
        byte[] expectedHash = new byte[32];
        buffer.get(expectedHash);
        byte[] actualHash;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(data, HEADER_SIZE, PAYLOAD_SIZE);
            actualHash = sha.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
        if(!MessageDigest.isEqual(expectedHash, actualHash))
            throw new IOException("NNUE payload checksum mismatch");
        int parameterBytes = 2 * (
            weights.length +
            biases.length +
            outputWeights.length +
            outputBiases.length
        );
        if(parameterBytes != PAYLOAD_SIZE)
            throw new IllegalStateException("NNUE parameter size mismatch");
        for(int i = 0; i < weights.length; i ++)
            weights[i] = buffer.getShort();
        for(int i = 0; i < biases.length; i ++)
            biases[i] = buffer.getShort();
        for(int i = 0; i < outputWeights.length; i ++)
            outputWeights[i] = buffer.getShort();
        for(int i = 0; i < outputBiases.length; i ++)
            outputBiases[i] = buffer.getShort();
        if(buffer.hasRemaining())
            throw new IOException("Unexpected trailing NNUE data");
    }

    public int evaluate(long board0, long board1, long board2, long board3, int status) {
        rebuild(board0, board1, board2, board3, acc);
        return score(board0, board1, board2, board3, status, acc);
    }

    void rebuild(long board0, long board1, long board2, long board3, int[] acc) {
        for(int i = 0; i < 256; i ++) {
            acc[i] = biases[i];
            acc[i + 256] = biases[i];
        }
        final int[] lsb = LSB;
        long kings = board0 & ~board1 & ~board2;
        int whiteKing = lsb[(int) (((kings & ~board3) * DB) >>> 58)];
        int blackKing = lsb[(int) (((kings & board3) * DB) >>> 58)];
        long occupied = board0 | board1 | board2;
        while(occupied != 0L) {
            final long b = occupied & -occupied;
            occupied ^= b;
            final int square = lsb[(int) ((b * DB) >>> 58)];
            int piece = (int) (((board0 >>> square) & 1) | (((board1 >>> square) & 1) << 1) | (((board2 >>> square) & 1) << 2) | ((( board3 >>> square) & 1) << 3));
            int white = featureIndex(square, piece, whiteKing, 0) << 8;
            int black = featureIndex(square, piece, blackKing, 1) << 8;
            for(int i = 0; i < 256; i ++) {
                acc[i] += weights[white + i];
                acc[i + 256] += weights[black + i];
            }
        }
    }

    void update(long pboard0, long pboard1, long pboard2, long pboard3, long cboard0, long cboard1, long cboard2, long cboard3, int[] pAcc, int[] cAcc) {
        long changed = (pboard0 ^ cboard0) | (pboard1 ^ cboard1) | (pboard2 ^ cboard2) | (pboard3 ^ cboard3);
        if(changed == 0L) {
            System.arraycopy(pAcc, 0, cAcc, 0, 512);
            return;
        }
        final int[] lsb = LSB;
        long pKings = pboard0 & ~pboard1 & ~pboard2;
        long cKings = cboard0 & ~cboard1 & ~cboard2;
        for(int player = 0; player < 2; player ++) {
            long pMask = ~(-(player) ^ pboard3);
            long cMask = ~(-(player) ^ cboard3);
            int pKing = lsb[(int) (((pKings & pMask) * DB) >>> 58)];
            int cKing = lsb[(int) (((cKings & cMask) * DB) >>> 58)];
            int offset = player << 8;
            if(featureIndex(0, 1, pKing, player) != featureIndex(0, 1, cKing, player)) {
                for(int i = 0; i < 256; i ++)
                    cAcc[offset + i] = biases[i];
                long occupied = cboard0 | cboard1 | cboard2;
                while(occupied != 0L) {
                    long bit = occupied & -occupied;
                    occupied ^= bit;
                    int sq = lsb[(int) ((bit * DB) >>> 58)];
                    int piece = (int) (((cboard0 >>> sq) & 1) | (((cboard1 >>> sq) & 1) << 1) | (((cboard2 >>> sq) & 1) << 2) | (((cboard3 >>> sq) & 1) << 3));
                    int index = featureIndex(sq, piece, cKing, player) << 8;
                    for(int i = 0; i < 256; i ++)
                        cAcc[offset + i] += weights[index + i];
                }
                continue;
            }
            System.arraycopy(pAcc, offset, cAcc, offset, 256);
            long squares = changed;
            while(squares != 0L) {
                long bit = squares & -squares;
                squares ^= bit;
                int sq = lsb[(int) ((bit * DB) >>> 58)];
                int oldPiece = (int) (((pboard0 >>> sq) & 1) | (((pboard1 >>> sq) & 1) << 1) | (((pboard2 >>> sq) & 1) << 2) | (((pboard3 >>> sq) & 1) << 3));
                int newPiece = (int) (((cboard0 >>> sq) & 1) | (((cboard1 >>> sq) & 1) << 1) | (((cboard2 >>> sq) & 1) << 2) | (((cboard3 >>> sq) & 1) << 3));
                if((oldPiece & 7) != 0) {
                int index = featureIndex(sq, oldPiece, cKing, player) << 8;
                for(int i = 0; i < 256; i ++)
                    cAcc[offset + i] -= weights[index + i];
                }
                if((newPiece & 7) != 0) {
                    int index = featureIndex(sq, newPiece, cKing, player) << 8;
                    for(int i = 0; i < 256; i ++)
                        cAcc[offset + i] += weights[index + i];
                }
            }
        }
    }

    private static final byte[] MAGIC = "S6CNU002".getBytes(StandardCharsets.US_ASCII);
    private static final int HEADER_SIZE = 72;
    private static final int PAYLOAD_SIZE = 3_940_880;
    private static final int FILE_SIZE = HEADER_SIZE + PAYLOAD_SIZE;

    private static final int[] BUCKET = {
        0, 1, 2, 3,
        4, 4, 5, 5,
        6, 6, 6, 6,
        7, 7, 7, 7,
        8, 8, 8, 8,
        8, 8, 8, 8,
        9, 9, 9, 9,
        9, 9, 9, 9
    };

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

    private final short[] weights = new short[7680 * 256];
    private final short[] biases = new short[256];
    private final int[] outputWeights = new int[8 * 512];
    private final short[] outputBiases = new short[8];

    private final int[] acc = new int[512];

    private static int featureIndex(int square, int piece, int kingSquare, int player) {
        int flip = player * 56;
        int king = kingSquare ^ flip;
        int mirror = -((king >>> 2) & 1) & 7;
        int bucket = BUCKET[((king & 56) >>> 1) | ((king ^ mirror) & 3)];
        int channel = 6 - (piece & 7) + 6 * (((piece >>> 3) ^ player) & 1);
        return bucket * 768 + (channel << 6) + (square ^ flip ^ mirror);
    }

    int score(long board0, long board1, long board2, long board3, int status, int[] acc) {
        int player = status & 1;
        int us = player << 8;
        int them = us ^ (0b100000000);
        int pieces = Long.bitCount(board0 | board1 | board2);
        int head = (pieces - 2) >>> 2;
        int offset = head << 9;
        long sum = 0;
        for(int i = 0; i < 256; i ++) {
            int a = Math.min(255, Math.max(0, acc[us + i]));
            int b = Math.min(255, Math.max(0, acc[them + i]));
            sum += (long) a * a * outputWeights[offset + i] + (long) b * b * outputWeights[offset + 256 + i];
        }
        long q = sum / 255;
        long scaled = ((q + outputBiases[head]) * 400) / (255 * 64);
        return (int) Math.max(-32511, Math.min(32511, scaled));
    }


}

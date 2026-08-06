package dbrighthd.wildfiregendermodplugin.networking.minecraft;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * A {@link DataInputStream} designed for the Minecraft network protocol specification
 * <br />
 * Many methods are shamelessly taken from <a href="https://minecraft.wiki/w/Java_Edition_protocol/Packets">Minecraft.Wiki</a>
 *
 * @author winnpixie
 */
public class CraftInputStream extends DataInputStream {
    /**
     * Create an instance of this stream that reads from an underlying {@link ByteArrayInputStream} from a provided byte-array
     *
     * @param bytes the code byte-array to read from.
     * @return An instance of this stream whose parent is a {@link ByteArrayInputStream}.
     */
    public static CraftInputStream ofBytes(byte[] bytes) {
        return new CraftInputStream(new ByteArrayInputStream(bytes));
    }

    public CraftInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads a variable amount of bytes and returns a {@code int} value.
     *
     * @return the {@code int} value read.
     * @throws IOException the stream has been closed and the contained
     *                     input stream does not support reading after close, or
     *                     another I/O error occurs.
     */
    public int readVarInt() throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = super.readByte();

            // An int has only four usable bits left in its fifth VarInt byte.
            // Reject overflow bits instead of silently truncating them during
            // the left shift.
            if (position == 28 && (currentByte & 0xF0) != 0)
                throw new IOException("VarInt is too big");

            value |= (currentByte & CraftDataConstants.SEGMENT_BITS) << position;

            if ((currentByte & CraftDataConstants.CONTINUE_BIT) == 0) break;

            position += 7;
            if (position >= 32) throw new IOException("VarInt is too big");
        }

        return value;
    }

    /**
     * Reads a variable amount of bytes and returns a {@code long} value.
     *
     * @return the {@code long} value read.
     * @throws IOException the stream has been closed and the contained
     *                     input stream does not support reading after close, or
     *                     another I/O error occurs.
     */
    public long readVarLong() throws IOException {
        long value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            currentByte = super.readByte();

            // A long has only one usable bit left in its tenth VarLong byte.
            if (position == 63 && (currentByte & 0xFE) != 0)
                throw new IOException("VarLong is too big");

            value |= (long) (currentByte & CraftDataConstants.SEGMENT_BITS) << position;

            if ((currentByte & CraftDataConstants.CONTINUE_BIT) == 0) break;

            position += 7;
            if (position >= 64) throw new IOException("VarLong is too big");
        }

        return value;
    }

    /**
     * Reads sixteen input bytes and returns a {@link UUID} value.
     *
     * @return the {@link UUID} value read.
     * @throws IOException the stream has been closed and the contained
     *                     input stream does not support reading after close, or
     *                     another I/O error occurs.
     */
    public UUID readUUID() throws IOException {
        return new UUID(super.readLong(), super.readLong());
    }

    /**
     * Reads a variable amount of bytes and returns a {@link Enum} value.
     *
     * @return the {@link Enum} value read.
     * @throws IOException the stream has been closed and the contained
     *                     input stream does not support reading after close, or
     *                     another I/O error occurs.
     */
    public <T extends Enum<T>> T readEnum(Class<T> enumClass) throws IOException {
        T[] constants = enumClass.getEnumConstants();
        int ordinal = readVarInt();
        if (ordinal < 0 || ordinal >= constants.length)
            throw new IOException("Enum ordinal " + ordinal + " out of range for " + enumClass.getSimpleName()
                    + " (max " + (constants.length - 1) + ")");
        return constants[ordinal];
    }
}

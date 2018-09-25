package ch.epfl.gameboj.component.memory;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Rom {

    private final byte[] memory;

    /**
     * Creates a new rom holding a copy the given array of bytes
     * 
     * @param memory
     *            the byte array that will be copied in the rom (must not be
     *            null)
     * @throws NullPointerException
     *             if the given array is null
     */
    public Rom(byte[] memory) {
        memory = Objects.requireNonNull(memory);
        this.memory = Arrays.copyOf(memory, memory.length);
    }

    /**
     * Returns the length of the memory array
     * 
     * @return memory.length
     */
    public int size() {
        return memory.length;
    }

    /**
     * Returns the value of the byte located at position index in the memory
     * array
     * 
     * @param index
     *            the index that will be used (must be between 0 (inclusive) and
     *            the size of the memory array (exclusive))
     * @throws IndexOutOfBoundsException
     *             if the index is not greater or equal than 0 and smaller than
     *             the size of the memory array
     * @return the integer value of the byte located at position index in the
     *         memory array
     */
    public int read(int index) {
        Objects.checkIndex(index, memory.length);
        return Byte.toUnsignedInt(memory[index]);
    }
}

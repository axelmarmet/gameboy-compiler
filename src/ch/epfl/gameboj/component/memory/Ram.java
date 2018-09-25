package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Ram {

    private byte[] memory;

    /**
     * Creates a new ram with an array of byte of the given size as memory
     * 
     * @param size
     *            the size of the byte array that will be created (must be
     *            greater or equal than 0)
     * @throws IllegalArgumentException
     *             if the given size is not greater or equal than 0
     */
    public Ram(int size) {
        Preconditions.checkArgument(size >= 0);
        memory = new byte[size];
    }

    /**
     * Returns the size of the memory array
     * 
     * @return the size of the memory array
     */
    public int size() {
        return memory.length;
    }

    /**
     * Returns the integer value located at position index in the memory array
     * 
     * @param index
     *            the index used (must be between 0 (inclusive) and the length
     *            of the memory array (exclusive))
     * @throws IndexOutOfBoundsException
     *             if the index is not greater or equal than 0 and smaller than
     *             the size of the memory array
     * @return the integer value located at position index in the memory array
     */
    public int read(int index) {
        Objects.checkIndex(index, memory.length);
        return Byte.toUnsignedInt(memory[index]);
    }

    /**
     * Writes the given value at the given index in the memory array
     * 
     * @param index
     *            the index used (must be between 0 (inclusive) and the length
     *            of the memory array (exclusive))
     * @param value
     *            the value used (must fit in a byte)
     * @throws IndexOutOfBoundsException
     *             if the given index is not greater or equal than 0 and smaller
     *             than the size of the memory array
     * @throws IllegalArgumentException
     *             if the given value does not fit in a byte
     */
    public void write(int index, int value) {
        Objects.checkIndex(index, memory.length);
        Preconditions.checkBits8(value);
        memory[index] = (byte) value;
    }
}

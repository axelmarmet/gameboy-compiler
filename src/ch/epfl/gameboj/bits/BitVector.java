package ch.epfl.gameboj.bits;

import java.util.Arrays;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.lcd.LcdController;;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class BitVector {

    // Used to reduce the memory footprint of initialising more than one empty
    // bitvector of length LCD_WIDTH in the program
    public static final BitVector ZERO_BITVECTOR_LCD_WIDTH = new BitVector(
            LcdController.LCD_WIDTH, false);

    private final int[] chunks;
    // even though it can be obtained by multiplying chunks.length by 32 this
    // variable is stored because time efficiency was deemed more important than
    // memory efficiency
    private final int bitSize;

    private enum Extension {
        WRAPPED, ZEROES
    }

    /**
     * Creates a new BitVector of the given size initialised to the given value
     * 
     * @param size
     *            the size in bits of the new BitVector (must be strictly
     *            positive and a multiple of 32)
     * @param initialValue
     *            the initial value the bits will be initialised to. True for 1
     *            and false for 0
     * @throws IllegalArgumentException
     *             if the given size is not strictly positive or not a multiple
     *             of 32
     */
    public BitVector(int size, boolean initialValue) {
        bitSize = Preconditions.checkPositiveAndDivisibleBy32(size);
        chunks = new int[size / Integer.SIZE];
        if (initialValue)
            Arrays.fill(chunks, -1);
    }

    /**
     * Creates a new BitVector of the given size initialised to 0
     * 
     * @param size
     *            the size in bits of the new BitVector (must be strictly
     *            positive and a multiple of 32)
     * @throws IllegalArgumentException
     *             if the given size is not strictly positive or not a multiple
     *             of 32
     */
    public BitVector(int size) {
        this(size, false);
    }

    private BitVector(int[] chunks) {
        this.chunks = Objects.requireNonNull(chunks);
        bitSize = chunks.length * Integer.SIZE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = chunks.length - 1; i >= 0; --i) {
            char[] filler = new char[Integer.numberOfLeadingZeros(chunks[i])];
            Arrays.fill(filler, '0');
            sb.append(filler);
            if (chunks[i] != 0)
                sb.append(Integer.toBinaryString(chunks[i]));
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof BitVector) {
            return Arrays.equals(chunks, ((BitVector) that).chunks);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(chunks);
    }

    /**
     * Returns the size in bits of BitVector
     * 
     * @return the size in bits of BitVector
     */
    public int size() {
        return bitSize;
    }

    /**
     * Returns whether the bit at the given index is one
     * 
     * @param index
     *            the index of the bit to be tested (must be between 0
     *            (inclusive) and the size of the BitVector (exclusive))
     * @throws IndexOutOfBoundsException
     *             if the given index is out of bounds
     * @return whether the bit at the given index is one
     */
    public boolean testBit(int index) {
        Objects.checkIndex(index, bitSize);
        return Bits.test(chunks[index / Integer.SIZE], index % Integer.SIZE);
    }

    /**
     * Returns a new BitVector containing the complement of this
     * 
     * @return a new BitVector containing the complement of this
     */
    public BitVector not() {
        int[] newChunks = new int[chunks.length];
        for (int i = 0; i < chunks.length; ++i) {
            newChunks[i] = ~chunks[i];
        }
        return new BitVector(newChunks);
    }

    /**
     * Returns a new BitVector containing the logical and of the given BitVector
     * and this BitVector
     * 
     * @param other
     *            the second BitVector that will be used in the logical and
     *            (must not be null and must must be of equal size to this
     *            BitVector)
     * @throws IllegalArgumentException
     *             if the second BitVector is null or not of the same size as
     *             this BitVector
     * @return a new BitVector containing the logical and of the given BitVector
     *         and this BitVector
     */
    public BitVector and(BitVector other) {
        Preconditions.checkArgument(other.bitSize == bitSize);
        int[] newChunks = new int[chunks.length];
        for (int i = 0; i < chunks.length; ++i) {
            newChunks[i] = chunks[i] & other.chunks[i];
        }
        return new BitVector(newChunks);
    }

    /**
     * Returns a new BitVector containing the logical or of the given BitVector
     * and this BitVector
     * 
     * @param other
     *            the second BitVector that will be used in the logical or (must
     *            not be null and must must be of equal size to this BitVector)
     * @throws IllegalArgumentException
     *             if the second BitVector is null or not of the same size as
     *             this BitVector
     * @return a new BitVector containing the logical or of the given BitVector
     *         and this BitVector
     */
    public BitVector or(BitVector other) {
        Preconditions.checkArgument(other.bitSize == bitSize);
        int[] newChunks = new int[chunks.length];
        for (int i = 0; i < chunks.length; ++i) {
            newChunks[i] = chunks[i] | other.chunks[i];
        }
        return new BitVector(newChunks);
    }

    /**
     * Returns a new BitVector of the given size containing the bits of this
     * zero extended BitVector from start (inclusive) to start + size
     * (exclusive)
     * 
     * @param start
     *            the index of the first bit to extract
     * @param size
     *            the amount of bits to extract (must be divisible by 32 and
     *            strictly bigger than zero)
     * @throws IllegalArgumentException
     *             if size is not divisible by 32 or not strictly bigger than
     *             zero
     * @return a new BitVector of the given size containing the bits of this
     *         zero extended BitVector from start (inclusive) to start + size
     *         (exclusive)
     */
    public BitVector extractZeroExtended(int start, int size) {
        return extract(start, size, Extension.ZEROES);
    }

    /**
     * Returns a new BitVector of the given size containing the bits of this
     * wrapped BitVector from start (inclusive) to start + size (exclusive)
     * 
     * @param start
     *            the index of the first bit to extract
     * @param size
     *            the amount of bits to extract (must be divisible by 32 and
     *            strictly bigger than zero)
     * @throws IllegalArgumentException
     *             if size is not divisible by 32 or not strictly bigger than
     *             zero
     * @return a new BitVector of the given size containing the bits of this
     *         wrapped BitVector from start (inclusive) to start + size
     *         (exclusive)
     */
    public BitVector extractWrapped(int start, int size) {
        return extract(start, size, Extension.WRAPPED);
    }

    /**
     * Returns a new BitVector containing this BitVector shifted by the given
     * distance
     * 
     * @param distance
     *            the distance used in the shift, if the distance is negative
     *            then the BitVector is shifted to the right, if it is positive
     *            it is shifted to the left
     * @return a new BitVector containing this BitVector shifted by the given
     *         distance
     */
    public BitVector shift(int distance) {
        return extract(-distance, bitSize, Extension.ZEROES);
    }

    private BitVector extract(int start, int size, Extension e) {
        Preconditions.checkPositiveAndDivisibleBy32(size);
        int[] extractedChunks = new int[size / Integer.SIZE];
        if (start % 32 == 0) {
            for (int i = 0; i < extractedChunks.length; ++i) {
                extractedChunks[i] = getChunk(i + start / 32, e);
            }
        } else {
            for (int i = 0; i < extractedChunks.length; ++i) {
                int flooredQuotient = Math.floorDiv(start, 32);
                int flooredRemainder = Math.floorMod(start, 32);
                int firstPart = getChunk(i + flooredQuotient,
                        e) >>> flooredRemainder;
                int secondPart = getChunk(i + 1 + flooredQuotient,
                        e) << Integer.SIZE - flooredRemainder;
                extractedChunks[i] = firstPart | secondPart;
            }
        }
        return new BitVector(extractedChunks);
    }

    private int getChunk(int index, Extension e) {
        if (isInBounds(index)) {
            return chunks[index];
        } else {
            return e == Extension.ZEROES ? 0
                    : chunks[Math.floorMod(index, chunks.length)];
        }
    }

    private boolean isInBounds(int index) {
        return 0 <= index && index < chunks.length;
    }

    static public final class Builder {

        private static final int[] MASKS = { 0xFF_FF_FF_00, 0xFF_FF_00_FF,
                0xFF_00_FF_FF, 0x00_FF_FF_FF };

        private int[] chunks;

        /**
         * Creates a builder for a BitVector of the given size
         * 
         * @param size
         *            the size of the BitVector built (must be strictly positive
         *            and divisible by 32)
         * @throws IllegalArgumentException
         *             if the size is not strictly positive or divisible by 32
         */
        public Builder(int size) {
            Preconditions.checkPositiveAndDivisibleBy32(size);
            chunks = new int[size / Integer.SIZE];
        }

        /**
         * Sets the byte at the given index to the given value and returns this
         * builder
         * 
         * @param value
         *            the new value of the byte (must fit in 8 bits)
         * @param index
         *            the index of the byte to change (must be between 0
         *            (inclusive) and the size of the BitVector / 4 (exclusive))
         * @throws IllegalStateException
         *             if a BitVector has already been created with this builder
         * @throws IllegalArgumentException
         *             if the given value does not fit in 8 bits
         * @throws IndexOutOfBoundsException
         *             if the given index is not between 0 (inclusive) and the
         *             size of the BitVector / 4 (exclusive)
         * @return this builder after the byte has been set
         */
        public Builder setByte(int value, int index) {
            checkState();
            Preconditions.checkBits8(value);
            Objects.checkIndex(index, chunks.length * Integer.BYTES);
            int quotient = index / Integer.BYTES;
            int remainder = index % Integer.BYTES;
            chunks[quotient] = (chunks[quotient] & MASKS[remainder])
                    | (value << (8 * remainder));
            return this;
        }

        /**
         * Returns a new BitVector built from this Builder. Once this functions
         * has been called this builder cannot be used anymore
         * 
         * @throws IllegalStateException
         *             if a BitVector has already been created with this builder
         * @return a new BitVector built from this Builder
         */
        public BitVector build() {
            checkState();
            BitVector bv = new BitVector(chunks);
            chunks = null;
            return bv;
        }

        private void checkState() {
            if (chunks == null)
                throw new IllegalStateException(
                        "A BitVector was already built with this builder");
        }
    }
}

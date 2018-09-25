package ch.epfl.gameboj.bits;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Bits {

    static int[] tableOfReverses = { 0x00, 0x80, 0x40, 0xC0, 0x20, 0xA0, 0x60,
            0xE0, 0x10, 0x90, 0x50, 0xD0, 0x30, 0xB0, 0x70, 0xF0, 0x08, 0x88,
            0x48, 0xC8, 0x28, 0xA8, 0x68, 0xE8, 0x18, 0x98, 0x58, 0xD8, 0x38,
            0xB8, 0x78, 0xF8, 0x04, 0x84, 0x44, 0xC4, 0x24, 0xA4, 0x64, 0xE4,
            0x14, 0x94, 0x54, 0xD4, 0x34, 0xB4, 0x74, 0xF4, 0x0C, 0x8C, 0x4C,
            0xCC, 0x2C, 0xAC, 0x6C, 0xEC, 0x1C, 0x9C, 0x5C, 0xDC, 0x3C, 0xBC,
            0x7C, 0xFC, 0x02, 0x82, 0x42, 0xC2, 0x22, 0xA2, 0x62, 0xE2, 0x12,
            0x92, 0x52, 0xD2, 0x32, 0xB2, 0x72, 0xF2, 0x0A, 0x8A, 0x4A, 0xCA,
            0x2A, 0xAA, 0x6A, 0xEA, 0x1A, 0x9A, 0x5A, 0xDA, 0x3A, 0xBA, 0x7A,
            0xFA, 0x06, 0x86, 0x46, 0xC6, 0x26, 0xA6, 0x66, 0xE6, 0x16, 0x96,
            0x56, 0xD6, 0x36, 0xB6, 0x76, 0xF6, 0x0E, 0x8E, 0x4E, 0xCE, 0x2E,
            0xAE, 0x6E, 0xEE, 0x1E, 0x9E, 0x5E, 0xDE, 0x3E, 0xBE, 0x7E, 0xFE,
            0x01, 0x81, 0x41, 0xC1, 0x21, 0xA1, 0x61, 0xE1, 0x11, 0x91, 0x51,
            0xD1, 0x31, 0xB1, 0x71, 0xF1, 0x09, 0x89, 0x49, 0xC9, 0x29, 0xA9,
            0x69, 0xE9, 0x19, 0x99, 0x59, 0xD9, 0x39, 0xB9, 0x79, 0xF9, 0x05,
            0x85, 0x45, 0xC5, 0x25, 0xA5, 0x65, 0xE5, 0x15, 0x95, 0x55, 0xD5,
            0x35, 0xB5, 0x75, 0xF5, 0x0D, 0x8D, 0x4D, 0xCD, 0x2D, 0xAD, 0x6D,
            0xED, 0x1D, 0x9D, 0x5D, 0xDD, 0x3D, 0xBD, 0x7D, 0xFD, 0x03, 0x83,
            0x43, 0xC3, 0x23, 0xA3, 0x63, 0xE3, 0x13, 0x93, 0x53, 0xD3, 0x33,
            0xB3, 0x73, 0xF3, 0x0B, 0x8B, 0x4B, 0xCB, 0x2B, 0xAB, 0x6B, 0xEB,
            0x1B, 0x9B, 0x5B, 0xDB, 0x3B, 0xBB, 0x7B, 0xFB, 0x07, 0x87, 0x47,
            0xC7, 0x27, 0xA7, 0x67, 0xE7, 0x17, 0x97, 0x57, 0xD7, 0x37, 0xB7,
            0x77, 0xF7, 0x0F, 0x8F, 0x4F, 0xCF, 0x2F, 0xAF, 0x6F, 0xEF, 0x1F,
            0x9F, 0x5F, 0xDF, 0x3F, 0xBF, 0x7F, 0xFF, };

    private Bits() {
    }

    /**
     * Returns an integer containing exactly one positive bit at the index given
     * 
     * @param index
     *            the index (must be between 0 (inclusive) and 32 (exclusive))
     * @throws IllegalArgumentException
     *             if index is not between 0 (inclusive) and 32 (exclusive)
     * @return an integer containing exactly one positive bit at the index given
     */
    public static int mask(int index) {
        Objects.checkIndex(index, Integer.SIZE);
        return 1 << index;
    }

    /**
     * Returns whether the bit at the given index of the given value is positive
     * 
     * @param value
     *            the integer on which the test will be done
     * @param index
     *            the index of the bit to test (must be between 0 (inclusive)
     *            and 32 (exclusive))
     * @throws IndexOutOfBounds
     *             if index is not between 0 (inclusive) and 32 (exclusive)
     * @return whether the bit at the given index is positive
     */
    public static boolean test(int value, int index) {
        return ((mask(index) & value) != 0);
    }

    /**
     * Returns whether the bit at the index given is positive
     * 
     * @param value
     *            the bits on which the test will be done
     * @param bit
     *            an implementation of the interface Bit to specify which bit
     *            must be tested (must not be null)
     * @throws NullPointerException
     *             if bit is null
     * @return whether the bit at the index given is positive
     */
    public static boolean test(int value, Bit bit) {
        return test(value, bit.index());
    }

    /**
     * Sets the bit in the given value at indexPosition to 0 if newValue is
     * false and 1 if newValue is true
     * 
     * @param value
     *            the integer on which the operation will be done
     * @param index
     * @param newValue
     * @requirements index must be between 0 (inclusive) and 32 (exclusive)
     * @throws IllegalArgumentException
     *             - if index is not in bounds
     * @return the integer bits where the bit at position index has value 0 if
     *         newValue is false and 1 if newValue is true
     */
    public static int set(int value, int index, boolean newValue) {
        return newValue ? mask(index) | value : ~(mask(index)) & value;
    }

    /**
     * Returns the size amounts of least significant bits of the given value
     * 
     * @param size
     *            the amount of least significant bits to return (must be
     *            between 0 (inclusive) and 32 (inclusive))
     * @param value
     *            the integer that will be clipped
     * @throws IllegalArgumentException
     *             if size is not between 0 (inclusive) and 32 (inclusive)
     * @return the size amount of least significant bits of the given value
     */
    public static int clip(int size, int value) {
        Preconditions.checkArgument(size >= 0 && size <= 32);
        return size == Integer.SIZE ? value : value & ((1 << size) - 1);
    }

    /**
     * Returns an integer whose size least significant bits are equal to the
     * bits ranging from start (included) to start + size (excluded)
     * 
     * @param value
     *            the value from which the bits will be extracted
     * @param start
     *            the index of the first bit to be extracted (must designate a
     *            valid range when combined with the param size)
     * @param size
     *            the amount of bits that will be extracted (must designate a
     *            valid range when combined with the param start)
     * @throws IndexOutOfBoundsException
     *             if start to start + size is not a valid range
     * @return an integer whose size least significant bits are equal to the
     *         bits ranging from start (included) to start + size (excluded)
     */
    public static int extract(int value, int start, int size) {
        Objects.checkFromIndexSize(start, size, Integer.SIZE);
        return clip(size, value >> start);
    }

    /**
     * Returns the size amount of least significant bits to which a rotation of
     * distance d has been applied (the rotation is to the left if distance is
     * positive and to the right if it is negative)
     * 
     * @param size
     *            the amount of bits to rotate and return (must be between (0)
     *            exclusive and (32) inclusive)
     * @param value
     *            the integer on which to apply the rotation (must fit in size
     *            bits)
     * @param distance
     *            the distance rotation the bits will do
     * @throws IllegalArgumentException
     *             if the size is not between (0) exclusive and (32) inclusive
     *             or if the given value does not fit in size bits
     * @return the size amount of least significant bits to which a rotation of
     *         distance d has been applied
     */
    public static int rotate(int size, int value, int distance) {
        Preconditions.checkArgument(size > 0 && size <= Integer.SIZE);
        Preconditions
                .checkArgument(extract(value, size, Integer.SIZE - size) == 0);
        int updatedDistance = Math.floorMod(distance, size);
        return clip(size, value << updatedDistance)
                | (value >>> (size - updatedDistance));
    }

    /**
     * Returns the value given where the 7th bit has been copied in all bits
     * with index 8 to 31
     * 
     * @param value
     *            the value on which the operation will be done (must fit in 8
     *            bits)
     * @throws IllegalArgumentException
     *             if the given value does not fit in 8 bits
     * @return the value given where the 7th bit has been copied in all bits
     *         with index 8 to 31
     */
    public static int signExtend8(int value) {
        Preconditions.checkBits8(value);
        byte temp = (byte) value;
        return (int) temp;
    }

    /**
     * Returns an integer where the 8 least significant bits have been reversed
     * 
     * @param value
     * @requirements value the integer on which the operation will be done (must
     *               fit in 8 bits)
     * @throws IllegalArgumentException
     *             if the given value does not fit in 8 bits
     * @return an integer where the 8 least significant bits have been reversed
     */
    public static int reverse8(int value) {
        Preconditions.checkBits8(value);
        return tableOfReverses[value];
    }

    /**
     * Return an integer where the 8 least significant bits of value have been
     * inverted
     * 
     * @param value
     *            the integer on which the operation will be done (must fit in 8
     *            bits)
     * @throws IllegalArgumentException
     *             if the given value does not fit in 8 bits
     * @return an integer where the 8 least significant bits of value have been
     *         inverted
     */
    public static int complement8(int value) {
        Preconditions.checkBits8(value);
        return value ^ 0xFF;
    }

    /**
     * Return a 16 bit value where the 8 most significant bits are the bits from
     * highB and the 8 least significants one from lowB
     * 
     * @param highB
     *            the 8 most significant bits that will be returned (must fit in
     *            8 bits)
     * @param lowB
     *            the 8 least significant bits that will be returned (must fit
     *            in 8 bits)
     * @throws IllegalArgumentException
     *             if highB or lowB do not fit in 8 bits
     * @return a 16 bit value where the 8 most significant bits are the bits
     *         from highB and the 8 least significants one from lowB
     */
    public static int make16(int highB, int lowB) {
        Preconditions.checkBits8(highB | lowB);
        return (highB << 8) | lowB;
    }
}

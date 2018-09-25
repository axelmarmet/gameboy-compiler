package ch.epfl.gameboj.component.cpu;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Alu {

    private Alu() {
    }

    private final static int PADDING_MASK = 0b1111_1111_0000_0000_0000_0000_0000_1111;

    /**
     * The different flags returned by the ALU when it carries out an operation
     * UNUSED_O to UNUSED_3 are there to adjust the index of the 4 flags
     * 
     * @Z The flag Z is true if the result of the operation is equal to zero
     * @N The flag N is true if the operation was a subtraction
     * @H The flag H is true if there was a half carry T
     * @C the flag C is true if there was a full carry
     * 
     * @note When those rules do not make sense paired with an operation they
     *       may be changed so that a flag is always true or false
     */
    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, C, H, N, Z
    }

    /**
     * The two different rotations possible
     */
    public enum RotDir {
        RIGHT, LEFT
    }

    /**
     * Return a binary mask as an integer containing a 1 at the index of each
     * flag iff this flag was true
     * 
     * @param z
     *            the result of the operation is zero flag
     * @param n
     *            a subtraction has been done flag
     * @param h
     *            a carry or borrow has been made when adding or subtracting the
     *            4 least significant bits flag
     * @param c
     *            a carry or borrow has been made when adding or subtracting the
     *            whole 8 bits flag
     * @return a mask containing a 1 at the index of each flag iff this flag was
     *         true
     */
    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {
        int maskZ = z ? 0b1000_0000 : 0;
        int maskN = n ? 0b0100_0000 : 0;
        int maskH = h ? 0b0010_0000 : 0;
        int maskC = c ? 0b0001_0000 : 0;
        return (maskZ | maskN | maskH | maskC);
    }

    /**
     * Return the value contained in the packed integer valueFlags
     * 
     * @param valueFlags
     *            the result of an operation done by the Alu, bits 31 to 24 and
     *            3 to 0 are always 0, bits 23 to 8 contain the result and bits
     *            7 to 4 contain the various flags
     * @return the value contained in the packed integer valueFlags
     */
    public static int unpackValue(int valueFlags) {
        Preconditions.checkArgument((valueFlags & PADDING_MASK) == 0);
        return Bits.extract(valueFlags, 8, 16);
    }

    /**
     * Return the byte containing all the flags that are contained in the packed
     * integer valueFlags
     * 
     * @param valueFlags
     *            the result of an operation done by the Alu, bits 31 to 24 and
     *            3 to 0 are always 0, bits 23 to 8 contain the result and bits
     *            7 to 4 contain the various flags
     * @return the byte containing all the flags that are contained in the
     *         packed integer valueFlags
     */
    public static int unpackFlags(int valueFlags) {
        Preconditions.checkArgument((valueFlags & PADDING_MASK) == 0);
        return Bits.clip(8, valueFlags);
    }

    /**
     * Add the two 8 bits integers first and second together plus 1 if
     * initialCarry is true, return then a packed integer containing the result
     * of this addition and the flags Z0HC
     * 
     * @param firstValue
     *            first integer to add, must fit in 8 bits
     * @param secondValue
     *            second integer to add, must fit in 8 bits
     * @param initialCarry
     *            initial carry, add 1 to the addition if true add nothing if
     *            false
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 8 bits
     * @return a packed integer containing the result of the addition and the
     *         flags Z0HC
     */
    public static int add(int firstValue, int secondValue,
            boolean initialCarry) {
        Preconditions.checkBits8(firstValue | secondValue);
        int temp = firstValue + secondValue + (initialCarry ? 1 : 0);
        boolean halfCarryFlag = Bits.clip(4, firstValue)
                + Bits.clip(4, secondValue) + (initialCarry ? 1 : 0) > 0xF;
        boolean fullCarryFlag = temp > 0xFF;
        int value = Bits.clip(8, temp);
        return packValueZNHC(value, value == 0, false, halfCarryFlag,
                fullCarryFlag);
    }

    /**
     * Add the two 8 bits integers first and second together, return then a
     * packed integer containing the result of this addition and the flags Z0HC
     * 
     * @param firstValue
     *            first integer to add, must fit in 8 bits
     * @param secondValue
     *            second integer to add, must fit in 8 bits
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 8 bits
     * @return a packed integer containing the result of the addition and the
     *         flags Z0HC
     */
    public static int add(int firstValue, int secondValue) {
        return add(firstValue, secondValue, false);
    }

    /**
     * Add the two 16 bits integers first and second together, return then a
     * packed integer containing the result of this addition and the flags 00HC
     * corresponding to the addition of the 8 most significant bits
     * 
     * @param firstValue
     *            first integer to add, must fit in 16 bits
     * @param secondValue
     *            second integer to add, must fit in 16 bits
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 16 bits
     * @return a packed integer containing the result of the addition and the
     *         flags 00HC corresponding to the addition of the 8 most
     *         significant bits
     */
    public static int add16H(int firstValue, int secondValue) {
        Preconditions.checkBits16(firstValue | secondValue);
        int temp = firstValue + secondValue;
        // If the 13th bit of the result is different than the bit created by
        // the
        // addition modulo 2 (xor) of the first and second value then there must
        // have been a carry. This is faster than clipping and adding again
        // because the bitwise operators are faster. This is also used in
        // add16HL but not commented to lighten the code
        boolean halfCarryFlag = ((firstValue ^ secondValue ^ temp)
                & 0b1_0000_0000_0000) != 0;
        boolean fullCarryFlag = temp > 0xFFFF;
        int result = Bits.clip(16, temp);
        return packValueZNHC(result, false, false, halfCarryFlag,
                fullCarryFlag);
    }

    /**
     * Add the two 16 bits integers first and second together, return then a
     * packed integer containing the result of this addition and the flags 00HC
     * corresponding to the addition of the 8 least significant bits
     * 
     * @param firstValue
     *            first integer to add, must fit in 16 bits
     * @param secondValue
     *            second integer to add, must fit in 16 bits
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 16 bits
     * @return a packed integer containing the result of the addition and the
     *         flags 00HC corresponding to the addition of the 8 most
     *         significant bits
     */
    public static int add16L(int firstValue, int secondValue) {
        Preconditions.checkBits16(firstValue | secondValue);
        int result = Bits.clip(16, firstValue + secondValue);
        boolean halfCarryFlag = ((firstValue ^ secondValue ^ result)
                & 0b1_0000) != 0;
        boolean fullCarryFlag = ((firstValue ^ secondValue ^ result)
                & 0b1_0000_0000) != 0;
        return packValueZNHC(result, false, false, halfCarryFlag,
                fullCarryFlag);
    }

    /**
     * Sub the second 8 bits integer from the first and subtract by 1 if
     * initialBorrow is true, return then a packed integer containing the result
     * of this subtraction and the flags Z1HC
     * 
     * @param firstValue
     *            first integer to add, must fit in 8 bits
     * @param secondValue
     *            second integer to add, must fit in 8 bits
     * @param initialBorrow
     *            initial borrow, subtract 1 if true do nothing if false
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 8 bits
     * @return a packed integer containing the result of the subtraction and the
     *         flags Z1HC
     */
    public static int sub(int firstValue, int secondValue,
            boolean initialBorrow) {
        Preconditions.checkBits8(firstValue | secondValue);
        int temp = firstValue - (secondValue + (initialBorrow ? 1 : 0));
        boolean halfBorrowFlag = Bits.clip(4,
                firstValue) < Bits.clip(4, secondValue)
                        + (initialBorrow ? 1 : 0);
        boolean fullBorrowFlag = temp < 0;
        int value = Bits.clip(8, temp);
        return packValueZNHC(value, value == 0, true, halfBorrowFlag,
                fullBorrowFlag);
    }

    /**
     * Sub the second 8 bits integer from the first, return then a packed
     * integer containing the result of this subtraction and the flags
     * associated
     * 
     * @param firstValue
     *            first integer to add, must fit in 8 bits
     * @param secondValue
     *            second integer to add, must fit in 8 bits
     * @throws IllegalArgumentException
     *             if the first or second integer do not fit in 8 bits
     * @return a packed integer containing the result of the subtraction and the
     *         flags associated
     */
    public static int sub(int firstValue, int secondValue) {
        return sub(firstValue, secondValue, false);
    }

    /**
     * Convert the result of an addition or subtraction made using BCD so that
     * the result is also in BCD return the flags ZN0C
     * 
     * @param value
     *            must fit in 8 bits
     * @param isSubtractionFlag
     * @param halfCarryFlag
     * @param fullCarryFlag
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @return the BCD representation of the result of an addition or
     *         subtraction done on two numbers represented in BCD
     */
    public static int bcdAdjust(int value, boolean isSubtractionFlag,
            boolean halfCarryFlag, boolean fullCarryFlag) {
        Preconditions.checkBits8(value);
        boolean fixL = halfCarryFlag
                || (!isSubtractionFlag && Bits.clip(4, value) > 9);
        boolean fixH = fullCarryFlag || (!isSubtractionFlag && value > 0x99);
        int fix = (fixH ? 0x60 : 0) + (fixL ? 6 : 0);
        int adjustedValue = Bits.clip(8,
                value + (isSubtractionFlag ? -fix : fix));
        return packValueZNHC(adjustedValue, adjustedValue == 0,
                isSubtractionFlag, false, fixH);
    }

    /**
     * Return the logical and of the two values and the flags Z010
     * 
     * @param firstValue
     *            must fit in 8 bits
     * @param secondValue
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if firstValue or secondValue do not fit in 8 bits
     * @return the logical and of the two values and the flags Z010
     */
    public static int and(int firstValue, int secondValue) {
        Preconditions.checkBits8(firstValue | secondValue);
        int result = firstValue & secondValue;
        return packValueZNHC(result, result == 0, false, true, false);
    }

    /**
     * Return the logical or of the two values and the flags Z000
     * 
     * @param firstValue
     *            must fit in 8 bits
     * @param secondValue
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if firstValue or secondValue do not fit in 8 bits
     * @return the logical or of the two values and the flags Z000
     */
    public static int or(int firstValue, int secondValue) {
        Preconditions.checkBits8(firstValue | secondValue);
        int result = firstValue | secondValue;
        return packValueZNHC(result, result == 0, false, false, false);
    }

    /**
     * Return the logical xor of the two values and the flags Z000
     * 
     * @param firstValue
     *            must fit in 8 bits
     * @param secondValue
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if firstValue or secondValue do not fit in 8 bits
     * @return the logical xor of the two values and the flags Z000
     */
    public static int xor(int firstValue, int secondValue) {
        Preconditions.checkBits8(firstValue | secondValue);
        int result = firstValue ^ secondValue;
        return packValueZNHC(result, result == 0, false, false, false);
    }

    /**
     * Return the given value shifted by one to the left and the flags Z00C
     * where C is the value of the bit that was ejected
     * 
     * @param value
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @return the given value shifted by one to the left and the flags Z00C
     *         where C is the value of the bit that was ejected
     */
    public static int shiftLeft(int value) {
        Preconditions.checkBits8(value);
        boolean cFlag = Bits.test(value, 7);
        int result = Bits.clip(8, value << 1);
        return packValueZNHC(result, result == 0, false, false, cFlag);
    }

    /**
     * Return the given value arithmetically shifted by one to the right and the
     * flags Z00C where C is the value of the bit that was ejected
     * 
     * @param value
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @return the given value arithmetically shifted by one to the right and
     *         the flags Z00C where C is the value of the bit that was ejected
     */
    public static int shiftRightA(int value) {
        Preconditions.checkBits8(value);
        boolean cFlag = Bits.test(value, 0);
        int result = Bits.clip(8, Bits.signExtend8(value) >> 1);
        return packValueZNHC(result, result == 0, false, false, cFlag);
    }

    /**
     * Return the given value logically shifted by one to the right and the
     * flags Z00C where C is the value of the bit that was ejected
     * 
     * @param value
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @return the given value logically shifted by one to the right and the
     *         flags Z00C where C is the value of the bit that was ejected
     */
    public static int shiftRightL(int value) {
        Preconditions.checkBits8(value);
        boolean cFlag = Bits.test(value, 0);
        int result = Bits.clip(8, value >>> 1);
        return packValueZNHC(result, result == 0, false, false, cFlag);
    }

    /**
     * Return the given value rotated by one bit in the direction specified by
     * rotationDirection and the flags Z00C where C is the value of the bit that
     * changed extremities
     * 
     * @param rotationDirection
     *            must not be null
     * @param value
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @throws NullPointerException
     *             if rotationDirection is null
     * @return the given value rotated by one bit in the direction specified by
     *         rotationDirection and the flags Z00C where C is the value of the
     *         bit that changed extremities
     */
    public static int rotate(RotDir rotationDirection, int value) {
        Objects.requireNonNull(rotationDirection);
        Preconditions.checkBits8(value);
        boolean cFlag;
        if (rotationDirection == RotDir.LEFT) {
            cFlag = Bits.test(value, 7);
            value = Bits.rotate(8, value, 1);
        } else {
            cFlag = Bits.test(value, 0);
            value = Bits.rotate(8, value, -1);
        }
        return packValueZNHC(value, value == 0, false, false, cFlag);
    }

    /**
     * Return the given value rotated through the carry by one bit in the
     * direction specified by rotationDirection and the flags Z00C where C is
     * the value of the bit at index 8 after the rotation
     * 
     * @param rotationDirection
     *            must not be null
     * @param value
     *            must fit in 8 bits
     * @param carry
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @throws NullPointerException
     *             if rotationDirection is null
     * @return the given value rotated through the carry by one bit in the
     *         direction specified by rotationDirection and the flags Z00C where
     *         C is the value of the bit at index 8 after the rotation
     */
    public static int rotate(RotDir rotationDirection, int value,
            boolean carry) {
        Objects.requireNonNull(rotationDirection);
        Preconditions.checkBits8(value);
        value = Bits.set(value, 8, carry);
        value = Bits.rotate(9, value,
                (rotationDirection == RotDir.LEFT ? 1 : -1));
        boolean cFlag = Bits.test(value, 8);
        value = Bits.clip(8, value);
        return packValueZNHC(value, value == 0, false, false, cFlag);
    }

    /**
     * Return the value obtained by swapping the 4 least significant bits with
     * the 4 most significant bits and the flags Z000
     * 
     * @param value
     *            must fit in 8 bits
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @return the value obtained by swapping the 4 least significant bits with
     *         the 4 most significant bits and the flags Z000
     */
    public static int swap(int value) {
        Preconditions.checkBits8(value);
        int lowB = Bits.clip(4, value);
        int newValue = Bits.extract(value, 4, 4) | (lowB << 4);
        return packValueZNHC(newValue, newValue == 0, false, false, false);
    }

    /**
     * Return the value 0 and the flags Z010 where the bit Z is 0 if the bit
     * located at bitIndex in value is 1 and 1 otherwise
     * 
     * @param value
     *            must fit in 8 bits
     * @param bitIndex
     *            must be between 0 (inclusive) and 8 (exclusive)
     * @throws IllegalArgumentException
     *             if value does not fit in 8 bits
     * @throws IndexOutOfBoundsException
     *             if bitIndex is not in bounds
     * @return the value 0 and the flags Z010 where the bit Z is 0 if the bit
     *         located at bitIndex in value is 1 and 1 otherwise
     */
    public static int testBit(int value, int bitIndex) {
        Preconditions.checkBits8(value);
        Objects.checkIndex(bitIndex, 8);
        return packValueZNHC(0, !Bits.test(value, bitIndex), false, true,
                false);
    }

    private static int packValueZNHC(int value, boolean z, boolean n, boolean h,
            boolean c) {
        Preconditions.checkBits16(value);
        return (value << 8 | maskZNHC(z, n, h, c));
    }

}

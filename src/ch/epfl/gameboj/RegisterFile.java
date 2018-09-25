package ch.epfl.gameboj;

import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * @author Axel Marmet (288862)
 *
 * @param <E>
 */
public final class RegisterFile<E extends Register> {

    private byte[] data;

    /**
     * Creates a new 8 bit registers file holding as many register as the length
     * of the given array. The array parameter should always be created by using
     * the function values implement by an Enum.
     * 
     * @param allRegs
     *            the registers that will be created (should always be created
     *            by using the function values implemented by any Enum)
     * @throws NullPointerException
     *             if allRegs is null
     */
    public RegisterFile(E[] allRegs) {
        data = new byte[allRegs.length];
    }

    /**
     * Returns the 8 bit integer associated to the given register
     * 
     * @param register
     *            the register from which to retrieve data (must not be null)
     * @throws NullPointerException
     *             if register is null
     * @return the 8 bit integer associated to the given register
     */
    public int get(E register) {
        return Byte.toUnsignedInt(data[register.index()]);
    }

    /**
     * Sets the 8 bit integer associated to the given register to newValue
     * 
     * @param register
     *            the register to update (must not be null)
     * @param newValue
     *            the value that will be assigned (must fit in 8 bits)
     * @throws NullPointerException
     *             if register is null
     * @throws IllegalArgumentException
     *             if newValue does not fit in 8 bits
     */
    public void set(E register, int newValue) {
        Preconditions.checkBits8(newValue);
        data[register.index()] = (byte) newValue;
    }

    /**
     * Returns true iff the given bit of the register is 1
     * 
     * @param register
     *            the register from which to test the value (must not be null)
     * @param bit
     *            the bit that will be tested (must not be null)
     * @throws NullPointerException
     *             if register or bit is null
     * @return true iff the given bit of the register is 1
     */
    public boolean testBit(E register, Bit bit) {
        return Bits.test(data[register.index()], bit.index());
    }

    /**
     * Sets the given bit of the register to 1 if newValue is true and 0 if it
     * is false
     * 
     * @param register
     *            the register to update (must not be null)
     * @param bit
     *            the bit to set (must not be null)
     * @param newValue
     *            the value that the bit will be set to, true sets the bit to 1
     *            and false to 0
     * @throws NullPointerException
     *             if register or bit is null
     */
    public void setBit(E register, Bit bit, boolean newValue) {
        int value = Byte.toUnsignedInt(data[register.index()]);
        data[register.index()] = (byte) Bits.set(value, bit.index(), newValue);
    }

}

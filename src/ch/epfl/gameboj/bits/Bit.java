package ch.epfl.gameboj.bits;

/**
 * @author Axel Marmet (288862)
 *
 */
public interface Bit {

    /**
     * Returns the ordinal number of the enumeration constant that will
     * implement this interface
     * 
     * @return the ordinal number of the enumeration constant that will
     *         implement this interface
     */
    public int ordinal();

    /**
     * Simply returns the ordinal function. The objective of this function is
     * just to provide a clearer name than the function ordinal
     * 
     * @return the ordinal number of the enumeration constant that will
     *         implement this interface
     */
    default public int index() {
        return ordinal();
    }

    /**
     * Returns a mask where only the bit of index ordinal is one
     * 
     * @return a mask where only the bit of index ordinal is one
     */
    default public int mask() {
        return Bits.mask(ordinal());
    }
}

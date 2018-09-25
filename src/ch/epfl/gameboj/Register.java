package ch.epfl.gameboj;

/**
 * @author Axel Marmet (288862)
 *
 */
public interface Register {

    /**
     * Returns the ordinal number of the enumeration constant that will
     * implement this interface
     * 
     * @return the ordinal number of the enumeration constant that will
     *         implement this interface
     */
    abstract int ordinal();

    /**
     * Simply returns the ordinal function. The objective of this function is
     * just to provide a clearer name than the function ordinal
     * 
     * @return the ordinal number of the enumeration constant that will
     *         implement this interface
     */
    public default int index() {
        return ordinal();
    }
}

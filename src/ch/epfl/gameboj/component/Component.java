package ch.epfl.gameboj.component;

import java.util.Objects;

import ch.epfl.gameboj.Bus;

/**
 * @author Axel Marmet (288862)
 *
 */
public interface Component {

    // The value that must be returned by a component to signify that there is
    // no value to read at this address
    public static final int NO_DATA = 0x100;

    /**
     * Returns the byte stored at the given address or NO_DATA if the component
     * doesn't have a value located at this address
     * 
     * @param address
     *            the address (must fit in 16 bits)
     * @throws IllegalArgumentException
     *             if the address doesn't fit in 16 bits
     * @return the byte stored at the address given or NO_DATA if the component
     *         doesn't have a value located at this address
     */
    int read(int address);

    /**
     * Writes the given value at the given address or does nothing if the
     * component can't write a value at the given address
     * 
     * @param address
     *            the address (must fit in 16 bits)
     * @param value
     *            the value (must fit in 8 bits)
     * @throws IllegalArgumentException
     *             if the address doesn't fit in 16 bits or if the value doesn't
     *             fit in 8 bits
     */
    void write(int address, int value);

    /**
     * Link this component to the given bus
     * 
     * @param bus
     *            the bus to which the component will be attached (must not be
     *            null)
     * @throws NullPointerException
     *             if the object bus is null
     */
    default void attachTo(Bus bus) {
        Objects.requireNonNull(bus);
        bus.attach(this);
    }
}

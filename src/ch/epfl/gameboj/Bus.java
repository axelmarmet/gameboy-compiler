package ch.epfl.gameboj;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.component.Component;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Bus {

    private List<Component> components = new ArrayList<Component>();

    /**
     * Attaches the given component to the bus
     * 
     * @param component
     *            the component that will be attached (must not be null)
     * @throws NullPointerException
     *             if the given component is null
     */
    public void attach(Component component) {
        component = Objects.requireNonNull(component);
        components.add(component);
    }

    /**
     * Return the value stored at the given address if any component has a value
     * at this address, or 0xFF if no component do
     * 
     * @param address
     *            the address (must fit in 16 bits)
     * @throws IllegalArgumentException
     *             if address doesn't fit in 16 bits
     * @return the value stored at the given address if any component has a
     *         value at this address, or 0xFF if no component do
     */
    public int read(int address) {
        Preconditions.checkBits16(address);
        for (Component c : components) {
            int value = c.read(address);
            if (value != Component.NO_DATA) {
                return value;
            }
        }
        return 0xFF;
    }

    /**
     * Tell all the components linked to the bus to write the given value at the
     * given address
     * 
     * @param address
     *            the address (must fit in 16 bits)
     * @param value
     *            the value to write (must fit in 8 bits)
     * @throws IllegalArgumentException
     *             if the given address doesn't fit in 16 bits or the given
     *             value doesn't fit in 8 bits
     */
    public void write(int address, int value) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(value);
        for (Component c : components) {
            c.write(address, value);
        }
    }
}

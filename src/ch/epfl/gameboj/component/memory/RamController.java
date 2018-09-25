package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;

/**
 * @author Axel Marmet (288862)
 *
 */

public final class RamController implements Component {

    Ram ram;
    int startAddress, endAddress;

    /**
     * Creates a new RamController with the given ram and the given start and
     * end addresses
     * 
     * @param ram
     *            the ram that will be used (must not be null)
     * @param startAddress
     *            the first accessible address of the RamController (must fit in
     *            16 bits)
     * @param endAddress
     *            the first address that is not accessible anymore of the
     *            RamController (must fit in 16 bits)
     * @throws NullPointerException
     *             if the given ram is null
     * @throws IllegalArgumentException
     *             if startAddress or endAddress is not contained in 16 bits or
     *             if startAddress and endAddress describe either a negative
     *             range or a range bigger than the size of the given ram
     */
    public RamController(Ram ram, int startAddress, int endAddress) {
        Preconditions.checkArgument(startAddress <= endAddress
                && startAddress + ram.size() >= endAddress);
        this.ram = Objects.requireNonNull(ram);
        this.startAddress = Preconditions.checkBits16(startAddress);
        this.endAddress = Preconditions.checkBits16(endAddress);
    }

    /**
     * Creates a new RamController with the given ram and the given start
     * address, the end address is computed as the start address plus the size
     * of the ram
     * 
     * @param ram
     *            the ram that will be used (must not be null)
     * @param startAddress
     *            the first accessible address of the RamController (must fit in
     *            16 bits)
     * @throws NullPointerException
     *             if the given ram is null
     * @throws IllegalArgumentException
     *             if startAddress is not contained in 16 bits
     */
    public RamController(Ram ram, int startAddress) {
        this(ram, startAddress, startAddress + ram.size());
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (isInBounds(address))
            return ram.read(address - startAddress);
        else
            return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(data);
        if (isInBounds(address))
            ram.write(address - startAddress, data);
    }

    private boolean isInBounds(int address) {
        return (address >= startAddress && address < endAddress);
    }

}

package ch.epfl.gameboj.component.cartridge;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class MBC0 implements Component {

    private final Rom rom;
    private static final int CARTRIDGE_SIZE = 32768;

    /**
     * Creates a memory bank controller of type 0 with the given rom
     * 
     * @param rom
     *            the rom that will be used (must not be null and must be of
     *            size equal to 32 768 bytes)
     * @throws IllegalArgumentException
     *             if the given rom is not of size equal to 32 768 bytes
     * @throws NullPointerException
     *             if the given rom is null
     */
    public MBC0(Rom rom) {
        Preconditions.checkArgument(rom.size() == CARTRIDGE_SIZE);
        this.rom = rom;
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        if (address < CARTRIDGE_SIZE) {
            return rom.read(address);
        }
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) {
        // INTENTIONALLY EMPTY
    }

}

package ch.epfl.gameboj.component.cartridge;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Rom;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class Cartridge implements Component {

    private static final int ADDRESS_OF_CARTRIDGE_TYPE = 0x147;
    private static final int ADDRESS_OF_RAM_SIZE = 0x149;

    private static final int[] ramSizeCorrespondance = { 0, 2048, 8192, 32768 };

    Component memoryBankController;

    private Cartridge(Component memoryBankController) {
        this.memoryBankController = memoryBankController;
    }

    /**
     * Returns a Cartridge having for rom a rom containing the bytes stored in
     * the given romFile
     * 
     * @param romFile
     *            The file containing the bytes that will be used in the rom
     * @return a Cartridge having for rom a rom containing the bytes stored in
     *         the given romFile
     * @throws IOException
     *             if an I/O error occurs or if the named file does not exist,
     *             is a directory or cannot be opened
     */
    public static Cartridge ofFile(File romFile) throws IOException {
        byte[] bytes;
        try (InputStream s = new BufferedInputStream(
                new FileInputStream(romFile))) {
            bytes = s.readAllBytes();
        } catch (FileNotFoundException e) {
            throw new IOException("File not found");
        }
        Preconditions.checkArgument(bytes[ADDRESS_OF_CARTRIDGE_TYPE] < 4
                && bytes[ADDRESS_OF_CARTRIDGE_TYPE] >= 0);

        Rom rom = new Rom(bytes);
        Component memoryBankController;
        if (bytes[ADDRESS_OF_CARTRIDGE_TYPE] == 0) {
            memoryBankController = new MBC0(rom);
        } else {
            int sizeArg = bytes[ADDRESS_OF_RAM_SIZE];
            memoryBankController = new MBC1(rom,
                    ramSizeCorrespondance[sizeArg]);
        }
        return new Cartridge(memoryBankController);
    }

    @Override
    public int read(int address) {
        Preconditions.checkBits16(address);
        return memoryBankController.read(address);
    }

    @Override
    public void write(int address, int value) {
        Preconditions.checkBits16(address);
        Preconditions.checkBits8(value);
        memoryBankController.write(address, value);
    }

}

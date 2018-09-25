package ch.epfl.gameboj.component.memory;

import java.util.Objects;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;

public final class BootRomController implements Component {

	private final Cartridge cartridge;
	boolean isTransparent = false;

	/**
	 * Creates a new BootRomController with the given cartridge attached * @param
	 * cartridge the cartridge that will be attached (must not be null)
	 * 
	 * @throws IllegalArgumentException
	 *             if cartridge is null
	 */
	public BootRomController(Cartridge cartridge) {
		this.cartridge = Objects.requireNonNull(cartridge);
	}

	@Override
	public int read(int address) {
		if (!isTransparent && address >= AddressMap.BOOT_ROM_START && address < AddressMap.BOOT_ROM_END) {
			return Byte.toUnsignedInt(CustomBootRom.DATA[address]);
		} else {
			return cartridge.read(address);
		}
	}

	@Override
	public void write(int address, int data) {
		if (address == AddressMap.REG_BOOT_ROM_DISABLE) {
			isTransparent = true;
		} else {
			cartridge.write(address, data);
		}
	}

}

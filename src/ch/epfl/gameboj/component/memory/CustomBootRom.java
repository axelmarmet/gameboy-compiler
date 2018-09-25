package ch.epfl.gameboj.component.memory;

import ch.epfl.bonus.compiler.CartridgeOrganization;
import ch.epfl.bonus.compiler.Opcode;

public interface CustomBootRom {

	byte[] DATA = new byte[] {
			// init function stack
			(byte) Opcode.LD_SP_N16.encoding, (byte) 0xFE, (byte) 0xFF,
			// init own stack
			(byte) Opcode.LD_HL_N16.encoding, (byte) CartridgeOrganization.STACK_START,
			(byte) (CartridgeOrganization.STACK_START >> 8),
			// jump
			(byte) Opcode.JP_N16.encoding, (byte) 0xFC, (byte) 0x00,
			// untouched
			(byte) 0x6C, (byte) 0x28, (byte) 0xFB, (byte) 0x3E, (byte) 0x80, (byte) 0xE0, (byte) 0x26, (byte) 0xE0,
			(byte) 0x11, (byte) 0x3E, (byte) 0xF3, (byte) 0xE0, (byte) 0x12, (byte) 0xE0, (byte) 0x25, (byte) 0x3E,
			(byte) 0x77, (byte) 0xE0, (byte) 0x24, (byte) 0x3E, (byte) 0xFC, (byte) 0xE0, (byte) 0x47, (byte) 0x11,
			(byte) 0x04, (byte) 0x01, (byte) 0x21, (byte) 0x10, (byte) 0x80, (byte) 0x1A, (byte) 0x47, (byte) 0xCD,
			(byte) 0x82, (byte) 0x00, (byte) 0xCD, (byte) 0x82, (byte) 0x00, (byte) 0x13, (byte) 0x7B, (byte) 0xEE,
			(byte) 0x34, (byte) 0x20, (byte) 0xF2, (byte) 0x11, (byte) 0xB1, (byte) 0x00, (byte) 0x0E, (byte) 0x08,
			(byte) 0x1A, (byte) 0x13, (byte) 0x22, (byte) 0x23, (byte) 0x0D, (byte) 0x20, (byte) 0xF9, (byte) 0x3E,
			(byte) 0x19, (byte) 0xEA, (byte) 0x10, (byte) 0x99, (byte) 0x21, (byte) 0x2F, (byte) 0x99, (byte) 0x0E,
			(byte) 0x0C, (byte) 0x3D, (byte) 0x28, (byte) 0x08, (byte) 0x32, (byte) 0x0D, (byte) 0x20, (byte) 0xF9,
			(byte) 0x2E, (byte) 0x0F, (byte) 0x18, (byte) 0xF5, (byte) 0x3E, (byte) 0x91, (byte) 0xE0, (byte) 0x40,
			(byte) 0x06, (byte) 0x2D, (byte) 0xCD, (byte) 0xA3, (byte) 0x00, (byte) 0x3E, (byte) 0x83, (byte) 0xCD,
			(byte) 0xAA, (byte) 0x00, (byte) 0x06, (byte) 0x05, (byte) 0xCD, (byte) 0xA3, (byte) 0x00, (byte) 0x3E,
			(byte) 0xC1, (byte) 0xCD, (byte) 0xAA, (byte) 0x00, (byte) 0x06, (byte) 0x46, (byte) 0xCD, (byte) 0xA3,
			(byte) 0x00, (byte) 0x21, (byte) 0xB0, (byte) 0x01, (byte) 0xE5, (byte) 0xF1, (byte) 0x21, (byte) 0x4D,
			(byte) 0x01, (byte) 0x01, (byte) 0x13, (byte) 0x00, (byte) 0x11, (byte) 0xD8, (byte) 0x00, (byte) 0xC3,
			(byte) 0xFE, (byte) 0x00, (byte) 0x3E, (byte) 0x04, (byte) 0x0E, (byte) 0x00, (byte) 0xCB, (byte) 0x20,
			(byte) 0xF5, (byte) 0xCB, (byte) 0x11, (byte) 0xF1, (byte) 0xCB, (byte) 0x11, (byte) 0x3D, (byte) 0x20,
			(byte) 0xF5, (byte) 0x79, (byte) 0x22, (byte) 0x23, (byte) 0x22, (byte) 0x23, (byte) 0xC9, (byte) 0xE5,
			(byte) 0x21, (byte) 0x0F, (byte) 0xFF, (byte) 0xCB, (byte) 0x86, (byte) 0xCB, (byte) 0x46, (byte) 0x28,
			(byte) 0xFC, (byte) 0xE1, (byte) 0xC9, (byte) 0xCD, (byte) 0x97, (byte) 0x00, (byte) 0x05, (byte) 0x20,
			(byte) 0xFA, (byte) 0xC9, (byte) 0xE0, (byte) 0x13, (byte) 0x3E, (byte) 0x87, (byte) 0xE0, (byte) 0x14,
			(byte) 0xC9, (byte) 0x3C, (byte) 0x42, (byte) 0xB9, (byte) 0xA5, (byte) 0xB9, (byte) 0xA5, (byte) 0x42,
			(byte) 0x3C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
			(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE0, (byte) 0x50, };

}

package ch.epfl.bonus.compiler;

public interface CartridgeOrganization {
	public static int INTERRUPT_HANDLER_SIZE = 0x100;
	public static int HEADER_END = 0x150;
	public static int ROM_FUN_AREA_START = 0x150;
	public static int ROM_FUN_AREA_END = 0x8000;
	public static int ROM_FUN_AREA_SIZE = ROM_FUN_AREA_END - ROM_FUN_AREA_START;
	public static int STACK_START = 0xC000;
	public static int CARTRIDGE_SIZE = 0x8000;

}

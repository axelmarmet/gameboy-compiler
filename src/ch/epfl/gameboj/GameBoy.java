package ch.epfl.gameboj;

import static ch.epfl.gameboj.AddressMap.ECHO_RAM_END;
import static ch.epfl.gameboj.AddressMap.ECHO_RAM_START;
import static ch.epfl.gameboj.AddressMap.WORK_RAM_END;
import static ch.epfl.gameboj.AddressMap.WORK_RAM_SIZE;
import static ch.epfl.gameboj.AddressMap.WORK_RAM_START;

import java.util.Objects;

import ch.epfl.gameboj.component.SerialPortPrintComponent;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

/**
 * @author Axel Marmet (288862)
 *
 */
public final class GameBoy {

	private final Bus bus;
	private final Cpu cpu;
	private final Timer timer;
	private final SerialPortPrintComponent printer;

	public static final long cyclesPerSecond = 1 << 20;
	public static final double cyclesPerNanoSecond = (double) cyclesPerSecond * 1e-9;

	private long cycles = 0;

	/**
	 * Create a new GameBoy having for cartridge the one given in the parameter
	 * 
	 * @param cartridge
	 *            the cartridge that will be used by the GameBoy (must not be null)
	 * @throws NullPointerException
	 *             if the cartridge is null
	 */
	// Disabled graphics
	public GameBoy(Cartridge cartridge) {
		Objects.requireNonNull(cartridge);
		bus = new Bus();
		cpu = new Cpu();
		timer = new Timer(cpu);
		printer = new SerialPortPrintComponent();

		Ram workRam = new Ram(WORK_RAM_SIZE);
		RamController workRamController = new RamController(workRam, WORK_RAM_START, WORK_RAM_END);
		RamController echoRamController = new RamController(workRam, ECHO_RAM_START, ECHO_RAM_END);
		BootRomController bootRomController = new BootRomController(cartridge);

		workRamController.attachTo(bus);
		echoRamController.attachTo(bus);
		bootRomController.attachTo(bus);
		printer.attachTo(bus);
		cpu.attachTo(bus);
		timer.attachTo(bus);
	}

	/**
	 * Simulates the behaviour of a GameBoy until the given limitCycle - 1
	 * 
	 * @param limitCycle
	 *            the GameBoy will cycle until it reaches this number of simulate
	 *            cycles (must not be smaller than the number of cycles already
	 *            simulated)
	 * 
	 * @throws IllegalArgumentException
	 *             if the limit cycle is smaller than the amount of cycles already
	 *             simulated
	 */
	public void runUntil(long limitCycle) {
		if (limitCycle < cycles) {
			throw new IllegalArgumentException(
					"The limit cycle is smaller than the amount of cycles already simulated");
		}
		while (cycles < limitCycle) {
			timer.cycle(cycles);
			cpu.cycle(cycles);
			cycles++;
		}
	}

	// Added for compiler
	public void runUntilCpuHalted() {
		while (!cpu.isHalted()) {
			timer.cycle(cycles);
			cpu.cycle(cycles);
			cycles++;
		}
	}

	// Added for compiler
	public void runOneCpuInstruction() {
		runUntil(cpu.getNextNonIdleCycle() + 1);
	}

	/**
	 * Returns the timer associated to the GameBoy
	 * 
	 * @return the timer associated to the GameBoy
	 */
	public Timer timer() {
		return timer;
	}

	/**
	 * Returns the bus associated to the GameBoy
	 * 
	 * @return the bus associated to the GameBoy
	 */
	public Bus bus() {
		return bus;
	}

	/**
	 * Returns the cpu associated to the GameBoy
	 * 
	 * @return the cpu associated to the GameBoy
	 */
	public Cpu cpu() {
		return cpu;
	}

	// Added for compiler
	public SerialPortPrintComponent serialPortPrintComponent() {
		return printer;
	}

	/**
	 * Returns the amount of cycles already simulated
	 * 
	 * @return the amount of cycles already simulated
	 */
	public long cycles() {
		return cycles;
	}

}

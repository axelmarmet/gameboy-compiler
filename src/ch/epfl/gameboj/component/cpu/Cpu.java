package ch.epfl.gameboj.component.cpu;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Bus;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.Register;
import ch.epfl.gameboj.RegisterFile;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;
import ch.epfl.gameboj.component.Clocked;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cpu.Alu.Flag;
import ch.epfl.gameboj.component.cpu.Alu.RotDir;
import ch.epfl.gameboj.component.memory.Ram;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * @author Axel Marmet (288862)
 *
 */

public final class Cpu implements Component, Clocked {

	/**
	 * The different interrupts that may be raised
	 */
	public enum Interrupt implements Bit {
		VBLANK, LCD_STAT, TIMER, SERIAL, JOYPAD
	}

	private static final int PREFIX = 0xCB;

	/**
	 * The 8 bits registers used by the CPU
	 */
	private enum Reg implements Register {
		A, F, B, C, D, E, H, L
	}

	/**
	 * Pairs of the 8 bits registers used by the CPU, are used to give a pair of
	 * register in arguments
	 */
	private enum Reg16 {
		AF, BC, DE, HL
	}

	/**
	 * The different possibilities of assignment for a flag
	 * 
	 * @value V0 specifies that the flag should be 0
	 * @value V1 specifies that the flag should be 1
	 * @value ALU specifies that the flag should be equal to the flag produced by
	 *        the ALU
	 * @value CPU specifies that the flag should be equal to the flag already stored
	 *        in Register F
	 */
	private enum FlagSrc {
		V0, V1, ALU, CPU
	}

	private static final Opcode[] DIRECT_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.DIRECT);
	private static final Opcode[] PREFIXED_OPCODE_TABLE = buildOpcodeTable(Opcode.Kind.PREFIXED);

	public ObjectProperty<String> cpuProperty = new SimpleObjectProperty<>("");

	private Bus bus;
	private RegisterFile<Reg> regF = new RegisterFile<>(Reg.values());
	private Ram highRam = new Ram(AddressMap.HIGH_RAM_SIZE);

	private int PCRegister = 0;
	private int SPRegister = 0;
	private boolean IMERegister = false;
	private int IERegister = 0;
	private int IFRegister = 0;

	private boolean isHalted = false;
	private long nextNonIdleCycle = 0;

	// function added for compiler
	public void updateRegisterProperty() {
		StringBuilder sb = new StringBuilder("--------------------\n");
		sb.append("Will execute " + getNextOpcodeInformation() + "\n");
		sb.append(String.format("PC : %-5d \n", PCRegister));
		sb.append(String.format("SP : %-5d \n", SPRegister));
		for (Reg register : Reg.values()) {
			sb.append(String.format(register.name() + " : %-5d \n", regF.get(register)));
		}
		cpuProperty.set(cpuProperty.get() + sb.toString());
	}

	// function added for compiler
	private String getNextOpcodeInformation() {
		StringBuilder sb = new StringBuilder();
		int nextInstruction = read8AtPC();
		Opcode opcode = (nextInstruction == PREFIX ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()]
				: DIRECT_OPCODE_TABLE[nextInstruction]);
		sb.append(opcode.name());
		if (opcode.kind != Opcode.Kind.PREFIXED) {
			if (opcode.totalBytes == 2)
				sb.append(" with argument " + read8AfterOpcode());
			else if (opcode.totalBytes == 3)
				sb.append(" with argument " + read16AfterOpcode());
		}
		return sb.toString();
	}

	// function added for compiler
	public boolean isHalted() {
		return isHalted;
	}

	// function added for compiler
	public long getNextNonIdleCycle() {
		return nextNonIdleCycle;
	}

	/**
	 * Returns an array containing the values, in order, of the registers PC, SP, A,
	 * F, B, C, D, E, H and L
	 * 
	 * @return an array containing the values, in order, of the registers PC, SP, A,
	 *         F, B, C, D, E, H and L
	 */
	public int[] _testGetPcSpAFBCDEHL() {
		Reg[] allRegs = Reg.values();
		int[] table = new int[10];
		table[0] = PCRegister;
		table[1] = SPRegister;
		for (int i = 0; i < allRegs.length; ++i) {
			table[i + 2] = regF.get(allRegs[i]);
		}
		return table;
	}

	@Override
	public void cycle(long cycle) {
		if (isHalted) {
			if ((IERegister & IFRegister) != 0) {
				isHalted = false;
				nextNonIdleCycle = cycle;
			} else {
				return;
			}
		}
		// The two following if statements could be grouped together but it
		// wasn't done as to keep clarity since the conditions are not in the
		// same theme
		if (cycle < nextNonIdleCycle) {
			return;
		}
		if (IMERegister && handleInterruptions()) {
			return;
		}
		int nextInstruction = read8AtPC();
		Opcode opcode = (nextInstruction == PREFIX ? PREFIXED_OPCODE_TABLE[read8AfterOpcode()]
				: DIRECT_OPCODE_TABLE[nextInstruction]);
		dispatch(opcode);
		updateRegisterProperty();
	}

	@Override
	public int read(int address) {
		Preconditions.checkBits16(address);
		switch (address) {
		case AddressMap.REG_IE:
			return IERegister;
		case AddressMap.REG_IF:
			return IFRegister;
		default:
			if (isInHighRamBounds(address)) {
				return highRam.read(address - AddressMap.HIGH_RAM_START);
			} else {
				return NO_DATA;
			}
		}

	}

	@Override
	public void write(int address, int data) {
		Preconditions.checkBits8(data);
		Preconditions.checkBits16(address);
		switch (address) {
		case AddressMap.REG_IE:
			IERegister = data;
			break;
		case AddressMap.REG_IF:
			IFRegister = data;
			break;
		default:
			if (isInHighRamBounds(address)) {
				highRam.write(address - AddressMap.HIGH_RAM_START, data);
			}
		}
	}

	/**
	 * Set the bit corresponding to the given interrupt to 1 in the IF register
	 * 
	 * @param interrupt
	 *            the interrupt that will be raised (must not be null)
	 * @throws NullPointerException
	 *             if interrupt is null
	 */
	public void requestInterrupt(Interrupt interrupt) {
		IFRegister = Bits.set(IFRegister, interrupt.index(), true);
	}

	@Override
	public void attachTo(Bus bus) {
		bus.attach(this);
		this.bus = bus;
	}

	// Returns true if an interruption was handled
	private boolean handleInterruptions() {
		int interruptsRaisedAndEnabled = IERegister & IFRegister;
		if (interruptsRaisedAndEnabled != 0) {
			IMERegister = false;
			int index = Integer.numberOfTrailingZeros(interruptsRaisedAndEnabled);
			IFRegister = Bits.set(IFRegister, index, false);
			push16(PCRegister);
			PCRegister = AddressMap.INTERRUPTS[index];
			nextNonIdleCycle += 5;
			return true;
		}
		return false;
	}

	// Execute the given opcode
	private void dispatch(Opcode opcode) {

		int nextPC = PCRegister + opcode.totalBytes;
		int encoding = opcode.encoding;

		switch (opcode.family) {
		// Load
		case NOP:
			break;
		case LD_R8_HLR:
			regF.set(extractReg(opcode, 3), read8AtHL());
			break;
		case LD_A_HLRU:
			regF.set(Reg.A, read8AtHL());
			incrementHLAccordingToEncoding(encoding);
			break;
		case LD_A_N8R:
			regF.set(Reg.A, read8(AddressMap.REGS_START + read8AfterOpcode()));
			break;
		case LD_A_CR:
			regF.set(Reg.A, read8(AddressMap.REGS_START + regF.get(Reg.C)));
			break;
		case LD_A_N16R:
			regF.set(Reg.A, read8(read16AfterOpcode()));
			break;
		case LD_A_BCR:
			regF.set(Reg.A, read8(getValueFromR16(Reg16.BC)));
			break;
		case LD_A_DER:
			regF.set(Reg.A, read8(getValueFromR16(Reg16.DE)));
			break;
		case LD_R8_N8:
			regF.set(extractReg(opcode, 3), read8AfterOpcode());
			break;
		case LD_R16SP_N16:
			setR16SP(extractReg16(opcode), read16AfterOpcode());
			break;
		case POP_R16:
			setR16(extractReg16(opcode), pop16());
			break;
		case LD_HLR_R8:
			write8AtHL(regF.get(extractReg(opcode, 0)));
			break;
		case LD_HLRU_A:
			write8AtHL(regF.get(Reg.A));
			incrementHLAccordingToEncoding(encoding);
			break;
		case LD_N8R_A:
			write8(AddressMap.REGS_START + read8AfterOpcode(), regF.get(Reg.A));
			break;
		case LD_CR_A:
			write8(AddressMap.REGS_START + regF.get(Reg.C), regF.get(Reg.A));
			break;
		case LD_N16R_A:
			write8(read16AfterOpcode(), regF.get(Reg.A));
			break;
		case LD_BCR_A:
			write8(getValueFromR16(Reg16.BC), regF.get(Reg.A));
			break;
		case LD_DER_A:
			write8(getValueFromR16(Reg16.DE), regF.get(Reg.A));
			break;
		case LD_HLR_N8:
			write8AtHL(read8AfterOpcode());
			break;
		case LD_N16R_SP:
			write16(read16AfterOpcode(), SPRegister);
			break;
		case LD_R8_R8: {
			Reg sendingReg = extractReg(opcode, 0);
			Reg receivingReg = extractReg(opcode, 3);
			regF.set(receivingReg, regF.get(sendingReg));
		}
			break;
		case LD_SP_HL:
			SPRegister = getValueFromR16(Reg16.HL);
			break;
		case PUSH_R16:
			push16(getValueFromR16(extractReg16(opcode)));
			break;

		// Add
		case ADD_A_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.add(regF.get(Reg.A), regF.get(register), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case ADD_A_N8: {
			int result = Alu.add(regF.get(Reg.A), read8AfterOpcode(), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case ADD_A_HLR: {
			int result = Alu.add(regF.get(Reg.A), read8AtHL(), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case INC_R8: {
			Reg register = extractReg(opcode, 3);
			int result = Alu.add(regF.get(register), 1);
			setRegAndCombinedFlags(register, result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case INC_HLR: {
			int result = Alu.add(read8AtHL(), 1);
			write8AtHLAndSetCombinedFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case INC_R16SP: {
			Reg16 pair = extractReg16(opcode);
			int result = Bits.clip(16, getValueFromR16SP(pair) + 1);
			setR16SP(pair, result);
		}
			break;
		case ADD_HL_R16SP: {
			Reg16 pair = extractReg16(opcode);
			int result = Alu.add16H(getValueFromR16(Reg16.HL), getValueFromR16SP(pair));
			setR16(Reg16.HL, Alu.unpackValue(result));
			combineAluFlags(result, FlagSrc.CPU, FlagSrc.V0, FlagSrc.ALU, FlagSrc.ALU);
		}
			break;
		case LD_HLSP_S8: {
			int signedValue = read8AfterOpcodeAsSigned16();
			int result = Alu.add16L(SPRegister, signedValue);
			if (Bits.test(encoding, 4)) {
				setR16(Reg16.HL, Alu.unpackValue(result));
			} else {
				SPRegister = Alu.unpackValue(result);
			}
			setFlags(result);
		}
			break;

		// Subtract
		case SUB_A_R8: {
			Reg regToUse = extractReg(opcode, 0);
			int result = Alu.sub(regF.get(Reg.A), regF.get(regToUse), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case SUB_A_N8: {
			int result = Alu.sub(regF.get(Reg.A), read8AfterOpcode(), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case SUB_A_HLR: {
			int result = Alu.sub(regF.get(Reg.A), read8AtHL(), carryValue(opcode));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case DEC_R8: {
			Reg register = extractReg(opcode, 3);
			int result = Alu.sub(regF.get(register), 1, false);
			setRegAndCombinedFlags(register, result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case DEC_HLR: {
			int result = Alu.sub(read8AtHL(), 1, false);
			write8AtHLAndSetCombinedFlags(result, FlagSrc.ALU, FlagSrc.V1, FlagSrc.ALU, FlagSrc.CPU);
		}
			break;
		case CP_A_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.sub(regF.get(Reg.A), regF.get(register), false);
			setFlags(result);
		}
			break;
		case CP_A_N8: {
			int result = Alu.sub(regF.get(Reg.A), read8AfterOpcode(), false);
			setFlags(result);
		}
			break;
		case CP_A_HLR: {
			int result = Alu.sub(regF.get(Reg.A), read8AtHL(), false);
			setFlags(result);
		}
			break;
		case DEC_R16SP: {
			Reg16 pair = extractReg16(opcode);
			int result = Bits.clip(16, getValueFromR16SP(pair) - 1);
			setR16SP(pair, result);
		}
			break;

		// And, or, xor, complement
		case AND_A_N8: {
			int result = Alu.and(regF.get(Reg.A), read8AfterOpcode());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case AND_A_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.and(regF.get(Reg.A), regF.get(register));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case AND_A_HLR: {
			int result = Alu.and(regF.get(Reg.A), read8AtHL());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case OR_A_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.or(regF.get(Reg.A), regF.get(register));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case OR_A_N8: {
			int result = Alu.or(regF.get(Reg.A), read8AfterOpcode());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case OR_A_HLR: {
			int result = Alu.or(regF.get(Reg.A), read8AtHL());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case XOR_A_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.xor(regF.get(Reg.A), regF.get(register));
			setRegAndFlags(Reg.A, result);
		}
			break;
		case XOR_A_N8: {
			int result = Alu.xor(regF.get(Reg.A), read8AfterOpcode());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case XOR_A_HLR: {
			int result = Alu.xor(regF.get(Reg.A), read8AtHL());
			setRegAndFlags(Reg.A, result);
		}
			break;
		case CPL: {
			int value = regF.get(Reg.A);
			value = Bits.clip(8, ~value);
			regF.set(Reg.A, value);
			combineAluFlags(0, FlagSrc.CPU, FlagSrc.V1, FlagSrc.V1, FlagSrc.CPU);
		}
			break;

		// Rotate, shift
		case ROTCA: {
			int result = Alu.rotate(extractRotDir(opcode), regF.get(Reg.A));
			setRegAndCombinedFlags(Reg.A, result, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case ROTA: {
			int result = Alu.rotate(extractRotDir(opcode), regF.get(Reg.A), regF.testBit(Reg.F, Flag.C));
			setRegAndCombinedFlags(Reg.A, result, FlagSrc.V0, FlagSrc.V0, FlagSrc.V0, FlagSrc.ALU);
		}
			break;
		case ROTC_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.rotate(extractRotDir(opcode), regF.get(register));
			setRegAndFlags(register, result);
		}
			break;
		case ROT_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.rotate(extractRotDir(opcode), regF.get(register), regF.testBit(Reg.F, Flag.C));
			setRegAndFlags(register, result);
		}
			break;
		case ROTC_HLR: {
			int result = Alu.rotate(extractRotDir(opcode), read8AtHL());
			write8AtHLAndSetFlags(result);
		}
			break;
		case ROT_HLR: {
			int result = Alu.rotate(extractRotDir(opcode), read8AtHL(), regF.testBit(Reg.F, Flag.C));
			write8AtHLAndSetFlags(result);
		}
			break;
		case SWAP_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.swap(regF.get(register));
			setRegAndFlags(register, result);
		}
			break;
		case SWAP_HLR: {
			int result = Alu.swap(read8AtHL());
			write8AtHLAndSetFlags(result);
		}
			break;
		case SLA_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.shiftLeft(regF.get(register));
			setRegAndFlags(register, result);
		}
			break;
		case SRA_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.shiftRightA(regF.get(register));
			setRegAndFlags(register, result);
		}
			break;
		case SRL_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.shiftRightL(regF.get(register));
			setRegAndFlags(register, result);
		}
			break;
		case SLA_HLR: {
			int result = Alu.shiftLeft(read8AtHL());
			write8AtHLAndSetFlags(result);
		}
			break;
		case SRA_HLR: {
			int result = Alu.shiftRightA(read8AtHL());
			write8AtHLAndSetFlags(result);
		}
			break;
		case SRL_HLR: {
			int result = Alu.shiftRightL(read8AtHL());
			write8AtHLAndSetFlags(result);
		}
			break;

		// Bit test and set
		case BIT_U3_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Alu.testBit(regF.get(register), extractIndex(opcode));
			combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		}
			break;
		case BIT_U3_HLR: {
			int result = Alu.testBit(read8AtHL(), extractIndex(opcode));
			combineAluFlags(result, FlagSrc.ALU, FlagSrc.V0, FlagSrc.V1, FlagSrc.CPU);
		}
			break;
		case CHG_U3_R8: {
			Reg register = extractReg(opcode, 0);
			int result = Bits.set(regF.get(register), extractIndex(opcode), Bits.test(encoding, 6));
			regF.set(register, result);
		}
			break;
		case CHG_U3_HLR: {
			int result = Bits.set(read8AtHL(), extractIndex(opcode), Bits.test(encoding, 6));
			write8AtHL(result);
		}
			break;

		// Misc. ALU
		case DAA: {
			int flags = regF.get(Reg.F);
			boolean n = Bits.test(flags, Flag.N);
			boolean h = Bits.test(flags, Flag.H);
			boolean c = Bits.test(flags, Flag.C);

			int result = Alu.bcdAdjust(regF.get(Reg.A), n, h, c);
			setRegAndFlags(Reg.A, result);
		}
			break;
		case SCCF: {
			regF.setBit(Reg.F, Flag.N, false);
			regF.setBit(Reg.F, Flag.H, false);
			regF.setBit(Reg.F, Flag.C, !carryValue(opcode));
		}
			break;
		// Jumps
		case JP_HL:
			nextPC = getValueFromR16(Reg16.HL);
			break;
		case JP_N16:
			nextPC = read16AfterOpcode();
			break;
		case JP_CC_N16:
			if (checkCondition(opcode)) {
				nextNonIdleCycle += opcode.additionalCycles;
				nextPC = read16AfterOpcode();
			}
			break;
		case JR_E8:
			nextPC = Bits.clip(16, nextPC + read8AfterOpcodeAsSigned16());
			break;
		case JR_CC_E8:
			if (checkCondition(opcode)) {
				nextNonIdleCycle += opcode.additionalCycles;
				nextPC = Bits.clip(16, nextPC + read8AfterOpcodeAsSigned16());
			}
			break;

		// Calls and returns
		case CALL_N16:
			push16(nextPC);
			nextPC = read16AfterOpcode();
			break;
		case CALL_CC_N16:
			if (checkCondition(opcode)) {
				nextNonIdleCycle += opcode.additionalCycles;
				push16(nextPC);
				nextPC = read16AfterOpcode();
			}
			break;
		case RST_U3:
			push16(nextPC);
			nextPC = AddressMap.RESETS[extractIndex(opcode)];
			break;
		case RET:
			nextPC = pop16();
			break;
		case RET_CC:
			if (checkCondition(opcode)) {
				nextNonIdleCycle += opcode.additionalCycles;
				nextPC = pop16();
			}
			break;

		// Interrupts
		case EDI: {
			IMERegister = Bits.test(encoding, 3);
		}
			break;
		case RETI: {
			IMERegister = true;
			nextPC = pop16();
		}
			break;

		// Misc control
		case HALT:
			isHalted = true;
			break;
		case STOP:
			throw new Error("STOP is not implemented");
		}

		nextNonIdleCycle += opcode.cycles;
		PCRegister = nextPC;
	}

	private int read8(int address) {
		Preconditions.checkBits16(address);
		return bus.read(address);
	}

	private int read8AtHL() {
		return read8(getValueFromR16(Reg16.HL));
	}

	private int read8AtPC() {
		return read8(PCRegister);
	}

	private int read8AfterOpcode() {
		return read8(PCRegister + 1);
	}

	private int read8AfterOpcodeAsSigned16() {
		return Bits.clip(16, Bits.signExtend8(read8(PCRegister + 1)));
	}

	private int read16(int address) {
		Preconditions.checkBits16(address);
		return Bits.make16(bus.read(address + 1), bus.read(address));
	}

	private int read16AfterOpcode() {
		return read16(PCRegister + 1);
	}

	private void write8(int address, int value) {
		bus.write(address, value);
	}

	private void write16(int address, int value) {
		Preconditions.checkBits16(address);
		Preconditions.checkBits16(value);
		int lowB = Bits.clip(8, value);
		int highB = Bits.extract(value, 8, 8);
		bus.write(address, lowB);
		bus.write(address + 1, highB);
	}

	private void write8AtHL(int value) {
		write8(getValueFromR16(Reg16.HL), value);
	}

	private void push16(int value) {
		SPRegister = Bits.clip(16, SPRegister - 2);
		write16(SPRegister, value);
	}

	private int pop16() {
		int value = read16(SPRegister);
		SPRegister = Bits.clip(16, SPRegister + 2);
		return value;
	}

	private int getValueFromR16(Reg16 pair) {
		int highByte = regF.get(Reg.values()[pair.ordinal() * 2]);
		int lowByte = regF.get(Reg.values()[pair.ordinal() * 2 + 1]);
		return Bits.make16(highByte, lowByte);
	}

	private int getValueFromR16SP(Reg16 pair) {
		return pair == Reg16.AF ? SPRegister : getValueFromR16(pair);
	}

	private void setR16(Reg16 pair, int newValue) {
		Preconditions.checkBits16(newValue);
		int highByte = Bits.extract(newValue, 8, 8);
		int lowByte = (pair.equals(Reg16.AF) ? newValue & 0xF0 : Bits.clip(8, newValue));
		regF.set(Reg.values()[pair.ordinal() * 2], highByte);
		regF.set(Reg.values()[pair.ordinal() * 2 + 1], lowByte);
	}

	private void setR16SP(Reg16 pair, int newValue) {
		if (pair.equals(Reg16.AF)) {
			SPRegister = newValue;
		} else {
			setR16(pair, newValue);
		}
	}

	private void incrementHLAccordingToEncoding(int encoding) {
		int newValue = Bits.clip(16, getValueFromR16(Reg16.HL) + (Bits.test(encoding, 4) ? -1 : 1));
		setR16(Reg16.HL, newValue);
	}

	private Reg extractReg(Opcode opcode, int startIndex) {
		int code = Bits.extract(opcode.encoding, startIndex, 3);
		switch (code) {
		case 0b000:
			return Reg.B;
		case 0b001:
			return Reg.C;
		case 0b010:
			return Reg.D;
		case 0b011:
			return Reg.E;
		case 0b100:
			return Reg.H;
		case 0b101:
			return Reg.L;
		case 0b111:
			return Reg.A;
		default:
			throw new IllegalArgumentException("Illegal Opcode or erroneous startIndex");
		}
	}

	private Reg16 extractReg16(Opcode opcode) {
		int code = Bits.extract(opcode.encoding, 4, 2);
		switch (code) {
		case 0b00:
			return Reg16.BC;
		case 0b01:
			return Reg16.DE;
		case 0b10:
			return Reg16.HL;
		case 0b11:
			return Reg16.AF;
		default:
			throw new IllegalArgumentException(
					"Something went horribly wrong because Bits.extract did not work correctly");
		}
	}

	private RotDir extractRotDir(Opcode opcode) {
		return Bits.test(opcode.encoding, 3) ? RotDir.RIGHT : RotDir.LEFT;
	}

	private int extractIndex(Opcode opcode) {
		return Bits.extract(opcode.encoding, 3, 3);
	}

	private void setRegAndFlags(Reg register, int packedInteger) {
		setFlags(packedInteger);
		regF.set(register, Alu.unpackValue(packedInteger));
	}

	private void setRegAndCombinedFlags(Reg register, int packedInteger, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
		regF.set(register, Alu.unpackValue(packedInteger));
		combineAluFlags(packedInteger, z, n, h, c);
	}

	private void write8AtHLAndSetFlags(int packedInteger) {
		write8AtHL(Alu.unpackValue(packedInteger));
		setFlags(packedInteger);
	}

	private void write8AtHLAndSetCombinedFlags(int packedInteger, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
		write8AtHL(Alu.unpackValue(packedInteger));
		combineAluFlags(packedInteger, z, n, h, c);
	}

	private void setFlags(int packedInteger) {
		regF.set(Reg.F, Alu.unpackFlags(packedInteger));
	}

	private void combineAluFlags(int packedInteger, FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c) {
		int v1BitVector = createBitVector(z, n, h, c, FlagSrc.V1);
		int aluBitVector = createBitVector(z, n, h, c, FlagSrc.ALU) & Alu.unpackFlags(packedInteger);
		int cpuBitVector = createBitVector(z, n, h, c, FlagSrc.CPU) & regF.get(Reg.F);
		setFlags(v1BitVector | aluBitVector | cpuBitVector);

	}

	private int createBitVector(FlagSrc z, FlagSrc n, FlagSrc h, FlagSrc c, FlagSrc flagWanted) {
		return Alu.maskZNHC(z.equals(flagWanted), n.equals(flagWanted), h.equals(flagWanted), c.equals(flagWanted));
	}

	private boolean carryValue(Opcode opcode) {
		return regF.testBit(Reg.F, Flag.C) && Bits.test(opcode.encoding, 3);
	}

	private boolean checkCondition(Opcode opcode) {
		int condition = Bits.extract(opcode.encoding, 3, 2);
		switch (condition) {
		case 0b00:
			return !Bits.test(regF.get(Reg.F), Flag.Z);
		case 0b01:
			return Bits.test(regF.get(Reg.F), Flag.Z);
		case 0b10:
			return !Bits.test(regF.get(Reg.F), Flag.C);
		case 0b11:
			return Bits.test(regF.get(Reg.F), Flag.C);
		default:
			throw new IllegalArgumentException("Something went horribly wrong because extract did not work correctly");
		}
	}

	private boolean isInHighRamBounds(int address) {
		return address >= AddressMap.HIGH_RAM_START && address < AddressMap.HIGH_RAM_END;
	}

	private static Opcode[] buildOpcodeTable(Opcode.Kind desiredKind) {
		Opcode[] desiredOpcodes = new Opcode[0xFF + 1];
		for (Opcode opcode : Opcode.values()) {
			if (opcode.kind == desiredKind)
				desiredOpcodes[opcode.encoding] = opcode;
		}
		return desiredOpcodes;
	}

}

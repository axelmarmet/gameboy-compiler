package ch.epfl.bonus.compiler;

import java.util.LinkedList;
import java.util.List;

import ch.epfl.bonus.compiler.Compiler.ConditionalBody;
import ch.epfl.bonus.compiler.Compiler.Function;
import ch.epfl.bonus.compiler.Compiler.WhileLoop;
import ch.epfl.bonus.language.GameboyLanguageException;
import ch.epfl.gameboj.bits.Bits;

public final class AugmentedOpcode {

	public enum TO_RETRIEVE {
		ROM_ADDRESS, STACK, COMPLEMENT_STACK
	}

	private final boolean isPrefixed;
	private final Opcode opcode;
	private final Object argument;
	private TO_RETRIEVE paramToRetrieve;

	public AugmentedOpcode(Opcode opcode) {
		this(false, opcode, null);
	}

	public AugmentedOpcode(Opcode opcode, Object argument) {
		this(false, opcode, argument);
	}

	public AugmentedOpcode(boolean isPrefixed, Opcode opcode, Object argument) {
		this.isPrefixed = isPrefixed;
		this.opcode = opcode;
		this.argument = argument;
	}

	public AugmentedOpcode(Opcode opcode, Object argument, TO_RETRIEVE parameter) {
		this(opcode, argument);
		if (!(argument instanceof String))
			throw new IllegalArgumentException(
					"This constructor should only be used when the argument is a String that is function signature");
		this.paramToRetrieve = parameter;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (isPrefixed)
			sb.append("0xCB\n");
		sb.append(opcode.name() + "\n");
		if (argument instanceof Integer)
			sb.append(Integer.toHexString((int) argument));
		if (argument instanceof Function)
			sb.append("function with address " + ((Function) argument).getRomAddress());
		sb.append('\n');
		sb.append('\n');
		return sb.toString();
	}

	public List<Integer> translate() {
		List<Integer> instructions = new LinkedList<>();
		if (isPrefixed)
			instructions.add(0xCB);
		instructions.add(opcode.encoding);
		if (argument == null) {
			// Add nothing
		} else if (argument instanceof Integer) {
			int arg = (int) argument;
			if (!isPrefixed) {
				if (opcode.totalBytes == 2) {
					instructions.add(arg);
				} else {
					instructions.add(arg & 0xFF);
					instructions.add(arg >> 8);
				}
			} else {
				if (opcode.totalBytes == 3) {
					instructions.add(arg);
				} else {
					instructions.add(arg & 0xFF);
					instructions.add(arg >> 8);
				}
			}
		} else if (argument instanceof String) {
			int address = 0;
			Function function = Compiler.functionSignatures.get(argument);
			if (function == null)
				throw new GameboyLanguageException(argument + " hasn't been declared");
			switch (paramToRetrieve) {
			case ROM_ADDRESS:
				address = function.getRomAddress();
				break;
			case STACK:
				address = function.getVariablesSize();
				break;
			case COMPLEMENT_STACK:
				address = (~function.getVariablesSize()) + 1;
				break;
			}
			instructions.add(address & 0xFF);
			instructions.add(address >> 8);
		} else if (argument instanceof ConditionalBody) {
			int jump = ((ConditionalBody) argument).size();
			if (jump > 127) {
				throw new GameboyLanguageException("Body to big to do a relative jump");
			}
			instructions.add(jump);
		} else if (argument instanceof WhileLoop) {
			int size = ((WhileLoop) argument).size() + Opcode.JR_E8.totalBytes;
			if (size > 127) {
				throw new GameboyLanguageException("Body to big to do a relative jump");
			}
			instructions.add(Bits.clip(16, ~size + 1));
		} else {
			throw new IllegalArgumentException("Argument " + argument.toString() + " had unexpected type");
		}
		return instructions;
	}

	public int size() {
		return opcode.totalBytes;
	}

}

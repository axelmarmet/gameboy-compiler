package ch.epfl.bonus.compiler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.epfl.bonus.compiler.AugmentedOpcode.TO_RETRIEVE;
import ch.epfl.bonus.language.GameboyLanguageException;
import ch.epfl.bonus.parser.Expression;
import ch.epfl.bonus.parser.Expression.Binary;
import ch.epfl.bonus.parser.Expression.FunctionBody;
import ch.epfl.bonus.parser.Expression.FunctionCall;
import ch.epfl.bonus.parser.Expression.IfStatement;
import ch.epfl.bonus.parser.Expression.Litteral;
import ch.epfl.bonus.parser.Expression.ReturnStatement;
import ch.epfl.bonus.parser.Expression.VariableDeclaration;
import ch.epfl.bonus.parser.Expression.Visitor;
import ch.epfl.bonus.parser.Expression.WhileStatement;
import ch.epfl.bonus.scanner.Token;
import ch.epfl.bonus.scanner.TokenType;
import ch.epfl.bonus.tools.CountingOutputStream;
import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.bits.Bits;

public class Compiler {

	private FreeSpace romSpace = new FreeSpace(CartridgeOrganization.ROM_FUN_AREA_START,
			CartridgeOrganization.ROM_FUN_AREA_SIZE);
	static public Map<String, Function> functionSignatures = new HashMap<>();
	private Function currentFunction;

	class CompilerVisitor implements Visitor<List<AugmentedOpcode>> {

		@Override
		public List<AugmentedOpcode> visitBinaryExpr(Binary binaryExpr) {
			// In the base case the left operand is visited first, its result is then put in
			// the register B. Then the right operand is visited and its result is put in
			// the register A. Finally the opcode corresponding to the operator is executed

			// The equal operator is special because it is the only binary expression where
			// a variable can be assigned a new value. Since its behaviour is quite
			// different from other binary operators it is defined in its own visit function
			if (binaryExpr.operator.type == TokenType.EQUAL)
				return visitEqualBinary(binaryExpr.left, binaryExpr.right);
			List<AugmentedOpcode> bytecode = new LinkedList<>();
			// If the right operand is a function call we need to create a temporary
			// variable to hold the value of the left operand as we have no guarantee that
			// the register B won't be used
			if (binaryExpr.right instanceof Expression.FunctionCall) {
				// The character underscore is used because it is not a valid char in the
				// language so there is no risk that it is the name of an actual variable
				// defined by the programmer
				String tempName = "_" + currentFunction.getVariablesSize();
				Expression tempDeclaration = new Expression.VariableDeclaration(tempName, binaryExpr.left);
				Expression tempVar = new Expression.Litteral(new Token(TokenType.IDENTIFIER, tempName, tempName, 0));
				bytecode.addAll(tempDeclaration.accept(this));
				bytecode.addAll(binaryExpr.right.accept(this));
				bytecode.add(new AugmentedOpcode(Opcode.LD_C_A));
				bytecode.addAll(tempVar.accept(this));
				bytecode.add(new AugmentedOpcode(Opcode.LD_B_A));
				bytecode.add(new AugmentedOpcode(Opcode.LD_A_C));
			} else {
				bytecode.addAll(binaryExpr.left.accept(this));
				bytecode.add(new AugmentedOpcode(Opcode.LD_B_A));
				bytecode.addAll(binaryExpr.right.accept(this));
			}
			switch (binaryExpr.operator.type) {
			case SMALLER:
			case GREATER_OR_EQUAL:
				bytecode.add(new AugmentedOpcode(Opcode.LD_C_A));
				bytecode.add(new AugmentedOpcode(Opcode.LD_A_B));
				bytecode.add(new AugmentedOpcode(Opcode.LD_B_C));
				//$FALL-THROUGH$
			case SMALLER_OR_EQUAL:
			case GREATER:
			case EQUAL_EQUAL:
			case BANG_EQUAL:
				bytecode.add(new AugmentedOpcode(Opcode.CP_A_B));
				break;
			case PLUS:
				bytecode.add(new AugmentedOpcode(Opcode.ADD_A_B));
				break;
			case MINUS:
				// In case of the subtraction the two registers must be swapped
				bytecode.add(new AugmentedOpcode(Opcode.LD_C_B));
				bytecode.add(new AugmentedOpcode(Opcode.LD_B_A));
				bytecode.add(new AugmentedOpcode(Opcode.LD_A_C));
				bytecode.add(new AugmentedOpcode(Opcode.SUB_A_B));
				break;
			case OR:
				bytecode.add(new AugmentedOpcode(Opcode.OR_A_B));
				break;
			case AND:
				bytecode.add(new AugmentedOpcode(Opcode.AND_A_B));
				break;
			case XOR:
				bytecode.add(new AugmentedOpcode(Opcode.XOR_A_B));
				break;
			default:
				throw new GameboyLanguageException(binaryExpr.operator.type + " not supported yet");
			}
			return bytecode;
		}

		@Override
		public List<AugmentedOpcode> visitLitteralExpr(Litteral litteralExpr) {
			switch (litteralExpr.value.type) {
			case NUMBER:
				return List.of(new AugmentedOpcode(Opcode.LD_A_N8, litteralExpr.value.litteral));
			case IDENTIFIER: {
				List<AugmentedOpcode> bytecodes = new LinkedList<>();
				int index = currentFunction.getVariableIndex((String) litteralExpr.value.litteral);
				if (index == 0) {
					bytecodes.add(new AugmentedOpcode(Opcode.LD_A_HLR));
				} else {
					bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, get16BitComplement(index)));
					bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
					bytecodes.add(new AugmentedOpcode(Opcode.LD_A_HLR));
					bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, index));
					bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
				}
				return bytecodes;
			}
			default:
				throw new IllegalArgumentException("Unexpected type " + litteralExpr.value.type);
			}
		}

		@Override
		public List<AugmentedOpcode> visitVariableDeclarationExpr(VariableDeclaration declarationExpr) {
			List<AugmentedOpcode> bytecode = new LinkedList<>();
			int relativeIndex = currentFunction.addVariableToStack(declarationExpr.name);
			int complement = get16BitComplement(relativeIndex);

			// Get the value that will be assigned
			bytecode.addAll(declarationExpr.value.accept(this));

			// Go down to the right place in the stack
			bytecode.add(new AugmentedOpcode(Opcode.LD_DE_N16, complement));
			bytecode.add(new AugmentedOpcode(Opcode.ADD_HL_DE));

			// Assign the value to the correct address
			bytecode.add(new AugmentedOpcode(Opcode.LD_HLR_A));

			// Go back to the top of the stack
			bytecode.add(new AugmentedOpcode(Opcode.LD_DE_N16, relativeIndex));
			bytecode.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
			return bytecode;
		}

		public List<AugmentedOpcode> visitFunctionCallExpr(FunctionCall functionCallExpr) {

			List<AugmentedOpcode> bytecodes = new LinkedList<>();
			String calledFunctionName = functionCallExpr.name;
			int calledFunctionNumberOfArguments = functionCallExpr.arguments.size();
			String calledFunctionSignature = calledFunctionName + calledFunctionNumberOfArguments;
			int counter = 0;

			for (Expression e : functionCallExpr.arguments) {
				// Fetch the argument
				if (counter != 0) {
					bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, calledFunctionSignature,
							TO_RETRIEVE.COMPLEMENT_STACK));
					bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
				}

				bytecodes.addAll(e.accept(this));

				bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, calledFunctionSignature, TO_RETRIEVE.STACK));
				bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));

				// Write it into the stack for the function called
				bytecodes.addAll(Collections.nCopies(counter, new AugmentedOpcode(Opcode.DEC_HL)));
				bytecodes.add(new AugmentedOpcode(Opcode.LD_HLR_A));
				bytecodes.addAll(Collections.nCopies(counter, new AugmentedOpcode(Opcode.INC_HL)));

				counter++;
			}

			bytecodes.add(new AugmentedOpcode(Opcode.CALL_N16, calledFunctionSignature, TO_RETRIEVE.ROM_ADDRESS));
			return bytecodes;
		}

		public List<AugmentedOpcode> visitFunctionBodyExpr(FunctionBody functionBodyExpr) {
			String name = functionBodyExpr.name;
			int numberOfArguments = functionBodyExpr.nameOfArguments.size();
			String functionSignature = name + numberOfArguments;
			if (functionSignatures.containsKey(functionSignature)) {
				throw new GameboyLanguageException("a function called " + name + " with " + numberOfArguments
						+ " arguments has already been declared");
			} else {
				// It is necessary to have a reference to the current function being compiled as
				// for example a variable need to know to which function it belongs when it
				// wants to find its current value
				currentFunction = new Function(name, numberOfArguments);
				functionSignatures.put(functionSignature, currentFunction);
			}

			for (String n : functionBodyExpr.nameOfArguments) {
				currentFunction.addVariableToStack(n);
			}
			// Main is the only function where the stack needs to be incremented "manually"
			// because it is the only function not called
			if (functionSignature.equals("main0")) {
				currentFunction
						.addBytecode(new AugmentedOpcode(Opcode.LD_DE_N16, functionSignature, TO_RETRIEVE.STACK));
				currentFunction.addBytecode(new AugmentedOpcode(Opcode.ADD_HL_DE));
			}

			for (Expression e : functionBodyExpr.expressions) {
				currentFunction.addAllBytecodes(e.accept(this));
			}

			if (name.equals("main"))
				currentFunction.addBytecode(new AugmentedOpcode(Opcode.HALT));
			else {
				currentFunction.addAllBytecodes(getEndOfFunction());
			}

			currentFunction.romAddress = romSpace.allocate(currentFunction.size());

			return currentFunction.bytecodes;
		}

		private List<AugmentedOpcode> visitEqualBinary(Expression leftOperand, Expression rightOperand) {
			List<AugmentedOpcode> bytecodes = new LinkedList<>();
			Token leftToken = ((Litteral) leftOperand).value;
			bytecodes.addAll(rightOperand.accept(this));
			switch (leftToken.type) {
			case OUTPUT:
				bytecodes.add(new AugmentedOpcode(Opcode.LD_N16R_A, AddressMap.SERIAL_PORT));
				break;
			case IDENTIFIER:
				int index = currentFunction.getVariableIndex((String) leftToken.litteral);
				bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, index));
				bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
				bytecodes.add(new AugmentedOpcode(Opcode.LD_HLR_A));
				bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, get16BitComplement(index)));
				bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
				break;
			default:
				throw new GameboyLanguageException("Parser error : unexpected type " + leftToken.type);
			}
			return bytecodes;
		}

		@Override
		public List<AugmentedOpcode> visitIfStatementExpr(IfStatement ifStatementExpr) {
			List<AugmentedOpcode> bytecode = new LinkedList<>();
			bytecode.addAll(ifStatementExpr.condition.accept(this));

			ConditionalBody firstBody = createBody(ifStatementExpr.body1);
			bytecode.add(getConditionalJumpToEndOfBody(ifStatementExpr.condition.operator, firstBody));

			// Potentially create the second body
			if (ifStatementExpr.body2 != null) {
				ConditionalBody secondBody = createBody(ifStatementExpr.body2);
				firstBody.add(new AugmentedOpcode(Opcode.JR_E8, secondBody));
				bytecode.addAll(firstBody.getBytecode());
				bytecode.addAll(secondBody.getBytecode());
			} else {
				bytecode.addAll(firstBody.getBytecode());
			}

			return bytecode;
		}

		@Override
		public List<AugmentedOpcode> visitWhileStatementExpr(WhileStatement whileStatement) {
			List<AugmentedOpcode> bytecodes = new LinkedList<>();

			ConditionalBody conditionalBody = createBody(whileStatement.body);
			WhileLoop loop = new WhileLoop(whileStatement.condition.accept(this), conditionalBody);

			bytecodes.addAll(loop.condition);

			conditionalBody.add(new AugmentedOpcode(Opcode.JR_E8, loop));
			bytecodes.add(getConditionalJumpToEndOfBody(whileStatement.condition.operator, conditionalBody));

			bytecodes.addAll(conditionalBody.getBytecode());
			return bytecodes;
		}

		@Override
		public List<AugmentedOpcode> visitReturnStatementExpr(ReturnStatement returnStatementExpr) {
			List<AugmentedOpcode> bytecodes = new LinkedList<>();
			bytecodes.addAll(returnStatementExpr.returnValue.accept(this));
			bytecodes.addAll(getEndOfFunction());
			return bytecodes;
		}

		private ConditionalBody createBody(List<Expression> expressions) {
			List<AugmentedOpcode> bytecodes = new LinkedList<>();
			expressions.forEach((x) -> {
				bytecodes.addAll(x.accept(this));
			});
			return new ConditionalBody(bytecodes);
		}
	}

	public void compile(List<Expression> expressions) throws IOException {
		CompilerVisitor visitor = new CompilerVisitor();
		List<AugmentedOpcode> instructions = new LinkedList<>();
		functionSignatures.clear();

		// We know by the way that the parser is implemented that all top expressions
		// contained in the given list are FunctionBody
		// Thus we can simply start adding function defined by AugmentedOpcodes by
		// visiting each expression of the list
		// After this step the only thing left is to translate the AugmentedOpocodes
		// into normal Opcodes
		for (Expression e : expressions) {
			if (!(e instanceof Expression.FunctionBody)) {
				throw new GameboyLanguageException("Parser error");
			}
			instructions.addAll(e.accept(visitor));
		}

		// Writes the compiled code in binary to a savefile
		try (CountingOutputStream out = new CountingOutputStream(new FileOutputStream("save.gb"))) {
			writeInterruptHandler(out);
			writeHeader(out);
			for (AugmentedOpcode bytecode : instructions) {
				for (int i : bytecode.translate()) {
					out.write(i);
				}
			}
			// Ensures that the size of the cartridge is exactly 0x8000 bits
			int spaceLeft = CartridgeOrganization.CARTRIDGE_SIZE - out.count();
			for (int i = 0; i < spaceLeft; ++i) {
				out.write(0);
			}
		}
	}

	private void writeInterruptHandler(OutputStream out) throws IOException {
		for (int i = 0; i < CartridgeOrganization.INTERRUPT_HANDLER_SIZE; ++i) {
			out.write(0x00);
		}
	}

	private void writeHeader(OutputStream out) throws IOException {
		Function main = functionSignatures.get("main0");
		if (main == null) {
			throw new GameboyLanguageException("Main function couldn't be found");
		}
		int romAddressOfMain = main.romAddress;
		// encodes the jump to the beginning of the main function
		out.write(Opcode.JP_N16.encoding);
		out.write(romAddressOfMain & 0xFF);
		out.write(romAddressOfMain >> 8);
		// Fills the rest of the cartridge header with zeroes
		for (int i = 0x103; i < CartridgeOrganization.HEADER_END; ++i) {
			out.write(0x00);
		}
	}

	private List<AugmentedOpcode> getEndOfFunction() {
		List<AugmentedOpcode> bytecodes = new LinkedList<>();
		bytecodes.add(new AugmentedOpcode(Opcode.LD_DE_N16, currentFunction.signature, TO_RETRIEVE.COMPLEMENT_STACK));
		bytecodes.add(new AugmentedOpcode(Opcode.ADD_HL_DE));
		bytecodes.add(new AugmentedOpcode(Opcode.RET));
		return bytecodes;
	}

	private AugmentedOpcode getConditionalJumpToEndOfBody(Token operator, ConditionalBody body) {
		switch (operator.type) {
		case GREATER_OR_EQUAL:
		case SMALLER_OR_EQUAL:
			return new AugmentedOpcode(Opcode.JR_C_E8, body);
		case GREATER:
		case SMALLER:
			return new AugmentedOpcode(Opcode.JR_NC_E8, body);
		case EQUAL_EQUAL:
			return new AugmentedOpcode(Opcode.JR_NZ_E8, body);
		case BANG_EQUAL:
			return new AugmentedOpcode(Opcode.JR_Z_E8, body);
		default:
			// Should not be a GameboyLanguageException as if a mistake has been made it is
			// mine and not the user's
			throw new IllegalArgumentException("Unexepcted operator " + operator);
		}
	}

	private static int get16BitComplement(int valueToComplement) {
		return Bits.clip(16, (~valueToComplement) + 1);
	}

	static final class FreeSpace {

		int startAddress;
		int size;

		FreeSpace(int startAddress, int size) {
			this.startAddress = startAddress;
			this.size = size;
		}

		public int allocate(int amount) {
			if (canShrinkBy(amount)) {
				int oldAddress = startAddress;
				shrinkBy(amount);
				return oldAddress;
			}
			throw new IllegalArgumentException("Reached end of memory");
		}

		@Override
		public String toString() {
			return "start address : " + startAddress + "\n size : " + size;
		}

		public boolean canShrinkBy(int number) {
			return size - number >= 1;
		}

		public void shrinkBy(int number) {
			startAddress += number;
			size -= number;
		}
	}

	static final class Function {

		public final String name;
		public final int numberOfArguments;
		public final String signature;
		public final List<AugmentedOpcode> bytecodes = new LinkedList<>();
		public boolean isDone;
		private int romAddress;
		private final List<String> variables = new LinkedList<>();

		public Function(String name, int numberOfArguments) {
			this.name = name;
			this.numberOfArguments = numberOfArguments;
			signature = name + numberOfArguments;
			isDone = false;
		}

		// Returns the index of the allocated variable in the stack
		public int addVariableToStack(String varName) {
			if (variables.contains(varName))
				throw new GameboyLanguageException(
						"A variable called " + varName + "has already been declared in this scope");
			int index = variables.size();
			variables.add(varName);
			return index;
		}

		public int getVariableIndex(String varName) {
			int index = variables.indexOf(varName);
			if (index == -1)
				throw new GameboyLanguageException("var " + varName + " unknown");
			return index;
		}

		public int size() {
			int sum = 0;
			for (AugmentedOpcode e : bytecodes) {
				sum += e.size();
			}
			return sum;
		}

		public void addBytecode(AugmentedOpcode b) {
			bytecodes.add(b);
		}

		public void addAllBytecodes(Collection<AugmentedOpcode> b) {
			bytecodes.addAll(b);
		}

		public int getRomAddress() {
			return romAddress;
		}

		public int getVariablesSize() {
			return variables.size();
		}
	}

	static final class ConditionalBody {

		private final List<AugmentedOpcode> bytecodes;

		public ConditionalBody(List<AugmentedOpcode> bytecodes) {
			this.bytecodes = bytecodes;
		}

		public int size() {
			int sum = 0;
			for (AugmentedOpcode e : bytecodes) {
				sum += e.size();
			}
			return sum;
		}

		public List<AugmentedOpcode> getBytecode() {
			return bytecodes;
		}

		public void add(AugmentedOpcode bytecode) {
			bytecodes.add(bytecode);
		}
	}

	static final class WhileLoop {

		private final List<AugmentedOpcode> condition;
		private final ConditionalBody body;

		public WhileLoop(List<AugmentedOpcode> condition, ConditionalBody body) {
			this.condition = condition;
			this.body = body;
		}

		public int size() {
			int sizeOfCondition = 0;
			for (AugmentedOpcode e : condition) {
				sizeOfCondition += e.size();
			}
			return sizeOfCondition + body.size();
		}

		public List<AugmentedOpcode> getCondition() {
			return condition;
		}

	}
}

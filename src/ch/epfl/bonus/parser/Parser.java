package ch.epfl.bonus.parser;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import ch.epfl.bonus.language.GameboyLanguageException;
import ch.epfl.bonus.scanner.Token;
import ch.epfl.bonus.scanner.TokenType;

public final class Parser {

	private final List<Token> tokens;
	private static final TokenType[] LOGIC_OPERATORS = { TokenType.AND, TokenType.OR, TokenType.XOR };
	private static final TokenType[] ARITHMETIC_OPERATORS = { TokenType.PLUS, TokenType.MINUS };
	private static final TokenType[] COMPARISON_OPERTORS = { TokenType.EQUAL_EQUAL, TokenType.BANG_EQUAL,
			TokenType.SMALLER_OR_EQUAL, TokenType.GREATER_OR_EQUAL, TokenType.SMALLER, TokenType.GREATER };
	private static final TokenType[] VALID_STARTS = { TokenType.VAR, TokenType.IDENTIFIER, TokenType.IF,
			TokenType.WHILE, TokenType.OUTPUT, TokenType.RETURN };

	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Expression> parse() {
		return parseFunctions();
	}

	private List<Expression> parseFunctions() {
		List<Expression> functions = new LinkedList<>();
		while (match(TokenType.IDENTIFIER)) {
			functions.add(parseFunctionBody());
		}
		return functions;
	}

	private Expression.FunctionCall parseFunctionCall() {
		String name = (String) advance().litteral;
		skip(TokenType.L_PARENTHESIS);
		if (match(TokenType.R_PARENTHESIS)) {
			advance();
			return new Expression.FunctionCall(name, Collections.emptyList());
		}
		List<Expression> arguments = new LinkedList<>();
		arguments.add(logic());
		while (match(TokenType.COMMA)) {
			advance();
			arguments.add(logic());
		}
		skip(TokenType.R_PARENTHESIS);
		return new Expression.FunctionCall(name, arguments);
	}

	private Expression parseFunctionBody() {
		String name = (String) advance().litteral;
		List<String> argumentsName = new LinkedList<>();
		skip(TokenType.L_PARENTHESIS);
		while (match(TokenType.IDENTIFIER) && matchNext(TokenType.COMMA)) {
			argumentsName.add((String) advance().litteral);
			advance();
		}
		if (match(TokenType.IDENTIFIER)) {
			argumentsName.add((String) advance().litteral);
		}
		skip(TokenType.R_PARENTHESIS);
		skip(TokenType.L_CURLY_BRACES);
		List<Expression> expressions = parseExpressions();
		skip(TokenType.R_CURLY_BRACES);

		return new Expression.FunctionBody(name, argumentsName, expressions);
	}

	private Expression parseVarDeclaration() {
		skip(TokenType.VAR);
		String name = (String) (advance().litteral);
		skip(TokenType.EQUAL);
		Expression value = logic();
		return new Expression.VariableDeclaration(name, value);
	}

	private Expression parseIfStatement() {
		skip(TokenType.IF);
		skip(TokenType.L_PARENTHESIS);
		Expression condition = comparison();
		skip(TokenType.R_PARENTHESIS);
		skip(TokenType.L_CURLY_BRACES);
		List<Expression> body1 = parseExpressions();
		skip(TokenType.R_CURLY_BRACES);
		List<Expression> body2 = null;
		if (match(TokenType.ELSE)) {
			advance();
			if (match(TokenType.L_CURLY_BRACES)) {
				advance();
				body2 = parseExpressions();
				skip(TokenType.R_CURLY_BRACES);
			} else if (match(TokenType.IF)) {
				body2 = parseExpressions();
			} else {
				throw new GameboyLanguageException("Unexpected TokenType " + advance().toString());
			}

		}
		return new Expression.IfStatement((Expression.Binary) condition, body1, body2);
	}

	private Expression parseWhileStatement() {
		skip(TokenType.WHILE);
		skip(TokenType.L_PARENTHESIS);
		Expression condition = comparison();
		skip(TokenType.R_PARENTHESIS);
		skip(TokenType.L_CURLY_BRACES);
		List<Expression> body = parseExpressions();
		skip(TokenType.R_CURLY_BRACES);
		return new Expression.WhileStatement((Expression.Binary) condition, body);
	}

	private Expression.ReturnStatement parseReturnStatement() {
		skip(TokenType.RETURN);
		Expression returnValue = logic();
		return new Expression.ReturnStatement(returnValue);
	}

	private List<Expression> parseExpressions() {
		List<Expression> expressions = new LinkedList<>();
		while (match(VALID_STARTS)) {
			switch (peek().type) {
			case IF:
				expressions.add(parseIfStatement());
				break;
			case WHILE:
				expressions.add(parseWhileStatement());
				break;
			case VAR:
				expressions.add(parseVarDeclaration());
				break;
			case RETURN:
				expressions.add(parseReturnStatement());
				break;
			default:
				expressions.add(assignment());
			}
		}
		return expressions;
	}

	private Expression assignment() {
		// Since there are no notion of truth for the type used in the language a var
		// cannot be assigned a comparison so the parser skips the comparison step and
		// is directly parsing logic
		Expression expression = logic();
		if (match(TokenType.EQUAL)) {
			Token operator = advance();
			Expression rightOperand = assignment();
			expression = new Expression.Binary(expression, operator, rightOperand);
		}
		return expression;
	}

	private Expression comparison() {
		Expression expression = logic();
		if (match(COMPARISON_OPERTORS)) {
			Token operator = advance();
			Expression rightOperand = logic();
			expression = new Expression.Binary(expression, operator, rightOperand);
		}
		return expression;
	}

	private Expression logic() {
		Expression expression = arithmetic();
		if (match(LOGIC_OPERATORS)) {
			Token operator = advance();
			Expression rightOperand = arithmetic();
			expression = new Expression.Binary(expression, operator, rightOperand);
		}
		return expression;
	}

	private Expression arithmetic() {
		Expression expression = value();
		while (match(ARITHMETIC_OPERATORS)) {
			Token operator = advance();
			Expression rightOperand = value();
			expression = new Expression.Binary(expression, operator, rightOperand);
		}
		return expression;
	}

	private Expression value() {
		switch (peek().type) {
		case OUTPUT:
		case NUMBER:
			return new Expression.Litteral(advance());
		case IDENTIFIER:
			if (matchNext(TokenType.L_PARENTHESIS)) {
				return parseFunctionCall();
			} else {
				return new Expression.Litteral(advance());
			}
		default:
			throw new GameboyLanguageException("Expected primary value but got " + advance());
		}
	}

	private Token skip(TokenType type) {
		if (!match(type))
			throw new GameboyLanguageException("Assumed to find " + type.name() + " , but got "
					+ tokens.get(current).type.name() + " at line " + tokens.get(current).line);
		return advance();
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				return true;
			}
		}
		return false;
	}

	private boolean matchNext(TokenType... types) {
		for (TokenType type : types) {
			if (checkNext(type)) {
				return true;
			}
		}
		return false;
	}

	private Token advance() {
		if (!endOfFile())
			current++;
		return previous();
	}

	private Token peek() {
		return tokens.get(current);
	}

	private boolean check(TokenType type) {
		if (!endOfFile() && tokens.get(current).type == type)
			return true;
		else
			return false;
	}

	private boolean checkNext(TokenType type) {
		if (current + 1 < tokens.size() && tokens.get(current + 1).type == type)
			return true;
		else
			return false;
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	private boolean endOfFile() {
		return current >= tokens.size();
	}

}

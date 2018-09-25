package ch.epfl.bonus.scanner;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import ch.epfl.bonus.language.GameboyLanguageException;

public final class Scanner {

	private int start = 0;
	private int current = 0;
	private int line = 1;
	private final String source;

	private List<Token> tokens = new LinkedList<>();

	public Scanner(String source) {
		this.source = Objects.requireNonNull(source);
	}

	// Returns an ordered list of all tokens contained in the source
	public List<Token> getTokens() {
		char c;
		while (!endOfSource()) {
			start = current;
			c = source.charAt(current);
			switch (source.charAt(current)) {
			case '\n':
				line++;
				// $FALL-THROUGH$//
			case ' ':
			case '\r':
			case '\t':
				// Simply ignore whitespace
				advance();
				break;
			case '(':
				advance();
				addToken(TokenType.L_PARENTHESIS);
				break;
			case ')':
				advance();
				addToken(TokenType.R_PARENTHESIS);
				break;
			case '{':
				advance();
				addToken(TokenType.L_CURLY_BRACES);
				break;
			case '}':
				advance();
				addToken(TokenType.R_CURLY_BRACES);
				break;
			case ',':
				advance();
				addToken(TokenType.COMMA);
				break;
			case '+':
				advance();
				addToken(TokenType.PLUS);
				break;
			case '-':
				advance();
				addToken(TokenType.MINUS);
				break;
			case '&':
				advance();
				addToken(TokenType.AND);
				break;
			case '|':
				advance();
				addToken(TokenType.OR);
				break;
			case '^':
				advance();
				addToken(TokenType.XOR);
				break;
			case '!':
				if (current + 1 < source.length() && peekNext() == '=') {
					advance();
					advance();
					addToken(TokenType.BANG_EQUAL);
				} else {
					throw new GameboyLanguageException("Unexpected char '!' at line " + line);
				}
				break;
			case '=':
				advance();
				if (!endOfSource() && peek() == '=') {
					advance();
					addToken(TokenType.EQUAL_EQUAL);
				} else {
					addToken(TokenType.EQUAL);
				}
				break;
			case '>':
				if (current + 1 < source.length() && peekNext() == '=') {
					advance();
					advance();
					addToken(TokenType.GREATER_OR_EQUAL);
				} else {
					advance();
					addToken(TokenType.GREATER);
				}
				break;
			case '<':
				if (current + 1 < source.length() && peekNext() == '=') {
					advance();
					advance();
					addToken(TokenType.SMALLER_OR_EQUAL);
				} else {
					advance();
					addToken(TokenType.SMALLER);
				}
				break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlphabetic(c)) {
					identifier();
				} else {
					throw new GameboyLanguageException("Unexpected char " + Character.getName(c) + " at line " + line);
				}
			}
		}
		return tokens;
	}

	private void addToken(TokenType type) {
		addToken(type, null);
	}

	private void addToken(TokenType type, Object litteral) {
		tokens.add(new Token(type, source.substring(start, current), litteral, line));
	}

	private boolean endOfSource() {
		return current >= source.length();
	}

	private char advance() {
		return source.charAt(current++);
	}

	private char peek() {
		return source.charAt(current);
	}

	private char peekNext() {
		return source.charAt(current + 1);
	}

	private boolean isAlphabeticOrDigit(char c) {
		return isDigit(c) || isAlphabetic(c);
	}

	private boolean isDigit(char c) {
		return '0' <= c && c <= '9';
	}

	private boolean isAlphabetic(char c) {
		return 'A' <= c && c <= 'z';
	}

	private void number() {
		int base = 10;
		if (peek() == '0') {
			if (peekNext() == 'b') {
				start += 2;
				base = 2;
				current += 2;
			} else if (peekNext() == 'x') {
				start += 2;
				base = 16;
				current += 2;
			}
		}
		while (current + 1 < source.length() && isAlphabeticOrDigit(peekNext())) {
			advance();
		}
		advance();
		int value = Integer.parseInt(source.substring(start, current), base);
		if (value > 0xFF) {
			throw new GameboyLanguageException("value " + value + " at line " + line + " does not fit in a byte");
		}
		addToken(TokenType.NUMBER, value);
	}

	private void identifier() {
		while (current + 1 < source.length() && isAlphabeticOrDigit(peek())) {
			advance();
		}
		String identifier = source.substring(start, current);
		switch (identifier) {
		case "var":
			addToken(TokenType.VAR, identifier);
			break;
		case "return":
			addToken(TokenType.RETURN);
			break;
		case "output":
			addToken(TokenType.OUTPUT);
			addToken(TokenType.EQUAL, "=");
			break;
		case "if":
			addToken(TokenType.IF);
			break;
		case "else":
			addToken(TokenType.ELSE);
			break;
		case "while":
			addToken(TokenType.WHILE);
			break;
		default:
			addToken(TokenType.IDENTIFIER, identifier);
		}
	}

}

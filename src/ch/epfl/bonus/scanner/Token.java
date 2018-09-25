package ch.epfl.bonus.scanner;

public final class Token {

	public final TokenType type;
	public final String lexem;
	public final Object litteral;
	public final int line;

	public Token(TokenType type, String lexem, Object litteral, int line) {
		this.type = type;
		this.lexem = lexem;
		this.litteral = litteral;
		this.line = line;
	}

	@Override
	public String toString() {
		return type + " " + lexem + " at " + line;
	}

}

package ch.epfl.bonus.parser;

import java.util.List;
import java.util.Objects;

import ch.epfl.bonus.scanner.Token;

public abstract class Expression {

	public interface Visitor<E> {
		public E visitBinaryExpr(Binary binaryExpr);

		public E visitLitteralExpr(Litteral litteralExpr);

		public E visitVariableDeclarationExpr(VariableDeclaration varDeclarationExpr);

		public E visitFunctionBodyExpr(FunctionBody funBodyExpr);

		public E visitFunctionCallExpr(FunctionCall funCallExpr);

		public E visitIfStatementExpr(IfStatement ifStatementExpr);

		public E visitReturnStatementExpr(ReturnStatement returnStatement);

		public E visitWhileStatementExpr(WhileStatement whileStatement);
	}

	public abstract <E> E accept(Visitor<E> visitor);

	public static final class Binary extends Expression {

		public final Expression left;
		public final Token operator;
		public final Expression right;

		public Binary(Expression left, Token operator, Expression right) {
			this.left = left;
			this.operator = operator;
			this.right = right;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitBinaryExpr(this);
		}

	}

	public static final class Litteral extends Expression {

		public final Token value;

		public Litteral(Token value) {
			this.value = value;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitLitteralExpr(this);
		}

	}

	public static final class VariableDeclaration extends Expression {

		public final String name;
		public final Expression value;

		public VariableDeclaration(String name, Expression value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitVariableDeclarationExpr(this);
		}

	}

	public static final class FunctionBody extends Expression {

		public final String name;
		public final List<String> nameOfArguments;
		public final List<Expression> expressions;

		public FunctionBody(String name, List<String> nameOfArguments, List<Expression> expressions) {
			this.name = name;
			this.nameOfArguments = nameOfArguments;
			this.expressions = expressions;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitFunctionBodyExpr(this);
		}
	}

	public static final class FunctionCall extends Expression {

		public final String name;
		public final List<Expression> arguments;

		public FunctionCall(String name, List<Expression> arguments) {
			this.name = name;
			this.arguments = arguments;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitFunctionCallExpr(this);
		}
	}

	public static final class IfStatement extends Expression {
		public final Expression.Binary condition;
		public final List<Expression> body1;
		public final List<Expression> body2;

		public IfStatement(Expression.Binary condition, List<Expression> body1, List<Expression> body2) {
			this.condition = Objects.requireNonNull(condition);
			this.body1 = Objects.requireNonNull(body1);
			this.body2 = body2;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitIfStatementExpr(this);
		}
	}

	public static final class ReturnStatement extends Expression {
		public final Expression returnValue;

		public ReturnStatement(Expression returnValue) {
			this.returnValue = returnValue;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitReturnStatementExpr(this);
		}
	}

	public static final class WhileStatement extends Expression {
		public final Expression.Binary condition;
		public final List<Expression> body;

		public WhileStatement(Expression.Binary condition, List<Expression> body) {
			this.condition = condition;
			this.body = body;
		}

		public <E> E accept(Visitor<E> visitor) {
			return visitor.visitWhileStatementExpr(this);
		}
	}
}

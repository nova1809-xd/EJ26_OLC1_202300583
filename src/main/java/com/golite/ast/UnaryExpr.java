package com.golite.ast;

import com.golite.lexer.TokenType;
import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** maneja operadores unarios como negacion y signo menos. */
public final class UnaryExpr implements Expr {

    private final TokenType operator;
    private final Expr right;

    public UnaryExpr(TokenType operator, Expr right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return interpreter.evaluateUnary(operator, right.evaluate(interpreter, environment));
    }
}
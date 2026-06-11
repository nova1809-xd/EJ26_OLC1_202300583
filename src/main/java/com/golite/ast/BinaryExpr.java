package com.golite.ast;

import com.golite.lexer.TokenType;
import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** evalua operaciones binarias de aritmetica, logica y relacion. */
public final class BinaryExpr implements Expr {

    private final Expr left;
    private final TokenType operator;
    private final Expr right;

    public BinaryExpr(Expr left, TokenType operator, Expr right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return interpreter.evaluateBinary(operator, left.evaluate(interpreter, environment), right.evaluate(interpreter, environment));
    }
}
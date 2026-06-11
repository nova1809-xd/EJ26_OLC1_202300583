package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** ejecuta una expresion solo por su efecto secundario. */
public final class ExpressionStmt implements Stmt {

    private final Expr expression;

    public ExpressionStmt(Expr expression) {
        this.expression = expression;
    }

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        expression.evaluate(interpreter, environment);
    }
}
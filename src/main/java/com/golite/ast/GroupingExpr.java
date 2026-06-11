package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** agrupa una expresion para cambiar su prioridad. */
public final class GroupingExpr implements Expr {

    private final Expr expression;

    public GroupingExpr(Expr expression) {
        this.expression = expression;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return expression.evaluate(interpreter, environment);
    }
}
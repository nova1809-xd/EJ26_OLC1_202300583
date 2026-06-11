package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** asigna un nuevo valor a una variable existente. */
public final class AssignmentExpr implements Expr {

    private final String name;
    private final Expr value;

    public AssignmentExpr(String name, Expr value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        Value evaluated = value.evaluate(interpreter, environment);
        return interpreter.assignValue(name, evaluated, environment);
    }
}
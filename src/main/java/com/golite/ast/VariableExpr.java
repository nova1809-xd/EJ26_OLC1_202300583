package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** lee el valor de una variable desde el entorno actual. */
public final class VariableExpr implements Expr {

    private final String name;

    public VariableExpr(String name) {
        this.name = name;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return environment.get(name);
    }
}
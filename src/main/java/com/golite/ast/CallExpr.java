package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

import java.util.List;

/** representa una llamada a una funcion embebida. */
public final class CallExpr implements Expr {

    private final String target;
    private final List<Expr> arguments;

    public CallExpr(String target, List<Expr> arguments) {
        this.target = target;
        this.arguments = List.copyOf(arguments);
    }

    public String target() {
        return target;
    }

    public List<Expr> arguments() {
        return arguments;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return interpreter.evaluateCall(this, environment);
    }
}
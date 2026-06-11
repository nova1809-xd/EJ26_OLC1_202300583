package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** representa un literal directo como numero, texto o booleano. */
public final class LiteralExpr implements Expr {

    private final Value value;

    public LiteralExpr(Value value) {
        this.value = value;
    }

    @Override
    public Value evaluate(Interpreter interpreter, Environment environment) {
        return value;
    }
}
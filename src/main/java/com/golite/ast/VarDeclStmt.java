package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.ValueType;

/** declara una variable con tipo opcional y valor opcional. */
public final class VarDeclStmt implements Stmt {

    private final String name;
    private final ValueType declaredType;
    private final Expr initializer;

    public VarDeclStmt(String name, ValueType declaredType, Expr initializer) {
        this.name = name;
        this.declaredType = declaredType;
        this.initializer = initializer;
    }

    public String name() {
        return name;
    }

    public ValueType declaredType() {
        return declaredType;
    }

    public Expr initializer() {
        return initializer;
    }

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        interpreter.executeVarDecl(this, environment);
    }
}
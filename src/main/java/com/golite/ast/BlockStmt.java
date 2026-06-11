package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

import java.util.List;

/** ejecuta un conjunto de instrucciones dentro de un nuevo alcance. */
public final class BlockStmt implements Stmt {

    private final List<Stmt> statements;

    public BlockStmt(List<Stmt> statements) {
        this.statements = List.copyOf(statements);
    }

    public List<Stmt> statements() {
        return statements;
    }

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        interpreter.executeBlock(statements, environment);
    }
}
package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** ejecuta una rama condicionada con soporte para else. */
public final class IfStmt implements Stmt {

    private final Expr condition;
    private final Stmt thenBranch;
    private final Stmt elseBranch;

    public IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    public Expr condition() {
        return condition;
    }

    public Stmt thenBranch() {
        return thenBranch;
    }

    public Stmt elseBranch() {
        return elseBranch;
    }

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        interpreter.executeIf(this, environment);
    }
}
package com.golite.ast;

import java.util.List;

/** representa el programa completo como una lista de instrucciones. */
public final class Program {

    private final List<Stmt> statements;

    public Program(List<Stmt> statements) {
        this.statements = List.copyOf(statements);
    }

    public List<Stmt> statements() {
        return statements;
    }
}
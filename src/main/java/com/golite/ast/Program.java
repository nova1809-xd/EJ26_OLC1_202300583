package com.golite.ast;

import java.util.List;

public class Program {
    public final List<Statement> statements;

    public Program(List<Statement> statements) {
        this.statements = statements;
    }
}
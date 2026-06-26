package com.golite.ast;

import java.util.List;

public class BlockStmt extends Statement {
    public final List<Statement> statements;

    public BlockStmt(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

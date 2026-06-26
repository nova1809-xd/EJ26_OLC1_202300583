package com.golite.ast;

public class BreakStmt extends Statement {
    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
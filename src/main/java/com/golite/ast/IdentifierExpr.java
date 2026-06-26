package com.golite.ast;

public class IdentifierExpr extends Expression {
    public final String name;

    public IdentifierExpr(String name) {
        this.name = name;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
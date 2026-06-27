package com.golite.ast;

public class LenExpr extends Expression {
    public final Expression argument;

    public LenExpr(Expression argument) {
        this.argument = argument;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
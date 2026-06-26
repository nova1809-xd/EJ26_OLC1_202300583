package com.golite.ast;

public class TypeOfExpr extends Expression {
    public final Expression argument;

    public TypeOfExpr(Expression argument) {
        this.argument = argument;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

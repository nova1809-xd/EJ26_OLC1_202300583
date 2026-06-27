package com.golite.ast;

public class AppendExpr extends Expression {
    public final Expression slice;
    public final Expression value;

    public AppendExpr(Expression slice, Expression value) {
        this.slice = slice;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
package com.golite.ast;

public class UnaryExpr extends Expression {
    public final String operator;
    public final Expression right;

    public UnaryExpr(String operator, Expression right) {
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

package com.golite.ast;

public class BinaryExpr extends Expression {
    public final Expression left;
    public final String operator;
    public final Expression right;

    public BinaryExpr(Expression left, String operator, Expression right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}    


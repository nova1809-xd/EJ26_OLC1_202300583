package com.golite.ast;

public class ExpressionStmt extends Statement {
    public final Expression expression;

    public ExpressionStmt(Expression expression) {
        this.expression = expression;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
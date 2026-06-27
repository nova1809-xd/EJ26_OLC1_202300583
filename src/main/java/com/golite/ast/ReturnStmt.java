package com.golite.ast;

public class ReturnStmt extends Statement {
    public final Expression value; // null si es "return" sin valor

    public ReturnStmt(Expression value) {
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
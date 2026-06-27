package com.golite.ast;

public class FieldAccessExpr extends Expression {
    public final Expression target; // normalmente un IdentifierExpr (ej. "p")
    public final String fieldName;

    public FieldAccessExpr(Expression target, String fieldName) {
        this.target = target;
        this.fieldName = fieldName;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
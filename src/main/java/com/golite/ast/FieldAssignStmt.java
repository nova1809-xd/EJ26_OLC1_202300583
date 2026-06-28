package com.golite.ast;

public class FieldAssignStmt extends Statement {
    public final Expression target; // normalmente un IdentifierExpr (ej. "p")
    public final String fieldName;
    public final String operator; // "=", "+=", "-=", "*=", "/=", "%="
    public final Expression value;

    public FieldAssignStmt(Expression target, String fieldName, String operator, Expression value) {
        this.target = target;
        this.fieldName = fieldName;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
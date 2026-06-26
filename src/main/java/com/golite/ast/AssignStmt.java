package com.golite.ast;

public class AssignStmt extends Statement {
    public final String name;
    public final String operator; // "=", "+=", "-=", "*=", "/=", "%="
    public final Expression value;

    public AssignStmt(String name, String operator, Expression value) {
        this.name = name;
        this.operator = operator;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
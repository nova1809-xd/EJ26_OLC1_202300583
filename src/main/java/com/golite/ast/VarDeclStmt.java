package com.golite.ast;

public class VarDeclStmt extends Statement {
    public final String name;
    public final String type;       // "int", "float64", "string", "bool", "rune"
    public final Expression initializer; // puede ser null -> valor por defecto

    public VarDeclStmt(String name, String type, Expression initializer) {
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
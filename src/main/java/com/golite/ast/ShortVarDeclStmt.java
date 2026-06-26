package com.golite.ast;

public class ShortVarDeclStmt extends Statement {
    public final String name;
    public final Expression initializer; // nunca es null en := (siempre lleva valor)

    public ShortVarDeclStmt(String name, Expression initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

package com.golite.ast;

public class LiteralExpr extends Expression {
    public final Object value;
    public final Type type;

    // estos son los tipos nativos de GoLite
    public enum Type { INT, FLOAT, STRING, BOOL, RUNE, NIL }

    public LiteralExpr(Object value, Type type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

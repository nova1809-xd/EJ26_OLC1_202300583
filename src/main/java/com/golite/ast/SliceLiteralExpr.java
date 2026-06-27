package com.golite.ast;

import java.util.List;

public class SliceLiteralExpr extends Expression {
    public final String elementType; // ej. "int", "string", o "[]int" para [][]int
    public final List<Expression> elements;

    public SliceLiteralExpr(String elementType, List<Expression> elements) {
        this.elementType = elementType;
        this.elements = elements;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
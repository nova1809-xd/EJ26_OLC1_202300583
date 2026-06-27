package com.golite.ast;

import java.util.List;

public class CallExpr extends Expression {
    public final String name;
    public final List<Expression> arguments;

    public CallExpr(String name, List<Expression> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
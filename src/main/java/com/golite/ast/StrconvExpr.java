package com.golite.ast;

public class StrconvExpr extends Expression {
    public enum Kind { ATOI, PARSE_FLOAT }

    public final Kind kind;
    public final Expression argument;

    public StrconvExpr(Kind kind, Expression argument) {
        this.kind = kind;
        this.argument = argument;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
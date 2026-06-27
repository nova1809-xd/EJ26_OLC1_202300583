package com.golite.ast;

public class IndexExpr extends Expression {
    public final Expression target; // normalmente un IdentifierExpr
    public final Expression index;

    public IndexExpr(Expression target, Expression index) {
        this.target = target;
        this.index = index;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
package com.golite.ast;

public class IndexAssignStmt extends Statement {
    public final Expression target; // normalmente un IdentifierExpr
    public final Expression index;
    public final Expression value;

    public IndexAssignStmt(Expression target, Expression index, Expression value) {
        this.target = target;
        this.index = index;
        this.value = value;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
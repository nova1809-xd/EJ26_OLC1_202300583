package com.golite.ast;

public class IfStmt extends Statement {
    public final Expression condition;
    public final BlockStmt thenBranch;
    public final Statement elseBranch; // puede ser: null, BlockStmt (else final), o IfStmt (else if)

    public IfStmt(Expression condition, BlockStmt thenBranch, Statement elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
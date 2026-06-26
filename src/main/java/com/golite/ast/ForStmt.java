package com.golite.ast;

public class ForStmt extends Statement {
    public final Statement init;       // puede ser null (for tipo while, o for { } infinito)
    public final Expression condition; // puede ser null (for infinito: for { })
    public final Statement post;       // puede ser null (for tipo while)
    public final BlockStmt body;

    public ForStmt(Statement init, Expression condition, Statement post, BlockStmt body) {
        this.init = init;
        this.condition = condition;
        this.post = post;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** representa un ciclo for clasico con init, condition y post opcionales. */
public final class ForStmt implements Stmt {

    private final Stmt initializer;
    private final Expr condition;
    private final Stmt post;
    private final Stmt body;

    public ForStmt(Stmt initializer, Expr condition, Stmt post, Stmt body) {
        this.initializer = initializer;
        this.condition = condition;
        this.post = post;
        this.body = body;
    }

    public Stmt initializer() {
        return initializer;
    }

    public Expr condition() {
        return condition;
    }

    public Stmt post() {
        return post;
    }

    public Stmt body() {
        return body;
    }

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        interpreter.executeFor(this, environment);
    }
}
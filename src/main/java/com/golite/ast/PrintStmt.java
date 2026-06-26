package com.golite.ast;

import java.util.List;

public class PrintStmt extends Statement {
    public final List<Expression> arguments; // puede estar vacia: fmt.Println()

    public PrintStmt(List<Expression> arguments) {
        this.arguments = arguments;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

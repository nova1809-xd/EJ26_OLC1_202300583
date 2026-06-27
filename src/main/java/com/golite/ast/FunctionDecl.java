package com.golite.ast;

import java.util.List;

public class FunctionDecl extends Statement {
    public final String name;
    public final List<Param> params;
    public final String returnType; // null si la funcion no retorna valor
    public final BlockStmt body;

    public FunctionDecl(String name, List<Param> params, String returnType, BlockStmt body) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
        this.body = body;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}

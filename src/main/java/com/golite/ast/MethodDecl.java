package com.golite.ast;

import java.util.List;

public class MethodDecl extends Statement {
    public final String receiverName; // ej. "p"
    public final String receiverType; // ej. "Producto"
    public final String name;
    public final List<Param> params;
    public final String returnType; // null si no retorna valor
    public final BlockStmt body;

    public MethodDecl(String receiverName, String receiverType, String name,
                       List<Param> params, String returnType, BlockStmt body) {
        this.receiverName = receiverName;
        this.receiverType = receiverType;
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
package com.golite.ast;

import java.util.List;

public class StructInstanceStmt extends Statement {
    public final String structType; // ej. "Producto"
    public final String varName;    // ej. "p"
    public final List<FieldInit> fieldInits;

    public StructInstanceStmt(String structType, String varName, List<FieldInit> fieldInits) {
        this.structType = structType;
        this.varName = varName;
        this.fieldInits = fieldInits;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
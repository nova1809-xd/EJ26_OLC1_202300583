package com.golite.ast;

import java.util.List;

public class StructDecl extends Statement {
    public final String name;
    public final List<Param> fields; // reuso Param: cada campo tiene name + type

    public StructDecl(String name, List<Param> fields) {
        this.name = name;
        this.fields = fields;
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }
}
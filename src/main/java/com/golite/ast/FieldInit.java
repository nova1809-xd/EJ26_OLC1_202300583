package com.golite.ast;

public class FieldInit {
    public final String fieldName;
    public final Expression value;

    public FieldInit(String fieldName, Expression value) {
        this.fieldName = fieldName;
        this.value = value;
    }
}
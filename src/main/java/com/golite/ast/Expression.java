package com.golite.ast;

public abstract class Expression {
    public abstract <T> T accept(Visitor<T> visitor);
}

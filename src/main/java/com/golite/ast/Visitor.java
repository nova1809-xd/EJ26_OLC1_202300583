package com.golite.ast;

public interface Visitor<T> {
    T visit(LiteralExpr expr);
    T visit(BinaryExpr expr);
    T visit(UnaryExpr expr);
}
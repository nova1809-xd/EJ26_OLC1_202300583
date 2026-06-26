package com.golite.ast;

public interface Visitor<T> {
    // --- Expresiones ---
    T visit(LiteralExpr expr);
    T visit(BinaryExpr expr);
    T visit(UnaryExpr expr);
    T visit(IdentifierExpr expr);
    T visit(TypeOfExpr expr);
    T visit(StrconvExpr expr);

    // --- Statements ---
    T visit(VarDeclStmt stmt);
    T visit(ShortVarDeclStmt stmt);
    T visit(ExpressionStmt stmt);
    T visit(PrintStmt stmt);
    T visit(AssignStmt stmt);
    T visit(BlockStmt stmt);
    T visit(IfStmt stmt);
    T visit(ForStmt stmt);
    T visit(BreakStmt stmt);
    T visit(ContinueStmt stmt);
}
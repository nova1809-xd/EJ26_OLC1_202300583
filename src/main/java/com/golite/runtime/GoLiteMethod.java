package com.golite.runtime;

import com.golite.ast.MethodDecl;

public class GoLiteMethod {
    public final MethodDecl declaration;

    public GoLiteMethod(MethodDecl declaration) {
        this.declaration = declaration;
    }
}
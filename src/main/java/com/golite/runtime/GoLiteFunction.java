package com.golite.runtime;

import com.golite.ast.FunctionDecl;

public class GoLiteFunction {
    public final FunctionDecl declaration;

    public GoLiteFunction(FunctionDecl declaration) {
        this.declaration = declaration;
    }

    public String getName() {
        return declaration.name;
    }

    public int getParamCount() {
        return declaration.params.size();
    }
}

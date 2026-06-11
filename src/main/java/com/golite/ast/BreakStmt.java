package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** rompe el ciclo for mas cercano. */
public final class BreakStmt implements Stmt {

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        throw new Interpreter.BreakSignal();
    }
}
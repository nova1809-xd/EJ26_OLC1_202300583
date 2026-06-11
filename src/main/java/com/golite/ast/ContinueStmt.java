package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** salta a la siguiente iteracion del ciclo for mas cercano. */
public final class ContinueStmt implements Stmt {

    @Override
    public void execute(Interpreter interpreter, Environment environment) {
        throw new Interpreter.ContinueSignal();
    }
}
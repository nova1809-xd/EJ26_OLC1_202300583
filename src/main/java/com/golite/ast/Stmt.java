package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;

/** define una instruccion ejecutable dentro del interprete. */
public interface Stmt {

    void execute(Interpreter interpreter, Environment environment);
}
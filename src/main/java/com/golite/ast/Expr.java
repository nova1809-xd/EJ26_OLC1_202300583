package com.golite.ast;

import com.golite.runtime.Environment;
import com.golite.runtime.Interpreter;
import com.golite.runtime.Value;

/** define una expresion que produce un valor al evaluarse. */
public interface Expr {

    Value evaluate(Interpreter interpreter, Environment environment);
}
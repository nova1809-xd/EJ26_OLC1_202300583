package com.golite.runtime;
import com.golite.ast.*;

public class Interpreter implements Visitor<Object> {
    public Object evaluate(Expression expr) {
        return expr.accept(this);
    }

    // 1. ¿cómo evaluamos un Literal (un número suelto o texto)?
    @Override
    public Object visit(LiteralExpr expr) {
        return expr.value; // Solo devolvemos su valor crudo
    }

    // 2. ¿cómo evaluamos una operación Unaria (-x o !true)?
    @Override
    public Object visit(UnaryExpr expr) {
        Object right = evaluate(expr.right); // evaluamos lo que hay a la derecha

        switch (expr.operator) {
            case "-":
                if (right instanceof Integer) return -(int) right;
                if (right instanceof Double) return -(double) right;
                throw new RuntimeException("Error Semántico: El operador '-' requiere un número.");
            case "!":
                if (right instanceof Boolean) return !(boolean) right;
                throw new RuntimeException("Error Semántico: El operador '!' requiere un booleano.");
        }
        return null;
    }

    // 3. ¿cómo evaluamos una operación Binaria (a + b, x * y)?
    @Override
    public Object visit(BinaryExpr expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator) {
            case "+":
                // si ambos son enteros
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left + (int) right;
                }
                // si alguno es flotante, la suma se vuelve flotante
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
                // si alguno es String, concatenamos
                if (left instanceof String || right instanceof String) {
                    return left.toString() + right.toString();
                }
                throw new RuntimeException("Error Semántico: Operandos inválidos para suma.");
                
            case "-":
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left - (int) right;
                }
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() - ((Number) right).doubleValue();
                }
                throw new RuntimeException("Error Semántico: La resta requiere números.");
                
            // aquí agregaremos luego la multiplicación, división, ==, >, &&, etc.
        }
        return null;
    }
}

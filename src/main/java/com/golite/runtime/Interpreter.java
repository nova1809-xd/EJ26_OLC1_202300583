package com.golite.runtime;
import com.golite.ast.*;
import java.util.List;

public class Interpreter implements Visitor<Object> {

    // Tabla de simbolos. Por ahora un solo ambito global (Fase 1 sin scopes anidados).
    private final Environment environment = new Environment();

    // Acumula todo lo que el programa va "imprimiendo" via fmt.Println
    private final StringBuilder outputBuffer = new StringBuilder();

    // --- Punto de entrada para correr un programa completo ---
    public void interpret(Program program) {
        for (Statement stmt : program.statements) {
            execute(stmt);
        }
    }

    // Devuelve todo lo acumulado por fmt.Println durante la ejecucion
    public String getOutput() {
        return outputBuffer.toString();
    }

    public void execute(Statement stmt) {
        stmt.accept(this);
    }

    public Object evaluate(Expression expr) {
        return expr.accept(this);
    }

    // ==========================================
    // EXPRESIONES
    // ==========================================

    @Override
    public Object visit(LiteralExpr expr) {
        return expr.value;
    }

    @Override
    public Object visit(IdentifierExpr expr) {
        return environment.get(expr.name);
    }

    @Override
    public Object visit(UnaryExpr expr) {
        Object right = evaluate(expr.right);

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

    @Override
    public Object visit(BinaryExpr expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);
        return applyBinaryOp(expr.operator, left, right);
    }

    // Logica compartida entre BinaryExpr (a + b) y AssignStmt compuesto (a += b -> a = a + b)
    private Object applyBinaryOp(String operator, Object left, Object right) {
        switch (operator) {
            case "+":
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left + (int) right;
                }
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() + ((Number) right).doubleValue();
                }
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

            case "*":
                if (left instanceof Integer && right instanceof Integer) {
                    return (int) left * (int) right;
                }
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() * ((Number) right).doubleValue();
                }
                throw new RuntimeException("Error Semántico: La multiplicación requiere números.");

            case "/":
                if (left instanceof Integer && right instanceof Integer) {
                    if ((int) right == 0) throw new RuntimeException("Error Semántico: división entera por cero.");
                    return (int) left / (int) right; // division entera, trunca hacia cero como en Go
                }
                if (left instanceof Number && right instanceof Number) {
                    return ((Number) left).doubleValue() / ((Number) right).doubleValue();
                }
                throw new RuntimeException("Error Semántico: La división requiere números.");

            case "%":
                if (left instanceof Integer && right instanceof Integer) {
                    if ((int) right == 0) throw new RuntimeException("Error Semántico: módulo por cero.");
                    return (int) left % (int) right;
                }
                throw new RuntimeException("Error Semántico: El módulo requiere enteros.");

            case "==":
                return areEqual(left, right);

            case "!=":
                return !areEqual(left, right);

            case "<":
                return compareNumbers(left, right) < 0;

            case ">":
                return compareNumbers(left, right) > 0;

            case "<=":
                return compareNumbers(left, right) <= 0;

            case ">=":
                return compareNumbers(left, right) >= 0;

            case "&&":
                requireBoolean(left, "&&");
                requireBoolean(right, "&&");
                return (boolean) left && (boolean) right;

            case "||":
                requireBoolean(left, "||");
                requireBoolean(right, "||");
                return (boolean) left || (boolean) right;
        }
        return null;
    }

    // ==========================================
    // STATEMENTS
    // ==========================================

    @Override
    public Object visit(ExpressionStmt stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Object visit(VarDeclStmt stmt) {
        Object value;

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
            value = coerceToDeclaredType(value, stmt.type, stmt.name);
        } else {
            // valores por defecto (seccion 1d de tu archivo de prueba)
            value = defaultValueFor(stmt.type);
        }

        environment.define(stmt.name, value);
        return null;
    }

    @Override
    public Object visit(ShortVarDeclStmt stmt) {
        Object value = evaluate(stmt.initializer);
        environment.define(stmt.name, value);
        return null;
    }

    @Override
    public Object visit(PrintStmt stmt) {
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < stmt.arguments.size(); i++) {
            if (i > 0) line.append(" ");
            Object value = evaluate(stmt.arguments.get(i));
            line.append(formatValue(value));
        }

        outputBuffer.append(line).append("\n");
        return null;
    }

    @Override
    public Object visit(AssignStmt stmt) {
        Object newValue = evaluate(stmt.value);

        if (stmt.operator.equals("=")) {
            environment.assign(stmt.name, newValue);
            return null;
        }

        // Asignacion compuesta: x += 5  equivale a  x = x + 5
        Object current = environment.get(stmt.name);
        String binaryOp = stmt.operator.substring(0, 1); // "+=" -> "+"
        Object result = applyBinaryOp(binaryOp, current, newValue);
        environment.assign(stmt.name, result);
        return null;
    }

    @Override
    public Object visit(BlockStmt stmt) {
        for (Statement s : stmt.statements) {
            execute(s);
        }
        return null;
    }

    @Override
    public Object visit(IfStmt stmt) {
        Object condition = evaluate(stmt.condition);
        requireBoolean(condition, "if");

        if ((boolean) condition) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Object visit(ForStmt stmt) {
        if (stmt.init != null) {
            execute(stmt.init);
        }

        while (stmt.condition == null || isConditionTrue(stmt.condition)) {
            try {
                execute(stmt.body);
            } catch (BreakException b) {
                break;
            } catch (ContinueException c) {
                // sigue al "post" igual que un continue normal
            }

            if (stmt.post != null) {
                execute(stmt.post);
            }
        }
        return null;
    }

    @Override
    public Object visit(BreakStmt stmt) {
        throw new BreakException();
    }

    @Override
    public Object visit(ContinueStmt stmt) {
        throw new ContinueException();
    }

    // ==========================================
    // HELPERS
    // ==========================================

    // Formatea un valor igual que lo haria Go con fmt.Println
    private String formatValue(Object value) {
        if (value == null) return "nil";
        return String.valueOf(value);
    }

    // Igualdad: numeros se comparan por valor numerico (5 == 5.0 -> true),
    // strings/booleanos por equals normal.
    private boolean areEqual(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return ((Number) left).doubleValue() == ((Number) right).doubleValue();
        }
        if (left == null) return right == null;
        return left.equals(right);
    }

    // Compara dos numeros y devuelve negativo/cero/positivo, como Comparable.compareTo
    private int compareNumbers(Object left, Object right) {
        if (left instanceof Number && right instanceof Number) {
            return Double.compare(((Number) left).doubleValue(), ((Number) right).doubleValue());
        }
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        throw new RuntimeException("Error Semántico: la comparación requiere números o strings del mismo tipo.");
    }

    private void requireBoolean(Object value, String operator) {
        if (!(value instanceof Boolean)) {
            throw new RuntimeException("Error Semántico: el operador '" + operator + "' requiere operandos booleanos.");
        }
    }

    private boolean isConditionTrue(Expression condition) {
        Object value = evaluate(condition);
        requireBoolean(value, "for/if");
        return (boolean) value;
    }

    // Valor por defecto segun el tipo declarado explicitamente (var x int -> 0, etc.)
    private Object defaultValueFor(String type) {
        switch (type) {
            case "int":     return 0;
            case "float64": return 0.0;
            case "string":  return "";
            case "bool":    return false;
            case "rune":    return '\0';
            default:
                throw new RuntimeException("Error Semántico: tipo desconocido '" + type + "'.");
        }
    }

    // Conversion implicita int -> float64 cuando el tipo declarado es float64
    // (caso: var conv float64 = 5)
    private Object coerceToDeclaredType(Object value, String type, String varName) {
        if (type.equals("float64") && value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        // Aqui despues se puede agregar validacion estricta de tipos si se requiere
        return value;
    }
}
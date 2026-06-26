package com.golite.runtime;
import com.golite.ast.*;
import java.util.List;

public class Interpreter implements Visitor<Object> {

    // mi tabla de simbolos ahora cambia dinamicamente para soportar los ambitos
    private Environment environment = new Environment();

    // guardo todo lo que mi programa va imprimiendo con fmt.println
    private final StringBuilder outputBuffer = new StringBuilder();

    // aqui arranco mi programa completo recorriendo mis instrucciones
    public void interpret(Program program) {
        for (Statement stmt : program.statements) {
            execute(stmt);
        }
    }

    // devuelvo todo el texto acumulado de mi consola
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
    // expresiones
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
    public Object visit(TypeOfExpr expr) {
        Object value = evaluate(expr.argument);
        return goTypeNameOf(value);
    }

    @Override
    public Object visit(StrconvExpr expr) {
        Object value = evaluate(expr.argument);
        if (!(value instanceof String)) {
            throw new RuntimeException("Error Semántico: strconv requiere un argumento de tipo string.");
        }
        String text = (String) value;

        try {
            if (expr.kind == StrconvExpr.Kind.ATOI) {
                return Integer.parseInt(text.trim());
            } else { // PARSE_FLOAT
                return Double.parseDouble(text.trim());
            }
        } catch (NumberFormatException e) {
            String fn = expr.kind == StrconvExpr.Kind.ATOI ? "strconv.Atoi" : "strconv.ParseFloat";
            throw new RuntimeException("Error Semántico: '" + text + "' no se pudo convertir con " + fn + ".");
        }
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

    // aplico la operacion binaria para sumas restas o asignaciones compuestas
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
                    return (int) left / (int) right; // hago la division entera truncando hacia cero como pide go
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
    // statements
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
            // coloco los valores por defecto si no inicializan la variable
            value = defaultValueFor(stmt.type);
        }

        // guardo la nueva variable en mi ambito actual
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

        // resuelvo asignaciones compuestas convirtiendolas a operacion normal
        Object current = environment.get(stmt.name);
        String binaryOp = stmt.operator.substring(0, 1); // quito el igual para quedarme solo con el operador
        Object result = applyBinaryOp(binaryOp, current, newValue);
        environment.assign(stmt.name, result);
        return null;
    }

    @Override
    public Object visit(BlockStmt stmt) {
        // resuelvo los bloques creando un nuevo entorno hijo seguro
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    
    // helper mio para cambiar de entorno de forma segura y restaurarlo al salir
    private void executeBlock(List<Statement> statements, Environment innerEnv) {
        Environment previous = this.environment; // guardo mi ambito padre actual
        try {
            this.environment = innerEnv;         // me muevo a mi nuevo ambito hijo
            for (Statement statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;         // regreso a mi ambito padre pase lo que pase al terminar
        }
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
        // meto el for en su propio entorno para que sus variables mueran al terminar
        Environment previous = this.environment;
        try {
            this.environment = new Environment(environment);
            
            if (stmt.init != null) {
                execute(stmt.init);
            }
    
            while (stmt.condition == null || isConditionTrue(stmt.condition)) {
                try {
                    execute(stmt.body); // mi cuerpo ya es un bloque asi que el solito maneja su subambito
                } catch (BreakException b) {
                    break;
                } catch (ContinueException c) {
                    // sigo directo al post con el catch del continue
                }
    
                if (stmt.post != null) {
                    execute(stmt.post);
                }
            }
        } finally {
            this.environment = previous; // restauro mi entorno anterior
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
    // helpers internos
    // ==========================================

    // le doy formato de texto a mis valores como lo hace go
    // (rune es alias de int32 en go real: se imprime como codigo numerico, no como caracter)
    private String formatValue(Object value) {
        if (value == null) return "nil";
        if (value instanceof Character) return String.valueOf((int) (char) value);
        return String.valueOf(value);
    }

    // comparo si mis objetos son iguales manejando enteros, decimales y runes por valor numerico
    private boolean areEqual(Object left, Object right) {
        if (isNumericLike(left) && isNumericLike(right)) {
            return toDouble(left) == toDouble(right);
        }
        if (left == null) return right == null;
        return left.equals(right);
    }

    // comparo mis numeros, runes o textos para operadores relacionales
    private int compareNumbers(Object left, Object right) {
        if (isNumericLike(left) && isNumericLike(right)) {
            return Double.compare(toDouble(left), toDouble(right));
        }
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        throw new RuntimeException("Error Semántico: la comparación requiere números o strings del mismo tipo.");
    }

    // un valor "numerico" para estos propositos es Number o Character (rune)
    private boolean isNumericLike(Object value) {
        return value instanceof Number || value instanceof Character;
    }

    // mapeo el tipo real en Java al nombre de tipo que usaria reflect.TypeOf en Go
    private String goTypeNameOf(Object value) {
        if (value instanceof Integer)   return "int";
        if (value instanceof Double)    return "float64";
        if (value instanceof String)    return "string";
        if (value instanceof Boolean)   return "bool";
        if (value instanceof Character) return "rune";
        if (value == null)              return "nil";
        throw new RuntimeException("Error Semántico: tipo desconocido para reflect.TypeOf.");
    }

    // convierto Number o Character a double para comparaciones uniformes
    private double toDouble(Object value) {
        if (value instanceof Character) return (double) (char) value;
        return ((Number) value).doubleValue();
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

    // busco el valor por defecto de mis tipos basicos
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

    // convierto mis enteros a decimales si el tipo destino lo pide
    private Object coerceToDeclaredType(Object value, String type, String varName) {
        if (type.equals("float64") && value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }
        return value;
    }
}
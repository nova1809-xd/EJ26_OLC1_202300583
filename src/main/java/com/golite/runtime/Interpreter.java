package com.golite.runtime;
import com.golite.ast.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Visitor<Object> {

    // mi tabla de simbolos ahora cambia dinamicamente para soportar los ambitos
    private Environment environment = new Environment();

    // guardo la referencia al ambiente GLOBAL (no cambia nunca). Las funciones
    // se ejecutan siempre a partir de este ambiente como padre, no del ambiente
    // de quien hizo la llamada, para evitar closures accidentales.
    private final Environment globalEnvironment = environment;

    // mi tabla de funciones declaradas a nivel global: nombre -> funcion
    private final Map<String, GoLiteFunction> functions = new HashMap<>();

    // guardo todo lo que mi programa va imprimiendo con fmt.println
    private final StringBuilder outputBuffer = new StringBuilder();

    // mi recolector de errores para mandar los fallos semanticos a la gui
    private final com.golite.reports.ReportCollector collector;

    // mi nuevo constructor que recibe el recolector
    public Interpreter(com.golite.reports.ReportCollector collector) {
        this.collector = collector;
    }

    // ==========================================
    // punto de entrada: ahora son DOS FASES
    // ==========================================

    // Fase 2: el programa ya no se ejecuta secuencialmente desde la linea 1.
    // Primero registro todas las declaraciones globales (variables y funciones),
    // y luego busco y ejecuto main().
    public void interpret(Program program) {
        // --- FASE 1: registrar declaraciones globales ---
        for (Statement decl : program.statements) {
            try {
                registerGlobalDeclaration(decl);
            } catch (RuntimeException e) {
                reportarError(e.getMessage());
            }
        }

        // --- FASE 2: buscar y ejecutar main() ---
        GoLiteFunction main = functions.get("main");
        if (main == null) {
            reportarError("no se encontró la función 'main'. GoLite requiere un punto de entrada main().");
            return;
        }

        try {
            callFunction(main, List.of());
        } catch (BreakException b) {
            reportarError("la instruccion 'break' solo se puede usar dentro de un ciclo for.");
        } catch (ContinueException c) {
            reportarError("la instruccion 'continue' solo se puede usar dentro de un ciclo for.");
        } catch (ReturnException r) {
            // un "return" dentro de main simplemente termina la ejecucion, no es error
        } catch (RuntimeException e) {
            reportarError(e.getMessage());
        }
    }

    // Registra una declaracion de nivel superior: variable global o funcion.
    // Las variables globales SI se evaluan y guardan de inmediato (pueden
    // usarse como constantes/config leida por cualquier funcion). Las
    // funciones solo se registran en la tabla, su cuerpo no se ejecuta aun.
    private void registerGlobalDeclaration(Statement decl) {
        if (decl instanceof FunctionDecl fn) {
            if (functions.containsKey(fn.name)) {
                throw new RuntimeException("Error Semántico: la función '" + fn.name + "' ya fue declarada.");
            }
            functions.put(fn.name, new GoLiteFunction(fn));
        } else {
            // cualquier otra cosa a nivel global (por ahora, VarDeclStmt) se ejecuta normal
            execute(decl);
        }
    }

    // mi helper para no repetir el guardado de errores
    private void reportarError(String mensaje) {
        if (collector != null) {
            collector.addError(com.golite.reports.ErrorType.SEMANTIC, 0, 0, mensaje);
        } else {
            System.err.println(mensaje);
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
    // invocacion de funciones definidas por el usuario
    // ==========================================

    private Object callFunction(GoLiteFunction function, List<Object> args) {
        FunctionDecl decl = function.declaration;

        if (args.size() != decl.params.size()) {
            throw new RuntimeException(
                "Error Semántico: la función '" + decl.name + "' espera " + decl.params.size() +
                " argumento(s), pero se recibieron " + args.size() + "."
            );
        }

        // Las funciones en GoLite NO tienen closure sobre el ambito del
        // llamador: su ambiente padre es siempre el global. Esto permite
        // recursion limpia (cada llamada tiene su propio Environment hijo
        // del global, no anidado sobre la llamada anterior) sin arrastrar
        // variables locales de quien invoco la funcion.
        Environment functionEnv = new Environment(globalEnvironment);

        // Paso de parametros por valor: cada parametro se valida contra su
        // tipo declarado y se define como variable nueva en el ambiente de
        // la funcion, igual que si fuera "var nombre tipo = argumento".
        for (int i = 0; i < decl.params.size(); i++) {
            Param param = decl.params.get(i);
            Object argValue = args.get(i);
            Object coerced = coerceToDeclaredType(argValue, param.type, param.name);
            functionEnv.define(param.name, coerced);
        }

        Environment previous = this.environment;
        try {
            this.environment = functionEnv;
            execute(decl.body);

            // si el cuerpo termino sin hacer return explicito:
            if (decl.returnType != null) {
                throw new RuntimeException(
                    "Error Semántico: la función '" + decl.name + "' debe retornar un valor de tipo " + decl.returnType + "."
                );
            }
            return null;

        } catch (ReturnException r) {
            // el return interrumpio la ejecucion del cuerpo: validamos su tipo
            if (decl.returnType == null) {
                if (r.value != null) {
                    throw new RuntimeException(
                        "Error Semántico: la función '" + decl.name + "' no declara tipo de retorno, no puede retornar un valor."
                    );
                }
                return null;
            }
            if (r.value == null) {
                throw new RuntimeException(
                    "Error Semántico: la función '" + decl.name + "' debe retornar un valor de tipo " + decl.returnType + "."
                );
            }
            return coerceToDeclaredType(r.value, decl.returnType, decl.name);

        } finally {
            this.environment = previous;
        }
    }

    @Override
    public Object visit(CallExpr expr) {
        GoLiteFunction function = functions.get(expr.name);
        if (function == null) {
            throw new RuntimeException("Error Semántico: la función '" + expr.name + "' no ha sido declarada.");
        }

        List<Object> args = new java.util.ArrayList<>();
        for (Expression argExpr : expr.arguments) {
            args.add(evaluate(argExpr));
        }

        return callFunction(function, args);
    }

    @Override
    public Object visit(FunctionDecl stmt) {
        // las declaraciones de funcion solo se procesan en registerGlobalDeclaration().
        // si por alguna razon se intenta "ejecutar" como statement normal, no hago nada.
        return null;
    }

    @Override
    public Object visit(ReturnStmt stmt) {
        Object value = (stmt.value != null) ? evaluate(stmt.value) : null;
        throw new ReturnException(value);
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
                throw new RuntimeException(
                    "Error Semántico: operación inválida " + goTypeNameOf(left) + " - " + goTypeNameOf(right) + "."
                );

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
                if (left instanceof Double || right instanceof Double) {
                    String badType = left instanceof Double ? goTypeNameOf(left) : goTypeNameOf(right);
                    throw new RuntimeException("Error Semántico: el operador % no es válido para " + badType + ".");
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

        // 1. PRIMERO evaluamos el lado derecho (esto disparará el Error 6 de la resta ilegal en la Fase 1)
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
            value = coerceToDeclaredType(value, stmt.type, stmt.name);
        } else {
            // coloco los valores por defecto si no inicializan la variable
            value = defaultValueFor(stmt.type);
        }

        // 2. DESPUÉS validamos si el nombre ya existía localmente (el Error 17)
        if (environment.isDefinedLocally(stmt.name)) {
            throw new RuntimeException("Error Semántico: la variable '" + stmt.name + "' ya fue declarada en este ámbito.");
        }

        // 3. Finalmente guardamos la nueva variable en mi ambito actual
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
            // ERROR SEMANTICO 3: el tipo de una variable es fijo desde su
            // declaracion (tipado estatico). Reasignar un tipo distinto,
            // aunque sea con "=", es un error.
            Object current = environment.get(stmt.name); // tambien valida que ya exista
            Object coerced = coerceToExistingType(newValue, current, stmt.name);
            environment.assign(stmt.name, coerced);
            return null;
        }

        // resuelvo asignaciones compuestas convirtiendolas a operacion normal
        Object current = environment.get(stmt.name);
        String binaryOp = stmt.operator.substring(0, 1); // quito el igual para quedarme solo con el operador
        Object result = applyBinaryOp(binaryOp, current, newValue);

        // ERROR SEMANTICO 15: el resultado de la operacion compuesta tambien
        // debe respetar el tipo original de la variable (intVar += 3.14 no es
        // valido si intVar es int, aunque la suma en si misma de numeros sea
        // matematicamente posible).
        Object coerced = coerceToExistingType(result, current, stmt.name);
        environment.assign(stmt.name, coerced);
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

    // comparo si mis objetos son iguales manejando enteros, decimales y runes por valor numerico.
    // ERROR SEMANTICO 8: comparar tipos incompatibles (ej. string == int) es un
    // error semantico, no debe devolver silenciosamente false.
    private boolean areEqual(Object left, Object right) {
        if (isNumericLike(left) && isNumericLike(right)) {
            return toDouble(left) == toDouble(right);
        }
        if (left == null || right == null) {
            return left == right;
        }
        if (left.getClass() != right.getClass()) {
            throw new RuntimeException(
                "Error Semántico: tipos incompatibles en comparación: " + goTypeNameOf(left) + " == " + goTypeNameOf(right) + "."
            );
        }
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

    // valido que un nuevo valor (de una asignacion = o compuesta) coincida con
    // el tipo que la variable ya tenia. GoLite es de tipado estatico: una vez
    // declarada como int, siempre debe seguir siendo int.
    private Object coerceToExistingType(Object newValue, Object currentValue, String varName) {
        String currentType = goTypeNameOf(currentValue);
        String newType = goTypeNameOf(newValue);

        if (currentType.equals(newType)) {
            return newValue;
        }

        // unica conversion implicita permitida: int -> float64
        if (currentType.equals("float64") && newValue instanceof Integer) {
            return ((Integer) newValue).doubleValue();
        }

        throw new RuntimeException(
            "Error Semántico: no se puede asignar " + newType + " a variable de tipo " + currentType + " ('" + varName + "')."
        );
    }

    // valido que el valor inicial (o argumento de funcion, o valor de return)
    // coincida con el tipo declarado explicitamente.
    // Unica conversion implicita permitida: int -> float64 (igual que en Go real).
    // Cualquier otra discrepancia es un error semantico.
    private Object coerceToDeclaredType(Object value, String type, String varName) {
        String actualType = goTypeNameOf(value);

        if (type.equals(actualType)) {
            return value; // coincide exacto, sin problema
        }

        // unica conversion implicita permitida: int asignado a float64
        if (type.equals("float64") && value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        throw new RuntimeException(
            "Error Semántico: no se puede asignar " + actualType + " a variable de tipo " + type + " ('" + varName + "')."
        );
    }
}
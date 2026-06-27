package com.golite.runtime;
import com.golite.ast.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class Interpreter implements Visitor<Object> {

    // mi tabla de simbolos ahora cambia dinamicamente para soportar los ambitos
    private Environment environment = new Environment();

    // guardo la referencia al ambiente GLOBAL (no cambia nunca). Las funciones
    // se ejecutan siempre a partir de este ambiente como padre, no del ambiente
    // de quien hizo la llamada, para evitar closures accidentales.
    private final Environment globalEnvironment = environment;

    // mi tabla de funciones declaradas a nivel global: nombre -> funcion
    private final Map<String, GoLiteFunction> functions = new HashMap<>();

    // mi tabla de structs declarados: nombre -> definicion (campos esperados)
    private final Map<String, StructType> structTypes = new HashMap<>();

    // mi tabla de metodos: la llave combina el tipo receptor + nombre del metodo,
    // para poder tener "Producto.actualizar" sin chocar con otros structs
    private final Map<String, GoLiteMethod> methods = new HashMap<>();

    // guardo todo lo que mi programa va imprimiendo con fmt.println
    private final StringBuilder outputBuffer = new StringBuilder();

    // mi recolector de errores para mandar los fallos semanticos a la gui
    private final com.golite.reports.ReportCollector collector;

    // mi nuevo constructor que recibe el recolector
    public Interpreter(com.golite.reports.ReportCollector collector) {
        this.collector = collector;
    }

    // ==========================================
    // punto de entrada: dos fases
    // ==========================================

    public void interpret(Program program) {
        // --- FASE 1: registrar declaraciones globales (vars, funcs, structs, metodos) ---
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

    // Registra una declaracion de nivel superior: variable global, funcion,
    // struct, o metodo. Las variables globales SI se evaluan de inmediato.
    // Funciones, structs y metodos solo se registran, su cuerpo no se
    // ejecuta hasta que sean invocados/instanciados.
    private void registerGlobalDeclaration(Statement decl) {
        if (decl instanceof FunctionDecl fn) {
            if (functions.containsKey(fn.name)) {
                throw new RuntimeException("Error Semántico: la función '" + fn.name + "' ya fue declarada.");
            }
            functions.put(fn.name, new GoLiteFunction(fn));

        } else if (decl instanceof StructDecl sd) {
            if (structTypes.containsKey(sd.name)) {
                throw new RuntimeException("Error Semántico: el struct '" + sd.name + "' ya fue declarado.");
            }
            structTypes.put(sd.name, new StructType(sd.name, sd.fields));

        } else if (decl instanceof MethodDecl md) {
            if (!structTypes.containsKey(md.receiverType)) {
                throw new RuntimeException(
                    "Error Semántico: el método '" + md.name + "' hace referencia al struct '" +
                    md.receiverType + "', que no ha sido declarado."
                );
            }
            String key = methodKey(md.receiverType, md.name);
            if (methods.containsKey(key)) {
                throw new RuntimeException(
                    "Error Semántico: el struct '" + md.receiverType + "' ya tiene un método llamado '" + md.name + "'."
                );
            }
            methods.put(key, new GoLiteMethod(md));

        } else {
            // cualquier otra cosa a nivel global (VarDeclStmt) se ejecuta normal
            execute(decl);
        }
    }

    private String methodKey(String structName, String methodName) {
        return structName + "." + methodName;
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
        // recursion limpia sin arrastrar variables locales de quien invoco.
        Environment functionEnv = new Environment(globalEnvironment);

        // Paso de parametros: primitivos por valor, structs/slices por
        // referencia (en Java esto sale natural porque GoLiteStruct y
        // GoLiteSlice son objetos mutables compartidos, no se clonan aqui).
        for (int i = 0; i < decl.params.size(); i++) {
            Param param = decl.params.get(i);
            Object argValue = args.get(i);
            Object coerced = coerceToDeclaredType(argValue, param.type, param.name);
            functionEnv.define(param.name, coerced);
        }

        return runFunctionBody(decl.name, decl.body, decl.returnType, functionEnv);
    }

    // Invoca un metodo: ademas de los parametros normales, define el
    // identificador del receptor (ej. "p") apuntando a la MISMA instancia
    // de GoLiteStruct que se le paso, permitiendo que el metodo mute sus
    // campos y el cambio se vea reflejado afuera (paso por referencia).
    private Object callMethod(GoLiteMethod method, GoLiteStruct receiverInstance, List<Object> args) {
        MethodDecl decl = method.declaration;

        if (args.size() != decl.params.size()) {
            throw new RuntimeException(
                "Error Semántico: el método '" + decl.name + "' espera " + decl.params.size() +
                " argumento(s), pero se recibieron " + args.size() + "."
            );
        }

        Environment methodEnv = new Environment(globalEnvironment);
        methodEnv.define(decl.receiverName, receiverInstance);

        for (int i = 0; i < decl.params.size(); i++) {
            Param param = decl.params.get(i);
            Object argValue = args.get(i);
            Object coerced = coerceToDeclaredType(argValue, param.type, param.name);
            methodEnv.define(param.name, coerced);
        }

        return runFunctionBody(decl.name, decl.body, decl.returnType, methodEnv);
    }

    // Logica compartida entre callFunction y callMethod: ejecuta el cuerpo
    // en el ambiente dado, atrapa el ReturnException, y valida el tipo del
    // valor retornado contra el tipo de retorno declarado.
    private Object runFunctionBody(String name, BlockStmt body, String returnType, Environment callEnv) {
        Environment previous = this.environment;
        try {
            this.environment = callEnv;
            execute(body);

            if (returnType != null) {
                throw new RuntimeException(
                    "Error Semántico: la función '" + name + "' debe retornar un valor de tipo " + returnType + "."
                );
            }
            return null;

        } catch (ReturnException r) {
            if (returnType == null) {
                if (r.value != null) {
                    throw new RuntimeException(
                        "Error Semántico: la función '" + name + "' no declara tipo de retorno, no puede retornar un valor."
                    );
                }
                return null;
            }
            if (r.value == null) {
                throw new RuntimeException(
                    "Error Semántico: la función '" + name + "' debe retornar un valor de tipo " + returnType + "."
                );
            }
            return coerceToDeclaredType(r.value, returnType, name);

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

        List<Object> args = new ArrayList<>();
        for (Expression argExpr : expr.arguments) {
            args.add(evaluate(argExpr));
        }

        return callFunction(function, args);
    }

    @Override
    public Object visit(FunctionDecl stmt) {
        // las declaraciones de funcion solo se procesan en registerGlobalDeclaration().
        return null;
    }

    @Override
    public Object visit(MethodDecl stmt) {
        // igual que FunctionDecl: solo se procesa en registerGlobalDeclaration().
        return null;
    }

    @Override
    public Object visit(ReturnStmt stmt) {
        Object value = (stmt.value != null) ? evaluate(stmt.value) : null;
        throw new ReturnException(value);
    }

    // ==========================================
    // structs
    // ==========================================

    @Override
    public Object visit(StructDecl stmt) {
        // ya se registro en registerGlobalDeclaration(). No hay nada que
        // "ejecutar" aqui (los structs solo viven en ambito global).
        return null;
    }

    @Override
    public Object visit(StructInstanceStmt stmt) {
        StructType structType = structTypes.get(stmt.structType);
        if (structType == null) {
            throw new RuntimeException("Error Semántico: el tipo '" + stmt.structType + "' no es un struct declarado.");
        }

        if (environment.isDefinedLocally(stmt.varName)) {
            throw new RuntimeException("Error Semántico: la variable '" + stmt.varName + "' ya fue declarada en este ámbito.");
        }

        GoLiteStruct instance = new GoLiteStruct(stmt.structType);

        // primero coloco los valores por defecto de TODOS los campos
        // (asi un campo omitido en la instanciacion queda con su default)
        for (Param field : structType.fields) {
            instance.setField(field.name, defaultValueFor(field.type));
        }

        // luego sobreescribo con los valores explicitos de la instanciacion
        for (FieldInit init : stmt.fieldInits) {
            if (!structType.hasField(init.fieldName)) {
                throw new RuntimeException(
                    "Error Semántico: el struct '" + stmt.structType + "' no tiene el campo '" + init.fieldName + "'."
                );
            }
            Object value = evaluate(init.value);
            String expectedType = structType.fieldType(init.fieldName);
            Object coerced = coerceToDeclaredType(value, expectedType, init.fieldName);
            instance.setField(init.fieldName, coerced);
        }

        environment.define(stmt.varName, instance);
        return null;
    }

    @Override
    public Object visit(FieldAccessExpr expr) {
        Object targetValue = evaluate(expr.target);
        if (!(targetValue instanceof GoLiteStruct struct)) {
            throw new RuntimeException("Error Semántico: solo se puede acceder a atributos de una instancia de struct.");
        }
        if (!struct.hasField(expr.fieldName)) {
            throw new RuntimeException(
                "Error Semántico: el struct '" + struct.typeName + "' no tiene el campo '" + expr.fieldName + "'."
            );
        }
        return struct.getField(expr.fieldName);
    }

    @Override
    public Object visit(FieldAssignStmt stmt) {
        Object targetValue = evaluate(stmt.target);
        if (!(targetValue instanceof GoLiteStruct struct)) {
            throw new RuntimeException("Error Semántico: solo se puede asignar a atributos de una instancia de struct.");
        }
        if (!struct.hasField(stmt.fieldName)) {
            throw new RuntimeException(
                "Error Semántico: el struct '" + struct.typeName + "' no tiene el campo '" + stmt.fieldName + "'."
            );
        }

        StructType structType = structTypes.get(struct.typeName);
        Object newValue = evaluate(stmt.value);
        String expectedType = structType.fieldType(stmt.fieldName);
        Object coerced = coerceToDeclaredType(newValue, expectedType, stmt.fieldName);

        struct.setField(stmt.fieldName, coerced);
        return null;
    }

    // ==========================================
    // slices
    // ==========================================

    @Override
    public Object visit(SliceLiteralExpr expr) {
        List<Object> values = new ArrayList<>();
        for (Expression elemExpr : expr.elements) {
            Object value = evaluate(elemExpr);
            Object coerced = coerceToDeclaredType(value, expr.elementType, "elemento de slice");
            values.add(coerced);
        }
        return new GoLiteSlice(expr.elementType, values);
    }

    @Override
    public Object visit(IndexExpr expr) {
        Object targetValue = evaluate(expr.target);
        if (!(targetValue instanceof GoLiteSlice slice)) {
            throw new RuntimeException("Error Semántico: el operador de índice [] solo aplica a slices.");
        }
        Object indexValue = evaluate(expr.index);
        int index = requireInt(indexValue, "el índice de un slice");
        return slice.get(index);
    }

    @Override
    public Object visit(IndexAssignStmt stmt) {
        Object targetValue = evaluate(stmt.target);
        if (!(targetValue instanceof GoLiteSlice slice)) {
            throw new RuntimeException("Error Semántico: el operador de índice [] solo aplica a slices.");
        }
        Object indexValue = evaluate(stmt.index);
        int index = requireInt(indexValue, "el índice de un slice");

        Object newValue = evaluate(stmt.value);
        Object coerced = coerceToDeclaredType(newValue, slice.elementType, "elemento de slice");
        slice.set(index, coerced);
        return null;
    }

    @Override
    public Object visit(AppendExpr expr) {
        Object sliceValue = evaluate(expr.slice);
        if (!(sliceValue instanceof GoLiteSlice slice)) {
            throw new RuntimeException("Error Semántico: append() solo aplica a slices.");
        }
        Object newElement = evaluate(expr.value);
        Object coerced = coerceToDeclaredType(newElement, slice.elementType, "elemento de slice");
        return slice.appended(coerced);
    }

    @Override
    public Object visit(LenExpr expr) {
        Object value = evaluate(expr.argument);
        if (value instanceof GoLiteSlice slice) {
            return slice.size();
        }
        if (value instanceof String str) {
            return str.length();
        }
        throw new RuntimeException("Error Semántico: len() solo aplica a slices o strings.");
    }

    private int requireInt(Object value, String context) {
        if (!(value instanceof Integer)) {
            throw new RuntimeException("Error Semántico: " + context + " debe ser de tipo int.");
        }
        return (int) value;
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
                    return (int) left / (int) right;
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

        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
            value = coerceToDeclaredType(value, stmt.type, stmt.name);
        } else {
            value = defaultValueFor(stmt.type);
        }

        if (environment.isDefinedLocally(stmt.name)) {
            throw new RuntimeException("Error Semántico: la variable '" + stmt.name + "' ya fue declarada en este ámbito.");
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
            Object current = environment.get(stmt.name);
            Object coerced = coerceToExistingType(newValue, current, stmt.name);
            environment.assign(stmt.name, coerced);
            return null;
        }

        Object current = environment.get(stmt.name);
        String binaryOp = stmt.operator.substring(0, 1);
        Object result = applyBinaryOp(binaryOp, current, newValue);

        Object coerced = coerceToExistingType(result, current, stmt.name);
        environment.assign(stmt.name, coerced);
        return null;
    }

    @Override
    public Object visit(BlockStmt stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    
    private void executeBlock(List<Statement> statements, Environment innerEnv) {
        Environment previous = this.environment;
        try {
            this.environment = innerEnv;
            for (Statement statement : statements) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
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
        Environment previous = this.environment;
        try {
            this.environment = new Environment(environment);
            
            if (stmt.init != null) {
                execute(stmt.init);
            }
    
            while (stmt.condition == null || isConditionTrue(stmt.condition)) {
                try {
                    execute(stmt.body);
                } catch (BreakException b) {
                    break;
                } catch (ContinueException c) {
                    // sigo directo al post
                }
    
                if (stmt.post != null) {
                    execute(stmt.post);
                }
            }
        } finally {
            this.environment = previous;
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

    private String formatValue(Object value) {
        if (value == null) return "nil";
        if (value instanceof Character) return String.valueOf((int) (char) value);
        return String.valueOf(value);
    }

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

    private int compareNumbers(Object left, Object right) {
        if (isNumericLike(left) && isNumericLike(right)) {
            return Double.compare(toDouble(left), toDouble(right));
        }
        if (left instanceof String && right instanceof String) {
            return ((String) left).compareTo((String) right);
        }
        throw new RuntimeException("Error Semántico: la comparación requiere números o strings del mismo tipo.");
    }

    private boolean isNumericLike(Object value) {
        return value instanceof Number || value instanceof Character;
    }

    // mapeo el tipo real en Java al nombre de tipo que usaria reflect.TypeOf en Go.
    // Ahora tambien reconoce structs y slices.
    private String goTypeNameOf(Object value) {
        if (value instanceof Integer)   return "int";
        if (value instanceof Double)    return "float64";
        if (value instanceof String)    return "string";
        if (value instanceof Boolean)   return "bool";
        if (value instanceof Character) return "rune";
        if (value instanceof GoLiteStruct s) return s.typeName;
        if (value instanceof GoLiteSlice sl) return "[]" + sl.elementType;
        if (value == null)              return "nil";
        throw new RuntimeException("Error Semántico: tipo desconocido para reflect.TypeOf.");
    }

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

    // busco el valor por defecto de mis tipos basicos, slices, y structs.
    private Object defaultValueFor(String type) {
        if (type.startsWith("[]")) {
            return GoLiteSlice.empty(type.substring(2));
        }
        if (structTypes.containsKey(type)) {
            // un struct sin inicializar explicito: instancia con todos sus
            // campos en su valor por defecto
            StructType st = structTypes.get(type);
            GoLiteStruct instance = new GoLiteStruct(type);
            for (Param field : st.fields) {
                instance.setField(field.name, defaultValueFor(field.type));
            }
            return instance;
        }
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
    // el tipo que la variable ya tenia.
    private Object coerceToExistingType(Object newValue, Object currentValue, String varName) {
        String currentType = goTypeNameOf(currentValue);
        String newType = goTypeNameOf(newValue);

        if (currentType.equals(newType)) {
            return newValue;
        }

        if (currentType.equals("float64") && newValue instanceof Integer) {
            return ((Integer) newValue).doubleValue();
        }

        throw new RuntimeException(
            "Error Semántico: no se puede asignar " + newType + " a variable de tipo " + currentType + " ('" + varName + "')."
        );
    }

    // valido que el valor inicial (o argumento de funcion, valor de return,
    // elemento de slice, o campo de struct) coincida con el tipo declarado.
    private Object coerceToDeclaredType(Object value, String type, String varName) {
        // Para structs y slices no exijo coincidencia estricta de instancia
        // de Java, solo que el tipo declarado en runtime coincida (ya que
        // GoLiteStruct/GoLiteSlice son siempre instancias unicas pasadas
        // por referencia, no necesitan "convertirse").
        String actualType = goTypeNameOf(value);

        if (type.equals(actualType)) {
            return value;
        }

        if (type.equals("float64") && value instanceof Integer) {
            return ((Integer) value).doubleValue();
        }

        throw new RuntimeException(
            "Error Semántico: no se puede asignar " + actualType + " a variable de tipo " + type + " ('" + varName + "')."
        );
    }
}
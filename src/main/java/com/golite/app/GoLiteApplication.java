package com.golite.app;

import com.golite.ast.Program;
import com.golite.lexer.GoLiteLexer;
import com.golite.lexer.Token;
import com.golite.parser.GoLiteParser;
import com.golite.reports.ErrorRecord;
import com.golite.reports.ReportCollector;
import com.golite.runtime.Interpreter;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** coordina el analisis y la ejecucion del archivo fuente. */
public final class GoLiteApplication {

    /** resultado final de una ejecucion. */
    public record RunResult(List<Token> tokens, List<ErrorRecord> errors, String output) {
    }

    public RunResult run(Path sourcePath) {
        String source;
        ReportCollector collector = new ReportCollector();
        
        try {
            source = Files.readString(sourcePath);
        } catch (IOException exception) {
            collector.addError(com.golite.reports.ErrorType.SEMANTIC, 0, 0, "no se pudo leer el archivo: " + exception.getMessage());
            return new RunResult(List.of(), collector.errors(), "");
        }

        String output = "";
        List<Token> dummyTokens = new ArrayList<>(); 

        try {
            GoLiteLexer lexer = new GoLiteLexer(new StringReader(source), collector);

            GoLiteParser parser = new GoLiteParser(lexer, collector);

            Object parseResult = parser.parse().value;

            if (parseResult instanceof Program astRoot) {
                Interpreter interpreter = new Interpreter(collector);
                interpreter.interpret(astRoot);
                output = interpreter.getOutput();
            } else {
                collector.addError(
                    com.golite.reports.ErrorType.SYNTACTIC,
                    0, 0,
                    "El análisis sintáctico no pudo completarse debido a errores graves. " +
                    "La ejecución fue detenida; revisa los errores reportados arriba."
                );
            }

        } catch (Exception e) {
            collector.addError(com.golite.reports.ErrorType.SEMANTIC, 0, 0, "Error crítico en el motor: " + e.getMessage());
        }

        return new RunResult(dummyTokens, collector.errors(), output);
    }
}
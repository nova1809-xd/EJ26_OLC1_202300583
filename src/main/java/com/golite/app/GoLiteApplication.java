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
        // por ahora pasamos una lista vacía a la tabla de la GUI
        // más adelante conectaremos JFlex para que llene esta lista
        List<Token> dummyTokens = new ArrayList<>(); 

        try {
            GoLiteLexer lexer = new GoLiteLexer(new StringReader(source));

            GoLiteParser parser = new GoLiteParser(lexer);

            // Antes: Expression astRoot = (Expression) parser.parse().value;
            // Ahora: el simbolo inicial de la gramatica es "program", no "expr"
            Program astRoot = (Program) parser.parse().value;

            Interpreter interpreter = new Interpreter();
            interpreter.interpret(astRoot);

            // La salida real ahora viene de fmt.Println, acumulada en el interpreter
            output = interpreter.getOutput();

        } catch (Exception e) {
            collector.addError(com.golite.reports.ErrorType.SEMANTIC, 0, 0, "Error crítico en el motor: " + e.getMessage());
        }

        return new RunResult(dummyTokens, collector.errors(), output);
    }
}
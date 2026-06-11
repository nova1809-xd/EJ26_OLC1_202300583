package com.golite.app;

import com.golite.ast.Program;
import com.golite.lexer.Lexer;
import com.golite.lexer.Token;
import com.golite.parser.Parser;
import com.golite.reports.ErrorRecord;
import com.golite.reports.ReportCollector;
import com.golite.runtime.Interpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** coordina el analisis y la ejecucion del archivo fuente. */
public final class GoLiteApplication {

    /** resultado final de una ejecucion. */
    public record RunResult(List<Token> tokens, List<ErrorRecord> errors, String output) {
    }

    public RunResult run(Path sourcePath) {
        String source;
        try {
            source = Files.readString(sourcePath);
        } catch (IOException exception) {
            ReportCollector collector = new ReportCollector();
            collector.addError(com.golite.reports.ErrorType.SEMANTIC, 0, 0, "no se pudo leer el archivo: " + exception.getMessage());
            return new RunResult(List.of(), collector.errors(), "");
        }

        ReportCollector collector = new ReportCollector();
        Lexer lexer = new Lexer(source, collector);
        List<Token> tokens = lexer.scanTokens();

        if (collector.hasErrors()) {
            return new RunResult(tokens, collector.errors(), "");
        }

        Parser parser = new Parser(tokens, collector);
        Program program = parser.parseProgram();
        if (collector.hasErrors()) {
            return new RunResult(tokens, collector.errors(), "");
        }

        Interpreter interpreter = new Interpreter(collector);
        String output = interpreter.execute(program);
        return new RunResult(tokens, collector.errors(), output);
    }
}
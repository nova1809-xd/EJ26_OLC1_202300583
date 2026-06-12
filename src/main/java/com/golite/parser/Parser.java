package com.golite.parser;

import com.golite.ast.*;
import com.golite.lexer.Token;
import com.golite.lexer.TokenType;
import com.golite.reports.ErrorType;
import com.golite.reports.ReportCollector;
import com.golite.runtime.Value;
import com.golite.runtime.ValueType;

import java.util.ArrayList;
import java.util.List;

/** parser manual para construir el arbol sintactico de fase 1. */
public final class Parser {

    private final List<Token> tokens;
    private final ReportCollector reportCollector;
    private int current;

    public Parser(List<Token> tokens, ReportCollector reportCollector) {
        this.tokens = tokens;
        this.reportCollector = reportCollector;
    }

    public Program parseProgram() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            Stmt statement = parseDeclarationOrStatement();
            if (statement != null) {
                statements.add(statement);
            } else {
                synchronize();
            }
            match(TokenType.SEMICOLON);
        }
        return new Program(statements);
    }

    private Stmt parseDeclarationOrStatement() {
        if (match(TokenType.VAR)) {
            return parseVarDeclaration();
        }
        return parseStatement();
    }

    private Stmt parseStatement() {
        if (match(TokenType.IF)) {
            return parseIfStatement();
        }
        if (match(TokenType.FOR)) {
            return parseForStatement();
        }
        if (match(TokenType.LBRACE)) {
            return parseBlockStatement();
        }
        if (match(TokenType.BREAK)) {
            return new BreakStmt();
        }
        if (match(TokenType.CONTINUE)) {
            return new ContinueStmt();
        }
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ASSIGN)) {
            return parseAssignmentStatement();
        }
        return new ExpressionStmt(parseExpression());
    }

    private Stmt parseVarDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "se esperaba un nombre de variable");
        ValueType declaredType = null;
        if (match(TokenType.TYPE_INT)) {
            declaredType = ValueType.INT;
        } else if (match(TokenType.TYPE_FLOAT64)) {
            declaredType = ValueType.FLOAT;
        } else if (match(TokenType.TYPE_STRING)) {
            declaredType = ValueType.STRING;
        } else if (match(TokenType.TYPE_BOOL)) {
            declaredType = ValueType.BOOL;
        } else if (match(TokenType.TYPE_RUNE)) {
            declaredType = ValueType.RUNE;
        }

        Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = parseExpression();
        }

        if (declaredType == null && initializer == null) {
            reportSyntax("la declaracion necesita tipo o inicializador", name);
        }

        return new VarDeclStmt(name.lexeme(), declaredType, initializer);
    }

    private Stmt parseAssignmentStatement() {
        Token name = consume(TokenType.IDENTIFIER, "se esperaba un identificador");
        consume(TokenType.ASSIGN, "se esperaba '=' en la asignacion");
        Expr value = parseExpression();
        return new ExpressionStmt(new AssignmentExpr(name.lexeme(), value));
    }

    private Stmt parseIfStatement() {
        Expr condition = parseExpression();
        Stmt thenBranch = parseStatementOrBlock();
        Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            if (match(TokenType.IF)) {
                elseBranch = parseIfStatement();
            } else {
                elseBranch = parseStatementOrBlock();
            }
        }
        return new IfStmt(condition, thenBranch, elseBranch);
    }

    private Stmt parseForStatement() {
        Stmt initializer = null;
        Expr condition = null;
        Stmt post = null;

        if (!check(TokenType.LBRACE)) {
            if (!check(TokenType.SEMICOLON)) {
                initializer = parseSimpleForClause();
            }
            consume(TokenType.SEMICOLON, "se esperaba ';' despues de la inicializacion del for");

            if (!check(TokenType.SEMICOLON)) {
                condition = parseExpression();
            }
            consume(TokenType.SEMICOLON, "se esperaba ';' despues de la condicion del for");

            if (!check(TokenType.LBRACE)) {
                post = parseSimpleForClause();
            }
        }

        Stmt body = parseStatementOrBlock();
        return new ForStmt(initializer, condition, post, body);
    }

    private Stmt parseSimpleForClause() {
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.ASSIGN)) {
            Token name = consume(TokenType.IDENTIFIER, "se esperaba un identificador");
            consume(TokenType.ASSIGN, "se esperaba '=' en la asignacion del for");
            Expr value = parseExpression();
            return new ExpressionStmt(new AssignmentExpr(name.lexeme(), value));
        }
        return new ExpressionStmt(parseExpression());
    }

    private Stmt parseStatementOrBlock() {
        if (match(TokenType.LBRACE)) {
            return parseBlockStatement();
        }
        return parseStatement();
    }

    private BlockStmt parseBlockStatement() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            Stmt statement = parseDeclarationOrStatement();
            if (statement != null) {
                statements.add(statement);
            } else {
                synchronize();
            }
            match(TokenType.SEMICOLON);
        }
        consume(TokenType.RBRACE, "se esperaba '}' al cerrar el bloque");
        return new BlockStmt(statements);
    }

    private Expr parseExpression() {
        return parseOr();
    }

    private Expr parseOr() {
        Expr expression = parseAnd();
        while (match(TokenType.OR_OR)) {
            Token operator = previous();
            Expr right = parseAnd();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseAnd() {
        Expr expression = parseEquality();
        while (match(TokenType.AND_AND)) {
            Token operator = previous();
            Expr right = parseEquality();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseEquality() {
        Expr expression = parseComparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = parseComparison();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseComparison() {
        Expr expression = parseTerm();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            Expr right = parseTerm();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseTerm() {
        Expr expression = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token operator = previous();
            Expr right = parseFactor();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseFactor() {
        Expr expression = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            Token operator = previous();
            Expr right = parseUnary();
            expression = new BinaryExpr(expression, operator.type(), right);
        }
        return expression;
    }

    private Expr parseUnary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            return new UnaryExpr(operator.type(), parseUnary());
        }
        return parsePrimary();
    }

    private Expr parsePrimary() {
        if (match(TokenType.BOOL_LITERAL)) {
            return new LiteralExpr(Value.bool(Boolean.TRUE.equals(previous().literal())));
        }
        if (match(TokenType.NIL_LITERAL)) {
            return new LiteralExpr(Value.nil());
        }
        if (match(TokenType.INT_LITERAL)) {
            return new LiteralExpr(Value.ofInt((Long) previous().literal()));
        }
        if (match(TokenType.FLOAT_LITERAL)) {
            return new LiteralExpr(Value.ofFloat((Double) previous().literal()));
        }
        if (match(TokenType.RUNE_LITERAL)) {
            return new LiteralExpr(Value.ofRune((Integer) previous().literal()));
        }
        if (match(TokenType.STRING_LITERAL)) {
            return new LiteralExpr(Value.ofString((String) previous().literal()));
        }
        if (match(TokenType.LPAREN)) {
            Expr expression = parseExpression();
            consume(TokenType.RPAREN, "se esperaba ')' al cerrar la expresion");
            return new GroupingExpr(expression);
        }

        if (check(TokenType.IDENTIFIER)) {
            return parseNameOrCall();
        }

        Token token = peek();
        reportSyntax("expresion inesperada", token);
        advance();
        return new LiteralExpr(Value.voidValue());
    }

    private Expr parseNameOrCall() {
        StringBuilder name = new StringBuilder(consume(TokenType.IDENTIFIER, "se esperaba un identificador").lexeme());
        while (match(TokenType.DOT)) {
            Token next = consume(TokenType.IDENTIFIER, "se esperaba un nombre despues del punto");
            name.append('.').append(next.lexeme());
        }

        if (match(TokenType.LPAREN)) {
            List<Expr> arguments = new ArrayList<>();
            if (!check(TokenType.RPAREN)) {
                do {
                    arguments.add(parseExpression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RPAREN, "se esperaba ')' al cerrar la llamada");
            return new CallExpr(name.toString(), arguments);
        }

        return new VariableExpr(name.toString());
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        Token token = peek();
        reportSyntax(message, token);
        return new Token(type, "", null, token.line(), token.column());
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) {
            return false;
        }
        return tokens.get(current + 1).type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private void synchronize() {
        if (!isAtEnd()) {
            advance();
        }

        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) {
                return;
            }
            switch (peek().type()) {
                case VAR, IF, FOR, BREAK, CONTINUE -> {
                    return;
                }
                default -> {
                }
            }
            advance();
        }
    }

    private void reportSyntax(String message, Token token) {
        reportCollector.addError(ErrorType.SYNTACTIC, token.line(), token.column(), message);
    }
}
package com.golite.lexer;

import java_cup.runtime.Symbol;
import com.golite.parser.sym;

%%

%class GoLiteLexer
%public
%unicode
%line
%column
%cup

// regex basicas
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

// comentarios para no perder el 20% de la nota
Comment = {TraditionalComment} | {EndOfLineComment}
TraditionalComment   = "/*" [^*] ~"*/" | "/*" "*"+ "/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?

// identificadores y literales
Identifier     = [a-zA-Z_][a-zA-Z0-9_]*
IntegerLiteral = [0-9]+
FloatLiteral   = [0-9]+"."[0-9]+
StringLiteral  = \"([^\"\\]|\\.)*\"

%%

<YYINITIAL> {
  // palabra clave de declaracion explicita
  "var"           { return new Symbol(sym.VAR, yyline+1, yycolumn+1); }

  // control de flujo
  "if"            { return new Symbol(sym.IF, yyline+1, yycolumn+1); }
  "else"          { return new Symbol(sym.ELSE, yyline+1, yycolumn+1); }
  "for"           { return new Symbol(sym.FOR, yyline+1, yycolumn+1); }
  "break"         { return new Symbol(sym.BREAK, yyline+1, yycolumn+1); }
  "continue"      { return new Symbol(sym.CONTINUE, yyline+1, yycolumn+1); }

  // fmt.Println (Fase 1: tratamos "fmt" y "Println" como palabras reservadas)
  "fmt"           { return new Symbol(sym.FMT, yyline+1, yycolumn+1); }
  "Println"       { return new Symbol(sym.PRINTLN, yyline+1, yycolumn+1); }

  // tipos basicos (palabras reservadas en esta fase)
  "int"           { return new Symbol(sym.TYPE_INT, yyline+1, yycolumn+1); }
  "float64"       { return new Symbol(sym.TYPE_FLOAT64, yyline+1, yycolumn+1); }
  "string"        { return new Symbol(sym.TYPE_STRING, yyline+1, yycolumn+1); }
  "bool"          { return new Symbol(sym.TYPE_BOOL, yyline+1, yycolumn+1); }
  "rune"          { return new Symbol(sym.TYPE_RUNE, yyline+1, yycolumn+1); }

  // booleanos
  "true"          { return new Symbol(sym.TRUE, yyline+1, yycolumn+1, true); }
  "false"         { return new Symbol(sym.FALSE, yyline+1, yycolumn+1, false); }

  // declaracion implicita y asignacion
  ":="            { return new Symbol(sym.DECLARE_ASSIGN, yyline+1, yycolumn+1); }

  // asignacion compuesta (deben ir antes de "=" y "+","-","*","/","%" para que JFlex los detecte completos)
  "+="            { return new Symbol(sym.PLUS_ASSIGN, yyline+1, yycolumn+1); }
  "-="            { return new Symbol(sym.MINUS_ASSIGN, yyline+1, yycolumn+1); }
  "*="            { return new Symbol(sym.STAR_ASSIGN, yyline+1, yycolumn+1); }
  "/="            { return new Symbol(sym.SLASH_ASSIGN, yyline+1, yycolumn+1); }
  "%="            { return new Symbol(sym.PERCENT_ASSIGN, yyline+1, yycolumn+1); }

  // comparacion (deben ir antes de "=", "<", ">", "!" simples)
  "=="            { return new Symbol(sym.EQ, yyline+1, yycolumn+1); }
  "!="            { return new Symbol(sym.NEQ, yyline+1, yycolumn+1); }
  "<="            { return new Symbol(sym.LE, yyline+1, yycolumn+1); }
  ">="            { return new Symbol(sym.GE, yyline+1, yycolumn+1); }
  "<"             { return new Symbol(sym.LT, yyline+1, yycolumn+1); }
  ">"             { return new Symbol(sym.GT, yyline+1, yycolumn+1); }

  // logicos
  "&&"            { return new Symbol(sym.AND, yyline+1, yycolumn+1); }
  "||"            { return new Symbol(sym.OR, yyline+1, yycolumn+1); }

  "="             { return new Symbol(sym.ASSIGN, yyline+1, yycolumn+1); }

  // incremento/decremento (deben ir antes de "+" y "-" simples)
  "++"            { return new Symbol(sym.INCREMENT, yyline+1, yycolumn+1); }
  "--"            { return new Symbol(sym.DECREMENT, yyline+1, yycolumn+1); }

  // operadores aritmeticos
  "+"             { return new Symbol(sym.PLUS, yyline+1, yycolumn+1); }
  "-"             { return new Symbol(sym.MINUS, yyline+1, yycolumn+1); }
  "*"             { return new Symbol(sym.STAR, yyline+1, yycolumn+1); }
  "/"             { return new Symbol(sym.SLASH, yyline+1, yycolumn+1); }
  "%"             { return new Symbol(sym.PERCENT, yyline+1, yycolumn+1); }
  "!"             { return new Symbol(sym.NOT, yyline+1, yycolumn+1); }

  // puntuacion
  ";"             { return new Symbol(sym.SEMI, yyline+1, yycolumn+1); }
  "."             { return new Symbol(sym.DOT, yyline+1, yycolumn+1); }
  "("             { return new Symbol(sym.LPAREN, yyline+1, yycolumn+1); }
  ")"             { return new Symbol(sym.RPAREN, yyline+1, yycolumn+1); }
  ","             { return new Symbol(sym.COMMA, yyline+1, yycolumn+1); }
  "{"             { return new Symbol(sym.LBRACE, yyline+1, yycolumn+1); }
  "}"             { return new Symbol(sym.RBRACE, yyline+1, yycolumn+1); }

  // valores (numeros y strings)
  {IntegerLiteral} { return new Symbol(sym.INT_LITERAL, yyline+1, yycolumn+1, Integer.parseInt(yytext())); }
  {FloatLiteral}   { return new Symbol(sym.FLOAT_LITERAL, yyline+1, yycolumn+1, Double.parseDouble(yytext())); }
  {StringLiteral}  { return new Symbol(sym.STRING_LITERAL, yyline+1, yycolumn+1, yytext().substring(1, yytext().length()-1)); }

  // identificador (nombres de variables) - va DESPUES de las palabras reservadas
  {Identifier}     { return new Symbol(sym.IDENTIFIER, yyline+1, yycolumn+1, yytext()); }

  // ignorar espacios y comentarios
  {WhiteSpace}     { /* no hacer nada con los espacios */ }
  {Comment}        { /* ignorar los comentarios para que el parser no explote */ }

  // recolector de errores lexicos (cualquier simbolo raro cae aca)
  . { 
      System.out.println("error lexico: caracter no reconocido '" + yytext() + "' en linea " + (yyline+1) + ", columna " + (yycolumn+1)); 
  }
}

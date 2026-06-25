package com.golite.lexer;

import java_cup.runtime.Symbol;
import com.golite.parser.sym; // tira error pero se quita cuando hagamos el cup

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
  // booleanos
  "true"          { return new Symbol(GoLiteSym.TRUE, yyline+1, yycolumn+1, true); }
  "false"         { return new Symbol(GoLiteSym.FALSE, yyline+1, yycolumn+1, false); }

  // operadores aritmeticos
  "+"             { return new Symbol(GoLiteSym.PLUS, yyline+1, yycolumn+1); }
  "-"             { return new Symbol(GoLiteSym.MINUS, yyline+1, yycolumn+1); }
  "!"             { return new Symbol(GoLiteSym.NOT, yyline+1, yycolumn+1); }

  // valores (numeros y strings)
  {IntegerLiteral} { return new Symbol(GoLiteSym.INT_LITERAL, yyline+1, yycolumn+1, Integer.parseInt(yytext())); }
  {FloatLiteral}   { return new Symbol(GoLiteSym.FLOAT_LITERAL, yyline+1, yycolumn+1, Double.parseDouble(yytext())); }
  {StringLiteral}  { return new Symbol(GoLiteSym.STRING_LITERAL, yyline+1, yycolumn+1, yytext().substring(1, yytext().length()-1)); }

  // ignorar espacios y comentarios
  {WhiteSpace}     { /* no hacer nada con los espacios */ }
  {Comment}        { /* ignorar los comentarios para que el parser no explote */ }

  // recolector de errores lexicos (cualquier simbolo raro cae aca)
  . { 
      System.out.println("error lexico: caracter no reconocido '" + yytext() + "' en linea " + (yyline+1) + ", columna " + (yycolumn+1)); 
  }
}
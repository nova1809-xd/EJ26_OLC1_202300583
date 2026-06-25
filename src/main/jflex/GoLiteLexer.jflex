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
  "true"          { return new Symbol(sym.TRUE, yyline+1, yycolumn+1, true); }
  "false"         { return new Symbol(sym.FALSE, yyline+1, yycolumn+1, false); }

  // operadores aritmeticos
  "+"             { return new Symbol(sym.PLUS, yyline+1, yycolumn+1); }
  "-"             { return new Symbol(sym.MINUS, yyline+1, yycolumn+1); }
  "!"             { return new Symbol(sym.NOT, yyline+1, yycolumn+1); }

  // valores (numeros y strings)
  {IntegerLiteral} { return new Symbol(sym.INT_LITERAL, yyline+1, yycolumn+1, Integer.parseInt(yytext())); }
  {FloatLiteral}   { return new Symbol(sym.FLOAT_LITERAL, yyline+1, yycolumn+1, Double.parseDouble(yytext())); }
  {StringLiteral}  { return new Symbol(sym.STRING_LITERAL, yyline+1, yycolumn+1, yytext().substring(1, yytext().length()-1)); }

  // ignorar espacios y comentarios
  {WhiteSpace}     { /* no hacer nada con los espacios */ }
  {Comment}        { /* ignorar los comentarios para que el parser no explote */ }

  // recolector de errores lexicos (cualquier simbolo raro cae aca)
  . { 
      System.out.println("error lexico: caracter no reconocido '" + yytext() + "' en linea " + (yyline+1) + ", columna " + (yycolumn+1)); 
  }
}
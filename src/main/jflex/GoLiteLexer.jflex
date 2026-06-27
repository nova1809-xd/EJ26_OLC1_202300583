package com.golite.lexer;

import java_cup.runtime.Symbol;
import com.golite.parser.sym;
import com.golite.reports.ReportCollector;
import com.golite.reports.ErrorType;

%%

%class GoLiteLexer
%public
%unicode
%line
%column
%cup

%{
    // mi recolector de errores para que la gui se entere de los problemas
    private ReportCollector collector;

    // constructor modificado para inyectar mi recolector
    public GoLiteLexer(java.io.Reader in, ReportCollector collector) {
        this(in);
        this.collector = collector;
    }
%}

// regex basicas
LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f]

// comentarios para no perder el 20% de la nota
Comment = {TraditionalComment} | {EndOfLineComment}
// Comentario de bloque: cualquier secuencia de caracteres que no contenga
// "*/" en medio. Usamos la forma estandar y robusta: cero o mas bloques de
// (no-asterisco* asterisco+ no-asterisco-no-barra) seguidos de asterisco+ barra.
// Esto si soporta correctamente el estilo Javadoc con " * " al inicio de cada
// linea intermedia, que es donde la regex anterior fallaba.
TraditionalComment   = "/*" ( [^*] | "*"+ [^*/] )* "*"* "*/"
EndOfLineComment     = "//" {InputCharacter}* {LineTerminator}?

// identificadores y literales
Identifier     = [a-zA-Z_][a-zA-Z0-9_]*
IntegerLiteral = [0-9]+
FloatLiteral   = [0-9]+"."[0-9]+
StringLiteral  = \"([^\"\\]|\\.)*\"
RuneLiteral    = '([^'\\]|\\.)'

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

  // funciones propias (Fase 2)
  "func"          { return new Symbol(sym.FUNC, yyline+1, yycolumn+1); }
  "return"        { return new Symbol(sym.RETURN, yyline+1, yycolumn+1); }

  // fmt.Println (Fase 1: tratamos "fmt" y "Println" como palabras reservadas)
  "fmt"           { return new Symbol(sym.FMT, yyline+1, yycolumn+1); }
  "Println"       { return new Symbol(sym.PRINTLN, yyline+1, yycolumn+1); }

  // reflect.TypeOf (Fase 1: igual que fmt.Println, palabras reservadas)
  "reflect"       { return new Symbol(sym.REFLECT, yyline+1, yycolumn+1); }
  "TypeOf"        { return new Symbol(sym.TYPEOF, yyline+1, yycolumn+1); }

  // strconv.Atoi / strconv.ParseFloat
  "strconv"       { return new Symbol(sym.STRCONV, yyline+1, yycolumn+1); }
  "Atoi"          { return new Symbol(sym.ATOI, yyline+1, yycolumn+1); }
  "ParseFloat"    { return new Symbol(sym.PARSEFLOAT, yyline+1, yycolumn+1); }

  // tipos basicos (palabras reservadas en esta fase)
  "int"           { return new Symbol(sym.TYPE_INT, yyline+1, yycolumn+1); }
  "float64"       { return new Symbol(sym.TYPE_FLOAT64, yyline+1, yycolumn+1); }
  "string"        { return new Symbol(sym.TYPE_STRING, yyline+1, yycolumn+1); }
  "bool"          { return new Symbol(sym.TYPE_BOOL, yyline+1, yycolumn+1); }
  "rune"          { return new Symbol(sym.TYPE_RUNE, yyline+1, yycolumn+1); }

  // booleanos
  "true"          { return new Symbol(sym.TRUE, yyline+1, yycolumn+1, true); }
  "false"         { return new Symbol(sym.FALSE, yyline+1, yycolumn+1, false); }

  // literal nil
  "nil"           { return new Symbol(sym.NIL, yyline+1, yycolumn+1); }

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
  {RuneLiteral}    {
                       String inner = yytext().substring(1, yytext().length() - 1);
                       char value;
                       if (inner.length() == 2 && inner.charAt(0) == '\\') {
                           // manejo de escapes comunes: \n \t \\ \' \"
                           switch (inner.charAt(1)) {
                               case 'n': value = '\n'; break;
                               case 't': value = '\t'; break;
                               case 'r': value = '\r'; break;
                               case '\\': value = '\\'; break;
                               case '\'': value = '\''; break;
                               case '"': value = '"'; break;
                               case '0': value = '\0'; break;
                               default: value = inner.charAt(1);
                           }
                       } else {
                           value = inner.charAt(0);
                       }
                       return new Symbol(sym.RUNE_LITERAL, yyline+1, yycolumn+1, value);
                   }

  // identificador (nombres de variables) - va DESPUES de las palabras reservadas
  {Identifier}     { return new Symbol(sym.IDENTIFIER, yyline+1, yycolumn+1, yytext()); }

  // ignorar espacios y comentarios
  {WhiteSpace}     { /* no hacer nada con los espacios */ }
  {Comment}        { /* ignorar los comentarios para que el parser no explote */ }

  // recolector de errores lexicos conectado a mi GUI
  . { 
      if (collector != null) {
          collector.addError(ErrorType.LEXICAL, yyline + 1, yycolumn + 1, "caracter no reconocido: '" + yytext() + "'");
      }
  }
}
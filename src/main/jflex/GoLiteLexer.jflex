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
    // mi recolector de errores y tokens para que la gui se entere de todo
    private ReportCollector collector;

    // constructor modificado para inyectar mi recolector
    public GoLiteLexer(java.io.Reader in, ReportCollector collector) {
        this(in);
        this.collector = collector;
    }

    // helper unico que registra el token en el ReportCollector (si existe)
    // y construye el Symbol que se le entrega al parser. Centralizar esto
    // en un solo lugar evita tener que repetir la llamada al collector en
    // cada una de las ~50 reglas lexicas, y garantiza que linea/columna/
    // lexema siempre se reporten de forma consistente.
    // helper unico que registra el token en el ReportCollector estático
    private Symbol token(int symType, String typeName) {
        // llamada directa a la memoria global
        com.golite.reports.ReportCollector.addToken(new Token(yyline + 1, yycolumn + 1, yytext(), typeName));
        
        return new Symbol(symType, yyline + 1, yycolumn + 1);
    }

    private Symbol token(int symType, String typeName, Object value) {
        // llamada directa a la memoria global
        com.golite.reports.ReportCollector.addToken(new Token(yyline + 1, yycolumn + 1, yytext(), typeName));
        
        return new Symbol(symType, yyline + 1, yycolumn + 1, value);
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
  "var"           { return token(sym.VAR, "VAR"); }

  // control de flujo
  "if"            { return token(sym.IF, "IF"); }
  "else"          { return token(sym.ELSE, "ELSE"); }
  "for"           { return token(sym.FOR, "FOR"); }
  "break"         { return token(sym.BREAK, "BREAK"); }
  "continue"      { return token(sym.CONTINUE, "CONTINUE"); }

  // funciones propias (Fase 2)
  "func"          { return token(sym.FUNC, "FUNC"); }
  "return"        { return token(sym.RETURN, "RETURN"); }

  // structs (Fase 2 - Bloque 1)
  "struct"        { return token(sym.STRUCT, "STRUCT"); }

  // funciones nativas de slices
  "append"        { return token(sym.APPEND, "APPEND"); }
  "len"           { return token(sym.LEN, "LEN"); }

  // fmt.Println (Fase 1: tratamos "fmt" y "Println" como palabras reservadas)
  "fmt"           { return token(sym.FMT, "FMT"); }
  "Println"       { return token(sym.PRINTLN, "PRINTLN"); }

  // reflect.TypeOf (Fase 1: igual que fmt.Println, palabras reservadas)
  "reflect"       { return token(sym.REFLECT, "REFLECT"); }
  "TypeOf"        { return token(sym.TYPEOF, "TYPEOF"); }

  // strconv.Atoi / strconv.ParseFloat
  "strconv"       { return token(sym.STRCONV, "STRCONV"); }
  "Atoi"          { return token(sym.ATOI, "ATOI"); }
  "ParseFloat"    { return token(sym.PARSEFLOAT, "PARSEFLOAT"); }

  // tipos basicos (palabras reservadas en esta fase)
  "int"           { return token(sym.TYPE_INT, "TYPE_INT"); }
  "float64"       { return token(sym.TYPE_FLOAT64, "TYPE_FLOAT64"); }
  "string"        { return token(sym.TYPE_STRING, "TYPE_STRING"); }
  "bool"          { return token(sym.TYPE_BOOL, "TYPE_BOOL"); }
  "rune"          { return token(sym.TYPE_RUNE, "TYPE_RUNE"); }

  // booleanos
  "true"          { return token(sym.TRUE, "TRUE", true); }
  "false"         { return token(sym.FALSE, "FALSE", false); }

  // literal nil
  "nil"           { return token(sym.NIL, "NIL"); }

  // declaracion implicita y asignacion
  ":="            { return token(sym.DECLARE_ASSIGN, "DECLARE_ASSIGN"); }

  // asignacion compuesta (deben ir antes de "=" y "+","-","*","/","%" para que JFlex los detecte completos)
  "+="            { return token(sym.PLUS_ASSIGN, "PLUS_ASSIGN"); }
  "-="            { return token(sym.MINUS_ASSIGN, "MINUS_ASSIGN"); }
  "*="            { return token(sym.STAR_ASSIGN, "STAR_ASSIGN"); }
  "/="            { return token(sym.SLASH_ASSIGN, "SLASH_ASSIGN"); }
  "%="            { return token(sym.PERCENT_ASSIGN, "PERCENT_ASSIGN"); }

  // comparacion (deben ir antes de "=", "<", ">", "!" simples)
  "=="            { return token(sym.EQ, "EQ"); }
  "!="            { return token(sym.NEQ, "NEQ"); }
  "<="            { return token(sym.LE, "LE"); }
  ">="            { return token(sym.GE, "GE"); }
  "<"             { return token(sym.LT, "LT"); }
  ">"             { return token(sym.GT, "GT"); }

  // logicos
  "&&"            { return token(sym.AND, "AND"); }
  "||"            { return token(sym.OR, "OR"); }

  "="             { return token(sym.ASSIGN, "ASSIGN"); }

  // incremento/decremento (deben ir antes de "+" y "-" simples)
  "++"            { return token(sym.INCREMENT, "INCREMENT"); }
  "--"            { return token(sym.DECREMENT, "DECREMENT"); }

  // operadores aritmeticos
  "+"             { return token(sym.PLUS, "PLUS"); }
  "-"             { return token(sym.MINUS, "MINUS"); }
  "*"             { return token(sym.STAR, "STAR"); }
  "/"             { return token(sym.SLASH, "SLASH"); }
  "%"             { return token(sym.PERCENT, "PERCENT"); }
  "!"             { return token(sym.NOT, "NOT"); }

  // puntuacion
  ";"             { return token(sym.SEMI, "SEMI"); }
  "."             { return token(sym.DOT, "DOT"); }
  "("             { return token(sym.LPAREN, "LPAREN"); }
  ")"             { return token(sym.RPAREN, "RPAREN"); }
  ","             { return token(sym.COMMA, "COMMA"); }
  "{"             { return token(sym.LBRACE, "LBRACE"); }
  "}"             { return token(sym.RBRACE, "RBRACE"); }
  "["             { return token(sym.LBRACKET, "LBRACKET"); }
  "]"             { return token(sym.RBRACKET, "RBRACKET"); }
  ":"             { return token(sym.COLON, "COLON"); }

  // valores (numeros y strings)
  {IntegerLiteral} { return token(sym.INT_LITERAL, "INT_LITERAL", Integer.parseInt(yytext())); }
  {FloatLiteral}   { return token(sym.FLOAT_LITERAL, "FLOAT_LITERAL", Double.parseDouble(yytext())); }
  {StringLiteral}  { return token(sym.STRING_LITERAL, "STRING_LITERAL", yytext().substring(1, yytext().length()-1)); }
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
                       return token(sym.RUNE_LITERAL, "RUNE_LITERAL", value);
                   }

  // identificador (nombres de variables) - va DESPUES de las palabras reservadas
  {Identifier}     { return token(sym.IDENTIFIER, "IDENTIFIER", yytext()); }

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

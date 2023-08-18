package decaf.analysis;


import org.jetbrains.annotations.NotNull;

import decaf.analysis.lexical.Scanner;

public class Token {
  @NotNull public TokenPosition tokenPosition;
  @NotNull public Type type;
  @NotNull public String lexeme;

  public Token(
      @NotNull TokenPosition tokenPosition,
      @NotNull Type type,
      @NotNull String lexeme
  ) {
    this.tokenPosition = tokenPosition;
    this.type = type;
    this.lexeme = lexeme;
  }

  public static String getScannerSourceCode(@NotNull Token.Type type) {
    return switch (type) {
      case TERNARY_COLON -> Scanner.TERNARY_COLON;
      case SEMICOLON -> Scanner.SEMICOLON;
      case RIGHT_PARENTHESIS -> Scanner.RIGHT_PARENTHESIS;
      case RIGHT_SQUARE_BRACKET -> Scanner.RIGHT_SQUARE_BRACKET;
      case RIGHT_CURLY -> Scanner.RIGHT_CURLY;
      case PLUS -> Scanner.PLUS;
      case NOT -> Scanner.NOT;
      case MULTIPLY -> Scanner.MULTIPLY;
      case MINUS -> Scanner.MINUS;
      case LT -> Scanner.LT;
      case LEFT_PARENTHESIS -> Scanner.LEFT_PARENTHESIS;
      case LEFT_SQUARE_BRACKET -> Scanner.LEFT_SQUARE_BRACKET;
      case LEFT_CURLY -> Scanner.LEFT_CURLY;
      case LEQ -> Scanner.LEQ;
      case INCREMENT -> Scanner.INCREMENT;
      case GT -> Scanner.GT;
      case GEQ -> Scanner.GEQ;
      case EQ -> Scanner.EQ;
      case DECREMENT -> Scanner.DECREMENT;
      case COMMA -> Scanner.COMMA;
      case CONDITIONAL_OR -> Scanner.CONDITIONAL_OR;
      case CONDITIONAL_AND -> Scanner.CONDITIONAL_AND;
      case ASSIGN -> Scanner.ASSIGN;
      case ADD_ASSIGN -> Scanner.ADD_ASSIGN;
      case MOD -> Scanner.MOD;
      case MINUS_ASSIGN -> Scanner.MINUS_ASSIGN;
      case MULTIPLY_ASSIGN -> Scanner.MULTIPLY_ASSIGN;
      case DIVIDE -> Scanner.DIVIDE;
      case TERNARY_QUESTION_MARK -> Scanner.TERNARY_QUESTION_MARK;
      case NEQ -> Scanner.NEQ;
      case RESERVED_BREAK -> Scanner.RESERVED_BREAK;
      case RESERVED_CONTINUE -> Scanner.RESERVED_CONTINUE;
      case RESERVED_ELSE -> Scanner.RESERVED_ELSE;
      case RESERVED_FALSE -> Scanner.RESERVED_FALSE;
      case RESERVED_FOR -> Scanner.RESERVED_FOR;
      case RESERVED_BOOL -> Scanner.RESERVED_BOOL;
      case RESERVED_IF -> Scanner.RESERVED_IF;
      case RESERVED_IMPORT -> Scanner.RESERVED_IMPORT;
      case RESERVED_INT -> Scanner.RESERVED_INT;
      case RESERVED_LEN -> Scanner.RESERVED_LEN;
      case RESERVED_RETURN -> Scanner.RESERVED_RETURN;
      case RESERVED_TRUE -> Scanner.RESERVED_TRUE;
      case RESERVED_VOID -> Scanner.RESERVED_VOID;
      case RESERVED_WHILE -> Scanner.RESERVED_WHILE;
      case INT_LITERAL, STRING_LITERAL, CHAR_LITERAL, ID, LINE_COMMENT, BLOCK_COMMENT, WHITESPACE, ERROR ->
          Scanner.IDENTIFIER;
      case EOF -> Scanner.EOF;
    };
  }

  @Override
  public String toString() {
    return "Token{" + "type=" + type +
        ", lexeme='" + lexeme + '\'' +
        '}';
  }

  // All the terminals
  public enum Type {
    RESERVED_IMPORT, RESERVED_INT, RESERVED_BOOL, RESERVED_IF, RESERVED_ELSE, RESERVED_FOR, RESERVED_RETURN, RESERVED_BREAK, RESERVED_CONTINUE, RESERVED_WHILE, RESERVED_VOID, RESERVED_LEN, RESERVED_TRUE, RESERVED_FALSE,

    LEFT_CURLY, RIGHT_CURLY, LEFT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET, LEFT_PARENTHESIS, RIGHT_PARENTHESIS,

    SEMICOLON, COMMA,

    TERNARY_QUESTION_MARK, TERNARY_COLON,

    NOT,

    PLUS, MINUS, MULTIPLY, DIVIDE, ASSIGN, MOD,

    ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN,

    INCREMENT, DECREMENT,

    LT, GT,

    LEQ, GEQ, EQ, NEQ,

    INT_LITERAL, CHAR_LITERAL, STRING_LITERAL,

    ID,

    LINE_COMMENT, BLOCK_COMMENT,

    CONDITIONAL_OR, CONDITIONAL_AND,

    EOF, WHITESPACE,

    ERROR,
  }
}

package decaf.grammar;

// All the terminals
public enum TokenType {
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

  DECIMAL_LITERAL, HEX_LITERAL, CHAR_LITERAL, STRING_LITERAL,

  ID,

  LINE_COMMENT, BLOCK_COMMENT,

  CONDITIONAL_OR, CONDITIONAL_AND,

  EOF, WHITESPACE,

  ERROR,
}

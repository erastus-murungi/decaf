package decaf.grammar;


import org.jetbrains.annotations.NotNull;

public class Token {
  public TokenPosition tokenPosition;
  public TokenType tokenType;
  public String lexeme;

  public Token(
      TokenPosition tokenPosition,
      TokenType tokenType,
      String lexeme
  ) {
    this.tokenPosition = tokenPosition;
    this.tokenType = tokenType;
    this.lexeme = lexeme;
  }

  public String lexeme() {
    return lexeme;
  }

  public TokenType tokenType() {
    return tokenType;
  }

  public TokenPosition tokenPosition() {
    return tokenPosition;
  }

  public boolean isNotEOF() {
    return tokenType != TokenType.EOF;
  }

  @Override
  public String toString() {
    return "Token{" + "type=" + tokenType +
        ", lexeme='" + lexeme + '\'' +
        '}';
  }

  public static String getScannerSourceCode(@NotNull TokenType tokenType) {
    return switch (tokenType) {
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
      case DECIMAL_LITERAL, STRING_LITERAL, CHAR_LITERAL, ID, HEX_LITERAL -> Scanner.IDENTIFIER;
      case LINE_COMMENT -> Scanner.IDENTIFIER;
      case BLOCK_COMMENT -> Scanner.IDENTIFIER;
      case EOF -> Scanner.EOF;
      case WHITESPACE -> Scanner.IDENTIFIER;
      case ERROR -> Scanner.IDENTIFIER;
    };
  }
}

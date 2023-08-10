package decaf.grammar;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import decaf.common.DecafExceptionProcessor;
import decaf.common.Utils;

public class Scanner {
  public static final String LEFT_CURLY = "{";
  public static final String RIGHT_CURLY = "}";
  public static final String LEFT_SQUARE_BRACKET = "[";
  public static final String RIGHT_SQUARE_BRACKET = "]";
  public static final String LEFT_PARENTHESIS = "(";
  public static final String RIGHT_PARENTHESIS = ")";

  public static final String SEMICOLON = ";";
  public static final String COMMA = ",";
  public static final String PLUS = "+";
  public static final String MINUS = "-";
  public static final String DIVIDE = "/";
  public static final String MULTIPLY = "*";
  public static final String MOD = "%";

  public static final String LT = "<";
  public static final String GT = ">";

  public static final String TERNARY_QUESTION_MARK = "?";
  public static final String TERNARY_COLON = ":";

  public static final String NOT = "!";

  public static final String ASSIGN = "=";

  public static final String SINGLE_QUOTES = "'";
  public static final String DOUBLE_QUOTES = "\"";

  public static final String NEW_LINE = "\n";

  public static final String ADD_ASSIGN = "+=";
  public static final String MINUS_ASSIGN = "-=";
  public static final String MULTIPLY_ASSIGN = "*=";

  public static final String LEQ = "<=";
  public static final String GEQ = ">=";
  public static final String EQ = "==";
  public static final String NEQ = "!=";

  public static final String INCREMENT = "++";
  public static final String DECREMENT = "--";

  public static final String CONDITIONAL_OR = "||";
  public static final String CONDITIONAL_AND = "&&";

  public static final String LINE_COMMENT_START = "//";
  public static final String BLOCK_COMMENT_START = "/*";

  public static final String HEX_PREFIX_LOWERCASE = "0x";

  public static final String IDENTIFIER = "identifier";

  public static final String EOF = "END_OF_FILE_MARKER";

  public static final String RESERVED_FOR = "for";
  public static final String RESERVED_IF = "if";
  public static final String RESERVED_IMPORT = "import";
  public static final String RESERVED_INT = "int";
  public static final String RESERVED_LEN = "len";
  public static final String RESERVED_RETURN = "return";
  public static final String RESERVED_ELSE = "else";
  public static final String RESERVED_CONTINUE = "continue";
  public static final String RESERVED_BREAK = "break";
  public static final String RESERVED_WHILE = "while";
  public static final String RESERVED_VOID = "void";
  public static final String RESERVED_BOOL = "bool";
  public static final String RESERVED_TRUE = "true";
  public static final String RESERVED_FALSE = "false";
  private final DecafExceptionProcessor decafExceptionProcessor;
  private final String inputString;
  private final List<String> syntaxLines = new ArrayList<>();
  private final List<ScannerError> errors = new ArrayList<>();
  private int column;
  private int line;
  private int stringIndex;
  private Token prevToken = null;
  private boolean shouldTrace = false;


  public Scanner(
      String in,
      DecafExceptionProcessor decafExceptionProcessor
  ) {
    inputString = maybeAppendNewLineCharacter(in);
    stringIndex = 0;
    this.decafExceptionProcessor = decafExceptionProcessor;
  }

  private static boolean isValidIdFirstCodePoint(char c) {
    return Character.isLetter(c) || c == '_';
  }

  public List<ScannerError> getErrors() {
    return errors;
  }

  public String getPrettyErrorOutput() {
    return decafExceptionProcessor.processScannerErrorOutput(errors);
  }

  public boolean finished() {
    return errors.isEmpty();
  }

  private String maybeAppendNewLineCharacter(String string) {
    if (string.charAt(string.length() - 1) != NEW_LINE.charAt(0)) {
      return string + NEW_LINE;
    }
    return string;
  }

  private void consumeCharacterNoCheck() {
    ++stringIndex;
    ++column;
  }

  private char getCurrentChar() {
    return inputString.charAt(stringIndex);
  }

  private void consumeNewlineCharacter(String sc) {
    final char c = sc.charAt(0);
    if (c != '\r' && c != '\n') {
      recordScannerError(
          ScannerError.ErrorType.INVALID_NEWLINE,
          String.format(
              "expected a newline character, one of: %s received %s",
              String.valueOf(new char[]{'\r', '\n'}),
              c
          ),
          new TokenPosition(
              line,
              column,
              stringIndex
          )
      );
      stringIndex += sc.length();
    } else {
      column = 0;
      ++line;
      ++stringIndex;
    }
  }

  private void consumeCharacter(
      TokenPosition tokenPosition,
      String c,
      String detail
  ) {
    if (inputString.charAt(stringIndex) != c.charAt(0)) {
      recordScannerError(
          ScannerError.ErrorType.INVALID_CHAR,
          String.format(
              "expected %s received %s (%s)",
              c,
              inputString.charAt(stringIndex)
              ,
              detail
          ),
          tokenPosition
      );
    } else {
      consumeCharacterNoCheck();
    }
  }

  private void consumeCompoundCharacter(String compoundOp) {
    if (!inputString.startsWith(
        compoundOp,
        stringIndex
    )) {
      throw new IllegalArgumentException("expected " + compoundOp + "received " + inputString.substring(
          stringIndex,
          stringIndex + 2
      ));
    }
    column += 2;
    stringIndex += 2;
  }

  private void consumeCharacter(
      char c,
      Function<Character, Boolean> validator
  ) {
    if (!validator.apply(inputString.charAt(stringIndex)))
      throw new IllegalArgumentException("expected " + c + " to be accepted by validator" + validator);
    consumeCharacterNoCheck();
  }

  private boolean isSkipAble(Token token) {
    return token.tokenType() == TokenType.WHITESPACE || token.tokenType() == TokenType.LINE_COMMENT ||
        token.tokenType() == TokenType.BLOCK_COMMENT || token.tokenType() == TokenType.ERROR;
  }

  /**
   * might find this useful
   **/
  public Token nextToken() {
    Token token;
    do {
      token = nextTokenHelper();
      if (!isSkipAble(token))
        prevToken = token;
      updateHighlighter(token);
      if (shouldTrace) {
        System.out.println(token);
      }
    } while (isSkipAble(token));
    decafExceptionProcessor.syntaxHighlightedSourceCode = String.join(
        "",
        syntaxLines
    );
    return token;
  }

  private boolean currentSubstringMatches(String s) {
    return inputString.startsWith(
        s,
        stringIndex
    );
  }

  private Token nextTokenHelper() {
    if (stringIndex >= inputString.length()) {
      if (prevToken != null)
        return makeToken(
            prevToken.tokenPosition(),
            TokenType.EOF,
            EOF
        );
      else
        return makeToken(
            new TokenPosition(
                line,
                column,
                stringIndex
            ),
            TokenType.EOF,
            EOF
        );
    }
    final char c = inputString.charAt(stringIndex);
    final TokenPosition tokenPosition = new TokenPosition(
        line,
        column,
        stringIndex
    );
    switch (String.valueOf(c)) {
      case LEFT_CURLY -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.LEFT_CURLY,
            LEFT_CURLY
        );
      }
      case RIGHT_CURLY -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.RIGHT_CURLY,
            RIGHT_CURLY
        );
      }
      case LEFT_PARENTHESIS -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.LEFT_PARENTHESIS,
            LEFT_PARENTHESIS
        );
      }
      case RIGHT_PARENTHESIS -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.RIGHT_PARENTHESIS,
            RIGHT_PARENTHESIS
        );
      }
      case LEFT_SQUARE_BRACKET -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.LEFT_SQUARE_BRACKET,
            LEFT_SQUARE_BRACKET
        );
      }
      case RIGHT_SQUARE_BRACKET -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.RIGHT_SQUARE_BRACKET,
            RIGHT_SQUARE_BRACKET
        );
      }
      case SEMICOLON -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.SEMICOLON,
            SEMICOLON
        );
      }
      case COMMA -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.COMMA,
            COMMA
        );
      }
      case MOD -> {
        return handleSingleOperator(
            tokenPosition,
            TokenType.MOD,
            MOD
        );
      }
      case DOUBLE_QUOTES -> {
        return handleStringLiteral(tokenPosition);
      }
      case SINGLE_QUOTES -> {
        return handleCharLiteral(tokenPosition);
      }
      case " ", "\t", "\n", "\r" -> {
        return handleWhiteSpace(tokenPosition);
      }
      default -> {
        if (currentSubstringMatches(LINE_COMMENT_START))
          return handleSingleLineComment(tokenPosition);
        else if (currentSubstringMatches(BLOCK_COMMENT_START))
          return handleBlockComment(tokenPosition);
        else if (isValidIdFirstCodePoint(c))
          return handleId(tokenPosition);
        else if (currentSubstringMatches(HEX_PREFIX_LOWERCASE))
          return handleHexLiteral(tokenPosition);
        else if (Character.isDigit(c))
          return handleDecimalLiteral(tokenPosition);
        else if (currentSubstringMatches(ADD_ASSIGN))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.ADD_ASSIGN,
              ADD_ASSIGN
          );
        else if (currentSubstringMatches(MINUS_ASSIGN))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.MINUS_ASSIGN,
              MINUS_ASSIGN
          );
        else if (currentSubstringMatches(MULTIPLY_ASSIGN))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.MULTIPLY_ASSIGN,
              MULTIPLY_ASSIGN
          );
        else if (currentSubstringMatches(NEQ))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.NEQ,
              NEQ
          );
        else if (currentSubstringMatches(GEQ))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.GEQ,
              GEQ
          );
        else if (currentSubstringMatches(LEQ))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.LEQ,
              LEQ
          );
        else if (currentSubstringMatches(EQ))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.EQ,
              EQ
          );
        else if (currentSubstringMatches(INCREMENT))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.INCREMENT,
              INCREMENT
          );
        else if (currentSubstringMatches(DECREMENT))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.DECREMENT,
              DECREMENT
          );
        else if (currentSubstringMatches(CONDITIONAL_OR))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.CONDITIONAL_OR,
              CONDITIONAL_OR
          );
        else if (currentSubstringMatches(CONDITIONAL_AND))
          return handleCompoundOperator(
              tokenPosition,
              TokenType.CONDITIONAL_AND,
              CONDITIONAL_AND
          );
        else {
          return switch (String.valueOf(c)) {
            case NOT -> handleSingleOperator(
                tokenPosition,
                TokenType.NOT,
                NOT
            );
            case PLUS -> handleSingleOperator(
                tokenPosition,
                TokenType.PLUS,
                PLUS
            );
            case MINUS -> handleSingleOperator(
                tokenPosition,
                TokenType.MINUS,
                MINUS
            );
            case MULTIPLY -> handleSingleOperator(
                tokenPosition,
                TokenType.MULTIPLY,
                MULTIPLY
            );
            case DIVIDE -> handleSingleOperator(
                tokenPosition,
                TokenType.DIVIDE,
                DIVIDE
            );
            case ASSIGN -> handleSingleOperator(
                tokenPosition,
                TokenType.ASSIGN,
                ASSIGN
            );
            case LT -> handleSingleOperator(
                tokenPosition,
                TokenType.LT,
                LT
            );
            case GT -> handleSingleOperator(
                tokenPosition,
                TokenType.GT,
                GT
            );
            case TERNARY_QUESTION_MARK -> handleSingleOperator(
                tokenPosition,
                TokenType.TERNARY_QUESTION_MARK,
                TERNARY_QUESTION_MARK
            );
            case TERNARY_COLON -> handleSingleOperator(
                tokenPosition,
                TokenType.TERNARY_COLON,
                TERNARY_COLON
            );
            default -> {
              recordScannerError(
                  ScannerError.ErrorType.INVALID_CHAR,
                  String.format(
                      "expected a valid character but received invalid character %s",
                      c
                  ),
                  tokenPosition
              );
              yield nextTokenHelper();
            }
          };
        }
      }
    }
  }

  private Token handleSingleLineComment(TokenPosition tokenPosition) {
    consumeCompoundCharacter(LINE_COMMENT_START);

    final int posEndComment = inputString.indexOf(
        NEW_LINE,
        stringIndex
    );
    assert posEndComment != -1;

    consumeMultipleCharactersNoCheck(posEndComment - tokenPosition.offset() - 2);
    consumeNewlineCharacter(NEW_LINE);

    return makeToken(
        tokenPosition,
        TokenType.LINE_COMMENT,
        inputString.substring(
            tokenPosition.offset(),
            stringIndex - 1
        )
    );
  }

  private Token handleBlockComment(TokenPosition tokenPosition) {
    consumeCompoundCharacter(BLOCK_COMMENT_START);

    int i = stringIndex;

    final int waitingForStar = 0;
    final int waitingForForwardSlash = 1;
    final int completed = 2;

    int state = waitingForStar;
    while (i < inputString.length() && state != completed) {
      final char c = inputString.charAt(i);

      if (state == waitingForStar) {
        if (c == '*') {
          state = waitingForForwardSlash;
        } else if (c == '\n' | c == '\r') {
          column = -1; // the global
          line += 1;
        }
      } else {
        state = switch (c) {
          case '/' -> completed;
          case '*' -> waitingForForwardSlash;
          default -> waitingForStar;
        };
      }
      consumeCharacterNoCheck();
      ++i;
    }
    if (state != completed) {
      recordScannerError(
          ScannerError.ErrorType.INVALID_COMMENT,
          "could not finish parsing the comment" + inputString.substring(
              tokenPosition.offset(),
              Math.min(
                  inputString.length(),
                  tokenPosition.offset() + 10
              )
          ) + "...",
          tokenPosition
      )
      ;
    }
    return makeToken(
        tokenPosition,
        TokenType.BLOCK_COMMENT,
        inputString.substring(
            tokenPosition.offset(),
            i
        )
    );
  }

  private void handleEscape(TokenPosition tokenPosition) {
    consumeCharacterNoCheck();
    char c = inputString.charAt(stringIndex);
    switch (c) {
      case 'n', '"', 't', 'r', '\'', '\\' -> consumeCharacterNoCheck();
      default -> recordScannerError(
          ScannerError.ErrorType.INVALID_ESCAPE_SEQUENCE,
          String.format(
              "found invalid escape sequence \\%s",
              c
          ),
          tokenPosition
      );
    }
  }

  private boolean isValidChar(char c) {
    if (c >= ' ' && c <= '!')
      return true;
    if (c >= '#' && c <= '&')
      return true;
    if (c >= '(' && c <= '[')
      return true;
    return c >= ']' && c <= '~';
  }

  private Token handleCharLiteral(TokenPosition tokenPosition) {
    consumeCharacterNoCheck();

    final char c = inputString.charAt(stringIndex);
    if (c == '\\' || isValidChar(c)) {
      if (c == '\\')
        handleEscape(tokenPosition);
      else if (isValidChar(c))
        consumeCharacter(
            c,
            this::isValidChar
        );
      consumeCharacter(
          tokenPosition,
          SINGLE_QUOTES,
          decafExceptionProcessor.getContextualErrorMessage(
              tokenPosition,
              "missing closing single quotes " + "current char literal is " + inputString.substring(
                  tokenPosition.offset(),
                  stringIndex + 1
              )
          )
      );
      return makeToken(
          tokenPosition,
          TokenType.CHAR_LITERAL,
          inputString.substring(
              tokenPosition.offset(),
              stringIndex
          )
      );
    } else {
      return recordScannerError(
          ScannerError.ErrorType.INVALID_CHAR_LITERAL,
          String.format(
              "found invalid char literal %s",
              c
          ),
          tokenPosition
      );
    }
  }

  private Token handleStringLiteral(TokenPosition tokenPosition) {
    consumeCharacterNoCheck();

    char c;
    while (true) {
      c = inputString.charAt(stringIndex);
      if (c == '\\')
        handleEscape(tokenPosition);
      else if (isValidChar(c))
        consumeCharacter(
            c,
            this::isValidChar
        );
      else
        break;
    }
    consumeCharacter(
        tokenPosition,
        DOUBLE_QUOTES,
        decafExceptionProcessor.getContextualErrorMessage(
            tokenPosition,
            "expected " + DOUBLE_QUOTES + " received " + inputString.charAt(stringIndex)
        )
    );
    return makeToken(
        tokenPosition,
        TokenType.STRING_LITERAL,
        inputString.substring(
            tokenPosition.offset(),
            stringIndex
        )
    );
  }

  private Token handleId(TokenPosition tokenPosition) {
    int i = stringIndex;
    while (i < inputString.length()) {
      final char alphaNum = inputString.charAt(i);
      if (!(Character.isLetterOrDigit(alphaNum) || alphaNum == '_'))
        break;
      ++i;
    }
    String idLexeme = inputString.substring(
        stringIndex,
        i
    );
    TokenType tokenType;
    switch (idLexeme) {
      case RESERVED_FOR -> tokenType = TokenType.RESERVED_FOR;
      case RESERVED_IF -> tokenType = TokenType.RESERVED_IF;
      case RESERVED_IMPORT -> tokenType = TokenType.RESERVED_IMPORT;
      case RESERVED_INT -> tokenType = TokenType.RESERVED_INT;
      case RESERVED_LEN -> tokenType = TokenType.RESERVED_LEN;
      case RESERVED_RETURN -> tokenType = TokenType.RESERVED_RETURN;
      case RESERVED_ELSE -> tokenType = TokenType.RESERVED_ELSE;
      case RESERVED_CONTINUE -> tokenType = TokenType.RESERVED_CONTINUE;
      case RESERVED_BREAK -> tokenType = TokenType.RESERVED_BREAK;
      case RESERVED_WHILE -> tokenType = TokenType.RESERVED_WHILE;
      case RESERVED_VOID -> tokenType = TokenType.RESERVED_VOID;
      case RESERVED_BOOL -> tokenType = TokenType.RESERVED_BOOL;
      case RESERVED_TRUE -> tokenType = TokenType.RESERVED_TRUE;
      case RESERVED_FALSE -> tokenType = TokenType.RESERVED_FALSE;
      default -> tokenType = TokenType.ID;
    }

    consumeMultipleCharactersNoCheck(i - stringIndex);
    return makeToken(
        tokenPosition,
        tokenType,
        idLexeme
    );
  }

  private void consumeMultipleCharactersNoCheck(int nChars) {
    stringIndex += nChars;
    column += nChars;
  }

  private Token handleDecimalLiteral(TokenPosition tokenPosition) {
    assert Character.isDigit(inputString.charAt(stringIndex));
    int i = stringIndex + 1;
    while (i < inputString.length()) {
      if (!Character.isDigit(inputString.charAt(i)))
        break;
      ++i;
    }
    consumeMultipleCharactersNoCheck(i - stringIndex);
    return makeToken(
        tokenPosition,
        TokenType.DECIMAL_LITERAL,
        inputString.substring(
            tokenPosition.offset(),
            stringIndex
        )
    );
  }

  private boolean isValidHexDigit(char hexDigit) {
    return Character.isDigit(hexDigit) || ((hexDigit <= 'f') && (hexDigit >= 'a')) ||
        ((hexDigit <= 'F') && (hexDigit >= 'A'));
  }

  private Token handleHexLiteral(TokenPosition tokenPosition) {
    consumeCompoundCharacter(HEX_PREFIX_LOWERCASE);

    int i = stringIndex;
    while (i < inputString.length()) {
      final char hexDigit = inputString.charAt(i);
      if (!isValidHexDigit(hexDigit)) {
        break;
      }
      ++i;
    }
    if (i == stringIndex)
      throw new IllegalStateException("<hex_literal> != <hex_digit> <hex_digit>*");
    consumeMultipleCharactersNoCheck(i - stringIndex);
    return makeToken(
        tokenPosition,
        TokenType.HEX_LITERAL,
        inputString.substring(
            tokenPosition.offset(),
            i
        )
    );
  }

  private Token handleSingleOperator(
      TokenPosition tokenPosition,
      TokenType tokenType,
      String lexeme
  ) {
    consumeCharacterNoCheck();
    return makeToken(
        tokenPosition,
        tokenType,
        lexeme
    );
  }

  private Token handleCompoundOperator(
      TokenPosition tokenPosition,
      TokenType tokenType,
      String lexeme
  ) {
    consumeMultipleCharactersNoCheck(lexeme.length());
    return makeToken(
        tokenPosition,
        tokenType,
        lexeme
    );
  }

  private Token makeToken(
      TokenPosition tokenPosition,
      TokenType tokenType,
      String lexeme
  ) {
    return new Token(
        tokenPosition,
        tokenType,
        lexeme
    );
  }

  private ScannerError recordScannerError(
      ScannerError.ErrorType errorType,
      String detail,
      TokenPosition tokenPosition
  ) {
    var error = new ScannerError(
        tokenPosition,
        errorType,
        detail
    );
    errors.add(error);
    if (getCurrentChar() == '\n') {
      consumeNewlineCharacter(NEW_LINE);
    } else {
      consumeCharacterNoCheck();
    }
    return error;
  }

  private Token handleWhiteSpace(TokenPosition tokenPosition) {
    final char c = inputString.charAt(stringIndex);

    switch (c) {
      case '\r', '\n' -> consumeNewlineCharacter(String.valueOf(c));
      case ' ', '\t' -> consumeCharacterNoCheck();
      default -> recordScannerError(
          ScannerError.ErrorType.INVALID_WHITESPACE,
          String.format(
              "found invalid whitespace %s",
              c
          ),
          tokenPosition
      );
    }
    return makeToken(
        tokenPosition,
        TokenType.WHITESPACE,
        String.valueOf(c)
    );
  }

  /**
   * Whether to display debug information.
   */
  public void setTrace(boolean shouldTrace) {
    this.shouldTrace = shouldTrace;
  }

  private void updateHighlighter(Token token) {
    switch (token.tokenType()) {
      case EOF -> {
      }
      case ID -> syntaxLines.add(Utils.coloredPrint(
          token.lexeme(),
          Utils.ANSIColorConstants.ANSI_BLUE
      ));
      case CHAR_LITERAL, STRING_LITERAL, RESERVED_FALSE, RESERVED_TRUE, HEX_LITERAL, DECIMAL_LITERAL ->
          syntaxLines.add(Utils.coloredPrint(
              token.lexeme(),
              Utils.ANSIColorConstants.ANSI_GREEN
          ));
      default -> {
        if (token.tokenType()
                 .toString()
                 .startsWith("RESERVED")) {
          syntaxLines.add(Utils.coloredPrint(
              token.lexeme(),
              Utils.ANSIColorConstants.ANSI_PURPLE
          ));
        } else {
          syntaxLines.add(token.lexeme());
        }
      }
    }
  }
}
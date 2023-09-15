package decaf.analysis.lexical;

import decaf.analysis.Token;
import decaf.analysis.TokenPosition;
import decaf.shared.CompilationContext;
import decaf.shared.errors.ScannerError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Function;
import java.util.regex.Pattern;

import static decaf.shared.Utils.escapeMetaCharacters;

public class Scanner implements Iterable<Token> {
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

    public static final Pattern INT_LITERAL_REGEX = Pattern.compile(
            "(0[xX](?:_?[0-9a-fA-F])+)|(?:0(?:_?0)*|[1-9](?:_?[0-9])*)|0[bB](?:_?[01])+|(0[oO](?:_?[0-7])+)");

    private final CompilationContext context;
    private final String sourceCode;
    private int column;
    private int line;
    private int stringIndex;
    private Token prevToken = null;

    public Scanner(String in, CompilationContext context) {
        sourceCode = maybeAppendNewLineCharacter(in);
        stringIndex = 0;
        this.context = context;
    }

    private static boolean isValidIdFirstCodePoint(char c) {
        return Character.isLetter(c) || c == '_';
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
        return sourceCode.charAt(stringIndex);
    }

    private void consumeNewlineCharacter(String sc) {
        final char c = sc.charAt(0);
        if (c != '\r' && c != '\n') {
            logScanningError(ScannerError.ErrorType.INVALID_NEWLINE,
                             String.format("expected a newline character, one of: `%s` received `%s`",
                                           String.valueOf(new char[]{'\r', '\n'}),
                                           c
                                          ),
                             new TokenPosition(line, column, stringIndex)
                            );
            stringIndex += sc.length();
        } else {
            column = 0;
            ++line;
            ++stringIndex;
        }
    }

    private void consumeCharacter(TokenPosition tokenPosition, String c, String errorMessage) {
        if (sourceCode.charAt(stringIndex) != c.charAt(0)) {
            logScanningError(ScannerError.ErrorType.INVALID_CHAR, errorMessage, tokenPosition);
        } else {
            consumeCharacterNoCheck();
        }
    }

    private void consumeCharacterSpecificDoubleQuotesError(TokenPosition tokenPosition, String errorMessage) {
        consumeCharacter(tokenPosition, Scanner.DOUBLE_QUOTES, errorMessage);
    }

    private void consumeCompoundCharacter(String compoundOp) {
        if (!sourceCode.startsWith(compoundOp, stringIndex)) {
            throw new IllegalArgumentException("expected `" +
                                               compoundOp +
                                               "`received " +
                                               sourceCode.substring(stringIndex, stringIndex + 2));
        }
        column += 2;
        stringIndex += 2;
    }

    private void consumeCharacter(char c, Function<Character, Boolean> validator) {
      if (!validator.apply(sourceCode.charAt(stringIndex))) {
        throw new IllegalArgumentException("expected " + c + " to be accepted by validator" + validator);
      }
        consumeCharacterNoCheck();
    }

    private boolean isSkipAble(Token token) {
        return token.type == Token.Type.WHITESPACE ||
               token.type == Token.Type.LINE_COMMENT ||
               token.type == Token.Type.BLOCK_COMMENT ||
               token.type == Token.Type.ERROR;
    }

    private boolean currentSubstringMatches(String s) {
        return sourceCode.startsWith(s, stringIndex);
    }

    private Token nextTokenHelper() {
        if (stringIndex >= sourceCode.length()) {
          if (prevToken != null) {
            return makeToken(prevToken.tokenPosition, Token.Type.EOF, EOF);
          } else {
            return makeToken(new TokenPosition(line, column, stringIndex), Token.Type.EOF, EOF);
          }
        }
        final char c = sourceCode.charAt(stringIndex);
        final TokenPosition tokenPosition = new TokenPosition(line, column, stringIndex);
        switch (String.valueOf(c)) {
            case LEFT_CURLY -> {
                return handleSingleOperator(tokenPosition, Token.Type.LEFT_CURLY, LEFT_CURLY);
            }
            case RIGHT_CURLY -> {
                return handleSingleOperator(tokenPosition, Token.Type.RIGHT_CURLY, RIGHT_CURLY);
            }
            case LEFT_PARENTHESIS -> {
                return handleSingleOperator(tokenPosition, Token.Type.LEFT_PARENTHESIS, LEFT_PARENTHESIS);
            }
            case RIGHT_PARENTHESIS -> {
                return handleSingleOperator(tokenPosition, Token.Type.RIGHT_PARENTHESIS, RIGHT_PARENTHESIS);
            }
            case LEFT_SQUARE_BRACKET -> {
                return handleSingleOperator(tokenPosition, Token.Type.LEFT_SQUARE_BRACKET, LEFT_SQUARE_BRACKET);
            }
            case RIGHT_SQUARE_BRACKET -> {
                return handleSingleOperator(tokenPosition, Token.Type.RIGHT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET);
            }
            case SEMICOLON -> {
                return handleSingleOperator(tokenPosition, Token.Type.SEMICOLON, SEMICOLON);
            }
            case COMMA -> {
                return handleSingleOperator(tokenPosition, Token.Type.COMMA, COMMA);
            }
            case MOD -> {
                return handleSingleOperator(tokenPosition, Token.Type.MOD, MOD);
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
              if (currentSubstringMatches(LINE_COMMENT_START)) {
                return handleSingleLineComment(tokenPosition);
              } else if (currentSubstringMatches(BLOCK_COMMENT_START)) {
                return handleBlockComment(tokenPosition);
              } else if (isValidIdFirstCodePoint(c)) {
                return handleId(tokenPosition);
              } else if (INT_LITERAL_REGEX.matcher(sourceCode.substring(stringIndex)).lookingAt()) {
                return INT_LITERAL_REGEX.matcher(sourceCode.substring(stringIndex))
                                        .results()
                                        .findFirst()
                                        .map(matchResult -> {
                                          consumeMultipleCharactersNoCheck(matchResult.group().length());
                                          return makeToken(tokenPosition, Token.Type.INT_LITERAL, matchResult.group());
                                        })
                                        .orElseThrow();
              } else if (currentSubstringMatches(ADD_ASSIGN)) {
                return handleCompoundOperator(tokenPosition, Token.Type.ADD_ASSIGN, ADD_ASSIGN);
              } else if (currentSubstringMatches(MINUS_ASSIGN)) {
                return handleCompoundOperator(tokenPosition, Token.Type.MINUS_ASSIGN, MINUS_ASSIGN);
              } else if (currentSubstringMatches(MULTIPLY_ASSIGN)) {
                return handleCompoundOperator(tokenPosition, Token.Type.MULTIPLY_ASSIGN, MULTIPLY_ASSIGN);
              } else if (currentSubstringMatches(NEQ)) {
                return handleCompoundOperator(tokenPosition, Token.Type.NEQ, NEQ);
              } else if (currentSubstringMatches(GEQ)) {
                return handleCompoundOperator(tokenPosition, Token.Type.GEQ, GEQ);
              } else if (currentSubstringMatches(LEQ)) {
                return handleCompoundOperator(tokenPosition, Token.Type.LEQ, LEQ);
              } else if (currentSubstringMatches(EQ)) {
                return handleCompoundOperator(tokenPosition, Token.Type.EQ, EQ);
              } else if (currentSubstringMatches(INCREMENT)) {
                return handleCompoundOperator(tokenPosition, Token.Type.INCREMENT, INCREMENT);
              } else if (currentSubstringMatches(DECREMENT)) {
                return handleCompoundOperator(tokenPosition, Token.Type.DECREMENT, DECREMENT);
              } else if (currentSubstringMatches(CONDITIONAL_OR)) {
                return handleCompoundOperator(tokenPosition, Token.Type.CONDITIONAL_OR, CONDITIONAL_OR);
              } else if (currentSubstringMatches(CONDITIONAL_AND)) {
                return handleCompoundOperator(tokenPosition, Token.Type.CONDITIONAL_AND, CONDITIONAL_AND);
              } else {
                return switch (String.valueOf(c)) {
                  case NOT -> handleSingleOperator(tokenPosition, Token.Type.NOT, NOT);
                  case PLUS -> handleSingleOperator(tokenPosition, Token.Type.PLUS, PLUS);
                  case MINUS -> handleSingleOperator(tokenPosition, Token.Type.MINUS, MINUS);
                  case MULTIPLY -> handleSingleOperator(tokenPosition, Token.Type.MULTIPLY, MULTIPLY);
                  case DIVIDE -> handleSingleOperator(tokenPosition, Token.Type.DIVIDE, DIVIDE);
                  case ASSIGN -> handleSingleOperator(tokenPosition, Token.Type.ASSIGN, ASSIGN);
                  case LT -> handleSingleOperator(tokenPosition, Token.Type.LT, LT);
                  case GT -> handleSingleOperator(tokenPosition, Token.Type.GT, GT);
                  case TERNARY_QUESTION_MARK ->
                          handleSingleOperator(tokenPosition, Token.Type.TERNARY_QUESTION_MARK, TERNARY_QUESTION_MARK);
                  case TERNARY_COLON -> handleSingleOperator(tokenPosition, Token.Type.TERNARY_COLON, TERNARY_COLON);
                  default -> {
                    logScanningError(ScannerError.ErrorType.INVALID_CHAR,
                                     String.format("expected a valid character but received `%s`",
                                                   escapeMetaCharacters(String.valueOf(c))
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

        final int posEndComment = sourceCode.indexOf(NEW_LINE, stringIndex);
        assert posEndComment != -1;

        consumeMultipleCharactersNoCheck(posEndComment - tokenPosition.offset() - 2);
        consumeNewlineCharacter(NEW_LINE);

        return makeToken(tokenPosition,
                         Token.Type.LINE_COMMENT,
                         sourceCode.substring(tokenPosition.offset(), stringIndex - 1)
                        );
    }

    private Token handleBlockComment(TokenPosition tokenPosition) {
        consumeCompoundCharacter(BLOCK_COMMENT_START);

        int i = stringIndex;

        final int waitingForStar = 0;
        final int waitingForForwardSlash = 1;
        final int completed = 2;

        int state = waitingForStar;
        while (i < sourceCode.length() && state != completed) {
            final char c = sourceCode.charAt(i);

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
            logScanningError(ScannerError.ErrorType.INVALID_COMMENT,
                             "could not finish parsing the comment" +
                             sourceCode.substring(tokenPosition.offset(),
                                                  Math.min(sourceCode.length(), tokenPosition.offset() + 10)
                                                 ) +
                             "...",
                             tokenPosition
                            );
        }
        return makeToken(tokenPosition, Token.Type.BLOCK_COMMENT, sourceCode.substring(tokenPosition.offset(), i));
    }

    private boolean handleEscape(TokenPosition tokenPosition) {
        consumeCharacterNoCheck();
        char c = sourceCode.charAt(stringIndex);
        switch (c) {
            case 'n', '"', 't', 'r', '\'', '\\' -> {
                consumeCharacterNoCheck();
                return false;
            }
            default -> logScanningError(ScannerError.ErrorType.INVALID_ESCAPE_SEQUENCE,
                                        String.format("found invalid escape sequence `\\%s`", c),
                                        tokenPosition
                                       );
        }
        return true;
    }

    private boolean isValidChar(char c) {
      if (c >= ' ' && c <= '!') {
        return true;
      }
      if (c >= '#' && c <= '&') {
        return true;
      }
      if (c >= '(' && c <= '[') {
        return true;
      }
        return c >= ']' && c <= '~';
    }

    private Token handleCharLiteral(TokenPosition tokenPosition) {
        consumeCharacterNoCheck();

        final char c = sourceCode.charAt(stringIndex);
        if (c == '\\' || isValidChar(c)) {
            if (c == '\\') {
                if (handleEscape(tokenPosition)) {
                    return logScanningError(ScannerError.ErrorType.INVALID_ESCAPE_SEQUENCE,
                                            String.format("found invalid char literal `%s`",
                                                          escapeMetaCharacters(String.valueOf(sourceCode.charAt(
                                                                  stringIndex)))
                                                         ),
                                            tokenPosition
                                           );
                }
            } else if (isValidChar(c)) {
                consumeCharacter(c, this::isValidChar);
            }
            consumeCharacter(tokenPosition,
                             SINGLE_QUOTES,
                             String.format("missing closing single quotes on `%s`",
                                           escapeMetaCharacters(sourceCode.substring(tokenPosition.offset(),
                                                                                     stringIndex + 1
                                                                                    ))
                                          )
                            );
            return makeToken(tokenPosition,
                             Token.Type.CHAR_LITERAL,
                             sourceCode.substring(tokenPosition.offset(), stringIndex)
                            );
        } else {
            return logScanningError(ScannerError.ErrorType.INVALID_CHAR_LITERAL,
                                    String.format("found invalid char literal `%s`",
                                                  escapeMetaCharacters(String.valueOf(c))
                                                 ),
                                    tokenPosition
                                   );
        }
    }

    private Token handleStringLiteral(TokenPosition tokenPosition) {
        consumeCharacterNoCheck();

        char c;
        while (true) {
            c = sourceCode.charAt(stringIndex);
            if (c == '\\') {
                if (handleEscape(tokenPosition)) {
                    return logScanningError(ScannerError.ErrorType.INVALID_ESCAPE_SEQUENCE,
                                            String.format("found invalid string literal \\%s",
                                                          sourceCode.charAt(stringIndex)
                                                         ),
                                            tokenPosition
                                           );
                }
            } else if (isValidChar(c)) {
              consumeCharacter(c, this::isValidChar);
            } else {
              break;
            }
        }
        consumeCharacterSpecificDoubleQuotesError(tokenPosition,
                                                  String.format("expected %s to close string literal, not `%s`",
                                                                DOUBLE_QUOTES,
                                                                escapeMetaCharacters(String.valueOf(c))
                                                               )
                                                 );
        return makeToken(tokenPosition,
                         Token.Type.STRING_LITERAL,
                         sourceCode.substring(tokenPosition.offset(), stringIndex)
                        );
    }

    private Token handleId(TokenPosition tokenPosition) {
        int i = stringIndex;
        while (i < sourceCode.length()) {
            final char alphaNum = sourceCode.charAt(i);
          if (!(Character.isLetterOrDigit(alphaNum) || alphaNum == '_')) {
            break;
          }
            ++i;
        }
        String idLexeme = sourceCode.substring(stringIndex, i);
        Token.Type type;
        switch (idLexeme) {
            case RESERVED_FOR -> type = Token.Type.RESERVED_FOR;
            case RESERVED_IF -> type = Token.Type.RESERVED_IF;
            case RESERVED_IMPORT -> type = Token.Type.RESERVED_IMPORT;
            case RESERVED_INT -> type = Token.Type.RESERVED_INT;
            case RESERVED_LEN -> type = Token.Type.RESERVED_LEN;
            case RESERVED_RETURN -> type = Token.Type.RESERVED_RETURN;
            case RESERVED_ELSE -> type = Token.Type.RESERVED_ELSE;
            case RESERVED_CONTINUE -> type = Token.Type.RESERVED_CONTINUE;
            case RESERVED_BREAK -> type = Token.Type.RESERVED_BREAK;
            case RESERVED_WHILE -> type = Token.Type.RESERVED_WHILE;
            case RESERVED_VOID -> type = Token.Type.RESERVED_VOID;
            case RESERVED_BOOL -> type = Token.Type.RESERVED_BOOL;
            case RESERVED_TRUE -> type = Token.Type.RESERVED_TRUE;
            case RESERVED_FALSE -> type = Token.Type.RESERVED_FALSE;
            default -> type = Token.Type.ID;
        }

        consumeMultipleCharactersNoCheck(i - stringIndex);
        return makeToken(tokenPosition, type, idLexeme);
    }

    private void consumeMultipleCharactersNoCheck(int nChars) {
        stringIndex += nChars;
        column += nChars;
    }

    private Token handleSingleOperator(TokenPosition tokenPosition, Token.Type type, String lexeme) {
        consumeCharacterNoCheck();
        return makeToken(tokenPosition, type, lexeme);
    }

    private Token handleCompoundOperator(TokenPosition tokenPosition, Token.Type type, String lexeme) {
        consumeMultipleCharactersNoCheck(lexeme.length());
        return makeToken(tokenPosition, type, lexeme);
    }

    private Token makeToken(TokenPosition tokenPosition, Token.Type type, String lexeme) {
        return new Token(tokenPosition, type, lexeme);
    }

    private ScannerError logScanningError(ScannerError.ErrorType errorType,
                                          String detail,
                                          TokenPosition tokenPosition) {
        var error = context.logScannerError(tokenPosition, errorType, detail);
        if (getCurrentChar() == '\n') {
            consumeNewlineCharacter(NEW_LINE);
        } else {
            consumeCharacterNoCheck();
        }
        return error;
    }

    private Token handleWhiteSpace(TokenPosition tokenPosition) {
        final char c = sourceCode.charAt(stringIndex);

        switch (c) {
            case '\r', '\n' -> consumeNewlineCharacter(String.valueOf(c));
            case ' ', '\t' -> consumeCharacterNoCheck();
            default -> logScanningError(ScannerError.ErrorType.INVALID_WHITESPACE,
                                        String.format("found invalid whitespace %s", c),
                                        tokenPosition
                                       );
        }
        return makeToken(tokenPosition, Token.Type.WHITESPACE, String.valueOf(c));
    }

    @NotNull
    @Override
    public Iterator<Token> iterator() {
        return new TokensIterator();
    }

    class TokensIterator implements Iterator<Token> {
        private @Nullable Token token = nextHelper();

        private Token nextHelper() {
            Token token;
            do {
                token = nextTokenHelper();
              if (!isSkipAble(token)) {
                prevToken = token;
              }
            } while (isSkipAble(token));
            return token;
        }

        public Token next() {
            final Token token = this.token;
          if (token == null) {
            throw new IllegalStateException("no more tokens");
          } else if (token.type == Token.Type.EOF) {
            this.token = null;
          } else {
            this.token = nextHelper();
          }
            return token;
        }

        @Override
        public boolean hasNext() {
            return token != null;
        }
    }
}
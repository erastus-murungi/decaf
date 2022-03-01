// YOU CAN MODIFY ANYTHING IN THIS FILE, THIS IS JUST A SUGGESTED CLASS

package edu.mit.compilers.grammar;

import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.exceptions.DecafScannerException;
import edu.mit.compilers.utils.StringDef;
import edu.mit.compilers.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.annotation.RetentionPolicy.SOURCE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class DecafScanner {
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


    private final String inputString;
    private final Logger logger = Logger.getLogger(DecafScanner.class.getName());
    private final List<String> syntaxLines = new ArrayList<>();
    private int column;
    private int line;
    private int stringIndex;
    private Token prevToken = null;
    private boolean shouldTrace = false;

    public DecafScanner(InputStream inputStream) {
        String str;
        try {
            str = new String(inputStream.readAllBytes(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            str = "";
        }
        inputString = maybeAppendNewLineCharacter(str);
        stringIndex = 0;
        logger.setLevel(Level.INFO);
    }


    public DecafScanner(String in) {
        inputString = maybeAppendNewLineCharacter(in);
        stringIndex = 0;
        logger.setLevel(Level.INFO);
    }

    private static boolean isValidIdFirstCodePoint(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private String maybeAppendNewLineCharacter(String string) {
        if (string.charAt(string.length() - 1) != NEW_LINE.charAt(0)) {
            logger.info("No newline character found in line. Will add one.");
            return string + NEW_LINE;
        }
        return string;
    }

    private void consumeCharacterNoCheck() {
        ++stringIndex;
        ++column;
    }

    private void consumeNewlineCharacter(String sc) {
        final char c = sc.charAt(0);
        if (c != '\r' && c != '\n') {
            throw new IllegalArgumentException("supported new line characters are: " + "[" + "\r" + ", " + "\n" + "]" + "not " + c);
        }
        column = 0;
        ++line;
        ++stringIndex;
    }

    private void consumeCharacter(TokenPosition tokenPosition, String c, String errMessage) throws DecafScannerException {
        if (inputString.charAt(stringIndex) != c.charAt(0))
            throw getContextualException(tokenPosition, errMessage);
        consumeCharacterNoCheck();
    }

    private void consumeCompoundCharacter(String compoundOp) {
        if (!inputString.startsWith(compoundOp, stringIndex)) {
            throw new IllegalArgumentException("expected " + compoundOp + "received " + inputString.substring(stringIndex, stringIndex + 2));
        }
        column += 2;
        stringIndex += 2;
    }

    private void consumeCharacter(char c, Function<Character, Boolean> validator) {
        if (!validator.apply(inputString.charAt(stringIndex)))
            throw new IllegalArgumentException("expected " + c + " to be accepted by validator" + validator);
        consumeCharacterNoCheck();
    }

    private boolean isSkipAble(Token token) {
        return token.tokenType() == TokenType.WHITESPACE || token.tokenType() == TokenType.LINE_COMMENT || token.tokenType() == TokenType.BLOCK_COMMENT;
    }

    /**
     * might find this useful
     **/
    public Token nextToken() throws DecafException {
        Token token;
        do {
            token = nextTokenHelper();
            if (!isSkipAble(token))
                prevToken = token;
            if (shouldTrace) {
                updateHighlighter(token);
                System.out.println(token);
            }
        } while (isSkipAble(token));
        if (shouldTrace)
            syntaxHighlight(System.out);
        return token;
    }

    private boolean currentSubstringMatches(String s) {
        return inputString.startsWith(s, stringIndex);
    }

    private Token nextTokenHelper() throws DecafException {
        if (stringIndex >= inputString.length()) {
            if (prevToken != null)
                return makeToken(prevToken.tokenPosition(), TokenType.EOF, EOF);
            else
                return makeToken(new TokenPosition(line, column, stringIndex), TokenType.EOF, EOF);
        }
        final char c = inputString.charAt(stringIndex);
        final TokenPosition tokenPosition = new TokenPosition(line, column, stringIndex);
        return switch (String.valueOf(c)) {
            case LEFT_CURLY -> handleSingleOperator(tokenPosition, TokenType.LEFT_CURLY, LEFT_CURLY);
            case RIGHT_CURLY -> handleSingleOperator(tokenPosition, TokenType.RIGHT_CURLY, RIGHT_CURLY);
            case LEFT_PARENTHESIS -> handleSingleOperator(tokenPosition, TokenType.LEFT_PARENTHESIS, LEFT_PARENTHESIS);
            case RIGHT_PARENTHESIS -> handleSingleOperator(tokenPosition, TokenType.RIGHT_PARENTHESIS, RIGHT_PARENTHESIS);
            case LEFT_SQUARE_BRACKET -> handleSingleOperator(tokenPosition, TokenType.LEFT_SQUARE_BRACKET, LEFT_SQUARE_BRACKET);
            case RIGHT_SQUARE_BRACKET -> handleSingleOperator(tokenPosition, TokenType.RIGHT_SQUARE_BRACKET, RIGHT_SQUARE_BRACKET);
            case SEMICOLON -> handleSingleOperator(tokenPosition, TokenType.SEMICOLON, SEMICOLON);
            case COMMA -> handleSingleOperator(tokenPosition, TokenType.COMMA, COMMA);
            case MOD -> handleSingleOperator(tokenPosition, TokenType.MOD, MOD);
            case DOUBLE_QUOTES -> handleStringLiteral(tokenPosition);
            case SINGLE_QUOTES -> handleCharLiteral(tokenPosition);
            case " ", "\t", "\n", "\r" -> handleWhiteSpace(tokenPosition);
            default -> {
                if (currentSubstringMatches(LINE_COMMENT_START))
                    yield handleSingleLineComment(tokenPosition);
                else if (currentSubstringMatches(BLOCK_COMMENT_START))
                    yield handleBlockComment(tokenPosition);
                else if (isValidIdFirstCodePoint(c))
                    yield handleId(tokenPosition);
                else if (currentSubstringMatches(HEX_PREFIX_LOWERCASE))
                    yield handleHexLiteral(tokenPosition);
                else if (Character.isDigit(c))
                    yield handleDecimalLiteral(tokenPosition);
                else if (currentSubstringMatches(ADD_ASSIGN))
                    yield handleCompoundOperator(tokenPosition, TokenType.ADD_ASSIGN, ADD_ASSIGN);
                else if (currentSubstringMatches(MINUS_ASSIGN))
                    yield handleCompoundOperator(tokenPosition, TokenType.MINUS_ASSIGN, MINUS_ASSIGN);
                else if (currentSubstringMatches(MULTIPLY_ASSIGN))
                    yield handleCompoundOperator(tokenPosition, TokenType.MULTIPLY_ASSIGN, MULTIPLY_ASSIGN);
                else if (currentSubstringMatches(NEQ))
                    yield handleCompoundOperator(tokenPosition, TokenType.NEQ, NEQ);
                else if (currentSubstringMatches(GEQ))
                    yield handleCompoundOperator(tokenPosition, TokenType.GEQ, GEQ);
                else if (currentSubstringMatches(LEQ))
                    yield handleCompoundOperator(tokenPosition, TokenType.LEQ, LEQ);
                else if (currentSubstringMatches(EQ))
                    yield handleCompoundOperator(tokenPosition, TokenType.EQ, EQ);
                else if (currentSubstringMatches(INCREMENT))
                    yield handleCompoundOperator(tokenPosition, TokenType.INCREMENT, INCREMENT);
                else if (currentSubstringMatches(DECREMENT))
                    yield handleCompoundOperator(tokenPosition, TokenType.DECREMENT, DECREMENT);
                else if (currentSubstringMatches(CONDITIONAL_OR))
                    yield handleCompoundOperator(tokenPosition, TokenType.CONDITIONAL_OR, CONDITIONAL_OR);
                else if (currentSubstringMatches(CONDITIONAL_AND))
                    yield handleCompoundOperator(tokenPosition, TokenType.CONDITIONAL_AND, CONDITIONAL_AND);
                else {
                    yield switch (String.valueOf(c)) {
                        case NOT -> handleSingleOperator(tokenPosition, TokenType.NOT, NOT);
                        case PLUS -> handleSingleOperator(tokenPosition, TokenType.PLUS, PLUS);
                        case MINUS -> handleSingleOperator(tokenPosition, TokenType.MINUS, MINUS);
                        case MULTIPLY -> handleSingleOperator(tokenPosition, TokenType.MULTIPLY, MULTIPLY);
                        case DIVIDE -> handleSingleOperator(tokenPosition, TokenType.DIVIDE, DIVIDE);
                        case ASSIGN -> handleSingleOperator(tokenPosition, TokenType.ASSIGN, ASSIGN);
                        case LT -> handleSingleOperator(tokenPosition, TokenType.LT, LT);
                        case GT -> handleSingleOperator(tokenPosition, TokenType.GT, GT);
                        case TERNARY_QUESTION_MARK -> handleSingleOperator(tokenPosition, TokenType.TERNARY_QUESTION_MARK, TERNARY_QUESTION_MARK);
                        case TERNARY_COLON -> handleSingleOperator(tokenPosition, TokenType.TERNARY_COLON, TERNARY_COLON);
                        default -> throw getContextualException(tokenPosition, "unrecognized character " + c);
                    };
                }
            }
        };
    }

    private Token handleSingleLineComment(TokenPosition tokenPosition) {
        consumeCompoundCharacter(LINE_COMMENT_START);

        final int posEndComment = inputString.indexOf(NEW_LINE, stringIndex);
        assert posEndComment != -1;

        consumeMultipleCharactersNoCheck(posEndComment - tokenPosition.offset() - 2);
        consumeNewlineCharacter(NEW_LINE);

        return makeToken(tokenPosition, TokenType.LINE_COMMENT, inputString.substring(tokenPosition.offset(), stringIndex - 1));
    }

    private Token handleBlockComment(TokenPosition tokenPosition) throws DecafException {
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
            throw getContextualException(tokenPosition, "could not finish parsing the comment" + inputString.substring(tokenPosition.offset(), Math.min(inputString.length(), tokenPosition.offset() + 10)) + "...");
        }
        return makeToken(tokenPosition, TokenType.BLOCK_COMMENT, inputString.substring(tokenPosition.offset(), i));
    }

    private char getCurrentChar() {
        return inputString.charAt(stringIndex);
    }

    private DecafScannerException getContextualException(TokenPosition tokenPosition, String errMessage) {
        final char c = getCurrentChar();
        errMessage = switch (prevToken.tokenType()) {
            case STRING_LITERAL -> "illegal string literal: " + prevToken.lexeme() + c;
            case ID -> "illegal id: " + prevToken.lexeme() + c;
            default -> errMessage;
        };
        return new DecafScannerException(tokenPosition, getContextualErrorMessage(tokenPosition, errMessage));
    }

    private void handleEscape(TokenPosition tokenPosition) throws DecafScannerException {
        consumeCharacterNoCheck();
        char c = inputString.charAt(stringIndex);
        switch (c) {
            case 'n', '"', 't', 'r', '\'', '\\' -> consumeCharacterNoCheck();
            default -> throw getContextualException(tokenPosition, "Invalid back-slashed character \"" + "\\" + c + "\"");
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

    private Token handleCharLiteral(TokenPosition tokenPosition) throws DecafScannerException {
        consumeCharacterNoCheck();

        final char c = inputString.charAt(stringIndex);
        if (c == '\\')
            handleEscape(tokenPosition);
        else if (isValidChar(c))
            consumeCharacter(c, this::isValidChar);
        else
            throw getContextualException(tokenPosition, "invalid char literal");

        consumeCharacter(tokenPosition, SINGLE_QUOTES, getContextualErrorMessage(tokenPosition, "missing closing single quotes " + "current char literal is " + inputString.substring(tokenPosition.offset(), stringIndex + 1)));
        return makeToken(tokenPosition, TokenType.CHAR_LITERAL, inputString.substring(tokenPosition.offset(), stringIndex));
    }

    public String getContextualErrorMessage(TokenPosition tokenPosition, String errMessage) {
        final int MAX_NUM_CHARS = 30;
        final String TILDE = "~";
        final String lineToPrint = inputString.split(NEW_LINE)[tokenPosition.line()];
        final int before = Math.min(tokenPosition.column(), MAX_NUM_CHARS);
        final int after = Math.min(lineToPrint.length() - tokenPosition.column(), MAX_NUM_CHARS);

        final String inputSub = lineToPrint.substring(tokenPosition.column() - before, tokenPosition.column() + after);
        final String spaces = Utils.SPACE.repeat(String.valueOf(tokenPosition.line()).length() + String.valueOf(tokenPosition.column()).length() + 3);

        return NEW_LINE + errMessage + NEW_LINE + spaces + inputSub + NEW_LINE + tokenPosition.line() + TERNARY_COLON + tokenPosition.column() + TERNARY_COLON + Utils.SPACE + Utils.coloredPrint(TILDE.repeat(before), Utils.ANSIColorConstants.ANSI_GREEN) + Utils.coloredPrint("^", Utils.ANSIColorConstants.ANSI_CYAN) + Utils.coloredPrint(TILDE.repeat(after), Utils.ANSIColorConstants.ANSI_GREEN);
    }

    private Token handleStringLiteral(TokenPosition tokenPosition) throws DecafException {
        consumeCharacterNoCheck();

        char c;
        while (true) {
            c = inputString.charAt(stringIndex);
            if (c == '\\')
                handleEscape(tokenPosition);
            else if (isValidChar(c))
                consumeCharacter(c, this::isValidChar);
            else
                break;
        }
        consumeCharacter(tokenPosition, DOUBLE_QUOTES, getContextualErrorMessage(tokenPosition, "expected " + DOUBLE_QUOTES + " received " + inputString.charAt(stringIndex)));
        return makeToken(tokenPosition, TokenType.STRING_LITERAL, inputString.substring(tokenPosition.offset(), stringIndex));
    }

    private Token handleId(TokenPosition tokenPosition) {
        int i = stringIndex;
        while (i < inputString.length()) {
            final char alphaNum = inputString.charAt(i);
            if (!(Character.isLetterOrDigit(alphaNum) || alphaNum == '_'))
                break;
            ++i;
        }
        String idLexeme = inputString.substring(stringIndex, i);
        TokenType tokenType = switch (idLexeme) {
            case RESERVED_FOR -> TokenType.RESERVED_FOR;
            case RESERVED_IF -> TokenType.RESERVED_IF;
            case RESERVED_IMPORT -> TokenType.RESERVED_IMPORT;
            case RESERVED_INT -> TokenType.RESERVED_INT;
            case RESERVED_LEN -> TokenType.RESERVED_LEN;
            case RESERVED_RETURN -> TokenType.RESERVED_RETURN;
            case RESERVED_ELSE -> TokenType.RESERVED_ELSE;
            case RESERVED_CONTINUE -> TokenType.RESERVED_CONTINUE;
            case RESERVED_BREAK -> TokenType.RESERVED_BREAK;
            case RESERVED_WHILE -> TokenType.RESERVED_WHILE;
            case RESERVED_VOID -> TokenType.RESERVED_VOID;
            case RESERVED_BOOL -> TokenType.RESERVED_BOOL;
            case RESERVED_TRUE -> TokenType.RESERVED_TRUE;
            case RESERVED_FALSE -> TokenType.RESERVED_FALSE;
            default -> TokenType.ID;
        };

        consumeMultipleCharactersNoCheck(i - stringIndex);
        return makeToken(tokenPosition, tokenType, idLexeme);
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
        return makeToken(tokenPosition, TokenType.DECIMAL_LITERAL, inputString.substring(tokenPosition.offset(), stringIndex));
    }

    private boolean isValidHexDigit(char hexDigit) {
        return Character.isDigit(hexDigit) || ((hexDigit <= 'f') && (hexDigit >= 'a')) || ((hexDigit <= 'F') && (hexDigit >= 'A'));
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
        return makeToken(tokenPosition, TokenType.HEX_LITERAL, inputString.substring(tokenPosition.offset(), i));
    }

    private Token handleSingleOperator(TokenPosition tokenPosition, TokenType tokenType, String lexeme) {
        consumeCharacterNoCheck();
        return makeToken(tokenPosition, tokenType, lexeme);
    }

    private Token handleCompoundOperator(TokenPosition tokenPosition, TokenType tokenType, String lexeme) {
        consumeMultipleCharactersNoCheck(lexeme.length());
        return makeToken(tokenPosition, tokenType, lexeme);
    }

    private Token makeToken(TokenPosition tokenPosition, TokenType tokenType, String lexeme) {
        return new Token(tokenPosition, tokenType, lexeme);
    }

    private Token handleWhiteSpace(TokenPosition tokenPosition) throws DecafException {
        final char c = inputString.charAt(stringIndex);

        switch (c) {
            case '\r', '\n' -> consumeNewlineCharacter(String.valueOf(c));
            case ' ', '\t' -> consumeCharacterNoCheck();
            default -> throw getContextualException(tokenPosition, "unexpected whitespace char " + c);
        }
        return makeToken(tokenPosition, TokenType.WHITESPACE, String.valueOf(c));
    }

    /**
     * Whether to display debug information.
     */
    public void setTrace(boolean shouldTrace) {
        this.shouldTrace = shouldTrace;
    }

    public String getInputString() {
        return inputString;
    }

    private void updateHighlighter(Token token) {
        switch (token.tokenType()) {
            case EOF -> {
            }
            case ID -> syntaxLines.add(Utils.coloredPrint(token.lexeme(), Utils.ANSIColorConstants.ANSI_BLUE));
            case CHAR_LITERAL, STRING_LITERAL, RESERVED_FALSE, RESERVED_TRUE, HEX_LITERAL, DECIMAL_LITERAL -> syntaxLines.add(Utils.coloredPrint(token.lexeme(), Utils.ANSIColorConstants.ANSI_GREEN));
            default -> {
                if (token.tokenType().toString().startsWith("RESERVED")) {
                    syntaxLines.add(Utils.coloredPrint(token.lexeme(), Utils.ANSIColorConstants.ANSI_PURPLE));
                } else {
                    syntaxLines.add(token.lexeme());
                }
            }
        }
    }

    public void syntaxHighlight(PrintStream out) {
        for (String string : syntaxLines) {
            out.print(string);
        }
    }

    @Retention(SOURCE)
    @StringDef({PLUS, MINUS, MULTIPLY, DIVIDE, MOD})
    public @interface ArithmeticOperator {
    }

    @Retention(SOURCE)
    @StringDef({PLUS, MINUS, MULTIPLY, DIVIDE, MOD, LT, GT, GEQ, LEQ, EQ, NEQ,})
    public @interface BinaryOperator {
    }

    @Retention(SOURCE)
    @StringDef({LT, GT, GEQ, LEQ})
    public @interface RelationalOperator {
    }

    @Retention(SOURCE)
    @StringDef({EQ, NEQ,})
    public @interface EqualityOperator {
    }

    @Retention(SOURCE)
    @StringDef({CONDITIONAL_AND, CONDITIONAL_OR,})
    public @interface ConditionalOperator {
    }

    @Retention(SOURCE)
    @StringDef({MINUS, NOT})
    public @interface UnaryOperator {
    }

    @Retention(SOURCE)
    @StringDef({RESERVED_FALSE, RESERVED_TRUE})
    public @interface BooleanLiteral {
    }

    @Retention(SOURCE)
    @StringDef({ASSIGN, ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN})
    public @interface AssignOperator {
    }

    @Retention(SOURCE)
    @StringDef({ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN})
    public @interface CompoundAssignOperator {
    }

    @Retention(SOURCE)
    @StringDef({RESERVED_INT, RESERVED_BOOL, RESERVED_VOID})
    public @interface BuiltinTypes {
    }
}
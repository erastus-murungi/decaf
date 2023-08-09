package decaf.grammar;

import static decaf.grammar.TokenType.ASSIGN;
import static decaf.grammar.TokenType.CHAR_LITERAL;
import static decaf.grammar.TokenType.COMMA;
import static decaf.grammar.TokenType.CONDITIONAL_AND;
import static decaf.grammar.TokenType.CONDITIONAL_OR;
import static decaf.grammar.TokenType.DECIMAL_LITERAL;
import static decaf.grammar.TokenType.DECREMENT;
import static decaf.grammar.TokenType.DIVIDE;
import static decaf.grammar.TokenType.EOF;
import static decaf.grammar.TokenType.EQ;
import static decaf.grammar.TokenType.GEQ;
import static decaf.grammar.TokenType.GT;
import static decaf.grammar.TokenType.HEX_LITERAL;
import static decaf.grammar.TokenType.ID;
import static decaf.grammar.TokenType.INCREMENT;
import static decaf.grammar.TokenType.LEFT_CURLY;
import static decaf.grammar.TokenType.LEFT_PARENTHESIS;
import static decaf.grammar.TokenType.LEFT_SQUARE_BRACKET;
import static decaf.grammar.TokenType.LEQ;
import static decaf.grammar.TokenType.LT;
import static decaf.grammar.TokenType.MINUS;
import static decaf.grammar.TokenType.MOD;
import static decaf.grammar.TokenType.MULTIPLY;
import static decaf.grammar.TokenType.NEQ;
import static decaf.grammar.TokenType.PLUS;
import static decaf.grammar.TokenType.RESERVED_BOOL;
import static decaf.grammar.TokenType.RESERVED_BREAK;
import static decaf.grammar.TokenType.RESERVED_CONTINUE;
import static decaf.grammar.TokenType.RESERVED_ELSE;
import static decaf.grammar.TokenType.RESERVED_FALSE;
import static decaf.grammar.TokenType.RESERVED_FOR;
import static decaf.grammar.TokenType.RESERVED_IF;
import static decaf.grammar.TokenType.RESERVED_IMPORT;
import static decaf.grammar.TokenType.RESERVED_INT;
import static decaf.grammar.TokenType.RESERVED_LEN;
import static decaf.grammar.TokenType.RESERVED_RETURN;
import static decaf.grammar.TokenType.RESERVED_TRUE;
import static decaf.grammar.TokenType.RESERVED_VOID;
import static decaf.grammar.TokenType.RESERVED_WHILE;
import static decaf.grammar.TokenType.RIGHT_CURLY;
import static decaf.grammar.TokenType.RIGHT_PARENTHESIS;
import static decaf.grammar.TokenType.RIGHT_SQUARE_BRACKET;
import static decaf.grammar.TokenType.SEMICOLON;
import static decaf.grammar.TokenType.STRING_LITERAL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import decaf.ast.AST;
import decaf.ast.ArithmeticOperator;
import decaf.ast.Array;
import decaf.ast.AssignExpr;
import decaf.ast.AssignOpExpr;
import decaf.ast.AssignOperator;
import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.Break;
import decaf.ast.CharLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.CompoundAssignOperator;
import decaf.ast.ConditionalOperator;
import decaf.ast.Continue;
import decaf.ast.DecimalLiteral;
import decaf.ast.Decrement;
import decaf.ast.EqualityOperator;
import decaf.ast.ExprContext;
import decaf.ast.Expression;
import decaf.ast.ExpressionParameter;
import decaf.ast.FieldDeclaration;
import decaf.ast.For;
import decaf.ast.HexLiteral;
import decaf.ast.If;
import decaf.ast.ImportDeclaration;
import decaf.ast.Increment;
import decaf.ast.Initialization;
import decaf.ast.IntLiteral;
import decaf.ast.Len;
import decaf.ast.Literal;
import decaf.ast.Location;
import decaf.ast.LocationArray;
import decaf.ast.LocationAssignExpr;
import decaf.ast.LocationVariable;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallParameter;
import decaf.ast.MethodCallStatement;
import decaf.ast.MethodDefinition;
import decaf.ast.MethodDefinitionParameter;
import decaf.ast.Name;
import decaf.ast.ParenthesizedExpression;
import decaf.ast.Program;
import decaf.ast.RelationalOperator;
import decaf.ast.Return;
import decaf.ast.Statement;
import decaf.ast.StringLiteral;
import decaf.ast.Type;
import decaf.ast.UnaryOpExpression;
import decaf.ast.UnaryOperator;
import decaf.ast.While;
import decaf.common.DecafExceptionProcessor;
import decaf.common.Pair;
import decaf.common.Utils;
import decaf.exceptions.DecafException;
import decaf.exceptions.DecafParserException;

public class DecafParser {
  public final DecafScanner scanner;
  public final List<DecafParserException> errors = new ArrayList<>();
  private final DecafExceptionProcessor decafExceptionProcessor;
  private boolean showTrace = false;
  private ArrayList<Token> tokens;
  private int currentTokenIndex;
  private Program root;

  public DecafParser(DecafScanner scanner) {
    this.scanner = scanner;
    decafExceptionProcessor = scanner.decafExceptionProcessor;
    try {
      this.tokens = getAllTokens();
    } catch (DecafException e) {
      e.printStackTrace();
      this.tokens = new ArrayList<>();
    }
    this.currentTokenIndex = 0;
  }

  private static void addTerminal(
      Pair<String, AST> labelAndNode,
      String prefix,
      String connector,
      List<String> tree
  ) {
    if (!labelAndNode.second()
                     .isTerminal())
      throw new IllegalArgumentException();
    tree.add(prefix + connector + " " + Utils.coloredPrint(
        labelAndNode.first(),
        Utils.ANSIColorConstants.ANSI_BLUE
    ) + " = " + labelAndNode.second());
  }

  private static void addNonTerminal(
      Pair<String, AST> labelAndNode,
      int index,
      int numChildren,
      String prefix,
      String connector,
      List<String> tree
  ) {
    tree.add(
        String.format(
            "%s%s %s = [%s]",
            prefix,
            connector,
            labelAndNode.first(),
            Utils.coloredPrint(
                labelAndNode.second()
                            .getSourceCode(),
                Utils.ANSIColorConstants.ANSI_PURPLE
            )
        ));
    prefix += (index != numChildren - 1) ? PrintConstants.PIPE_PREFIX: PrintConstants.SPACE_PREFIX;
    treeBody(
        labelAndNode,
        tree,
        prefix
    );
  }

  private static void treeBody(
      Pair<String, AST> parentNode,
      List<String> tree,
      String prefix
  ) {
    List<Pair<String, AST>> nodeList = parentNode.second()
                                                 .getChildren();
    for (int i = 0; i < nodeList.size(); i++) {
      final String connector = (i == nodeList.size() - 1) ? PrintConstants.ELBOW: PrintConstants.TEE;
      final Pair<String, AST> labelAndNode = nodeList.get(i);
      if (labelAndNode.second()
                      .isTerminal())
        addTerminal(
            labelAndNode,
            prefix,
            connector,
            tree
        );
      else
        addNonTerminal(
            labelAndNode,
            i,
            nodeList.size(),
            prefix,
            connector,
            tree
        );
    }
  }

  public Program getRoot() {
    return root;
  }

  public final void program() {
    final Program program = new Program();
    try {
      processImportDeclarations(program.getImportDeclarationList());
      processFieldOrMethod(program);
    } catch (DecafParserException e) {
      errors.add(e);
    }
    root = program;
    if (showTrace) {
      printParseTree();
    }
  }

  private Token getCurrentToken() {
    return tokens.get(currentTokenIndex);
  }

  private TokenType getCurrentTokenType() {
    return getCurrentToken().tokenType();
  }

  private ArrayList<Token> getAllTokens() throws DecafException {
    ArrayList<Token> tokens = new ArrayList<>();
    Token token;
    do {
      token = scanner.nextToken();
      tokens.add(token);
    } while (token.isNotEOF());
    return tokens;
  }

  private Token consumeToken(
      TokenType expectedTokenType,
      String expected
  ) throws DecafParserException {
    if (getCurrentToken().tokenType() != expectedTokenType) {
      String s = getCurrentToken().lexeme();
      if (getCurrentToken().tokenType()
                           .toString()
                           .startsWith("RESERVED"))
        s = "reserved keyword " + "\"" + s + "\"";
      throw getContextualException("expected " + "\"" + expected + "\"" + " received " + s);
    }
    Token token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
  }

  private Token consumeTokenNoCheck() {
    Token token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
  }

  private Token consumeToken(TokenType expectedTokenType) throws DecafParserException {
    return consumeToken(
        expectedTokenType,
        expectedTokenType.toString()
    );
  }

  private Token consumeToken(
      TokenType expectedTokenType,
      Function<Token, String> getErrMessage
  ) throws DecafParserException {
    if (getCurrentTokenType() != expectedTokenType) {
      final String errMessage = getErrMessage.apply(getCurrentToken());
      throw getContextualException(errMessage);
    }
    Token token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
  }

  public boolean hasError() {
    return errors.size() > 0;
  }

  private Expression parseOrExpr() throws DecafParserException {
    final Expression lhs = parseAndExpr();
    if (getCurrentTokenType() == CONDITIONAL_OR) {
      TokenPosition tokenPosition = consumeTokenNoCheck().tokenPosition();
      Expression rhs = parseOrExpr();
      return BinaryOpExpression.of(
          lhs,
          new ConditionalOperator(
              tokenPosition,
              DecafScanner.CONDITIONAL_OR
          ),
          rhs
      );
    }
    return lhs;
  }

  private Expression parseAndExpr() throws DecafParserException {
    final Expression lhs = parseEqualityExpr();
    if (getCurrentTokenType() == CONDITIONAL_AND) {
      TokenPosition tokenPosition = consumeTokenNoCheck().tokenPosition();
      Expression rhs = parseAndExpr();
      return BinaryOpExpression.of(
          lhs,
          new ConditionalOperator(
              tokenPosition,
              DecafScanner.CONDITIONAL_AND
          ),
          rhs
      );
    }
    return lhs;
  }

  private Expression parseEqualityExpr() throws DecafParserException {
    final Expression lhs = parseRelationalExpr();
    if (getCurrentTokenType() == EQ || getCurrentTokenType() == NEQ) {
      Token token = consumeTokenNoCheck();
      Expression rhs = parseEqualityExpr();
      if (token.tokenType() == EQ) {
        return BinaryOpExpression.of(
            lhs,
            new EqualityOperator(
                token.tokenPosition(),
                DecafScanner.EQ
            ),
            rhs
        );
      } else {
        return BinaryOpExpression.of(
            lhs,
            new EqualityOperator(
                token.tokenPosition(),
                DecafScanner.NEQ
            ),
            rhs
        );
      }
    }
    return lhs;
  }

  private Expression parseRelationalExpr() throws DecafParserException {
    final Expression lhs = parseAddSubExpr();
    if (getCurrentTokenType() == GT
        || getCurrentTokenType() == LT
        || getCurrentTokenType() == GEQ
        || getCurrentTokenType() == LEQ
    ) {
      Token token = consumeTokenNoCheck();
      Expression rhs = parseRelationalExpr();
      return switch (token.tokenType()) {
        case GT -> BinaryOpExpression.of(
            lhs,
            new RelationalOperator(
                token.tokenPosition(),
                DecafScanner.GT
            ),
            rhs
        );
        case LT -> BinaryOpExpression.of(
            lhs,
            new RelationalOperator(
                token.tokenPosition(),
                DecafScanner.LT
            ),
            rhs
        );
        case LEQ -> BinaryOpExpression.of(
            lhs,
            new RelationalOperator(
                token.tokenPosition(),
                DecafScanner.LEQ
            ),
            rhs
        );
        default -> BinaryOpExpression.of(
            lhs,
            new RelationalOperator(
                token.tokenPosition(),
                DecafScanner.GEQ
            ),
            rhs
        );
      };
    }
    return lhs;
  }

  private Expression parseAddSubExpr() throws DecafParserException {
    final Expression lhs = parseMulDivRemExpr();
    if (getCurrentTokenType() == PLUS || getCurrentTokenType() == MINUS) {
      Token token = consumeTokenNoCheck();
      Expression rhs = parseEqualityExpr();
      if (token.tokenType() == PLUS) {
        return BinaryOpExpression.of(
            lhs,
            new ArithmeticOperator(
                token.tokenPosition(),
                DecafScanner.PLUS
            ),
            rhs
        );
      } else {
        return BinaryOpExpression.of(
            lhs,
            new ArithmeticOperator(
                token.tokenPosition(),
                DecafScanner.MINUS
            ),
            rhs
        );
      }
    }
    return lhs;
  }

  private Expression parseMulDivRemExpr() throws DecafParserException {
    final Expression lhs = parseExpr();
    if (getCurrentTokenType() == MOD
        || getCurrentTokenType() == MULTIPLY
        || getCurrentTokenType() == DIVIDE
    ) {
      Token token = consumeTokenNoCheck();
      Expression rhs = parseMulDivRemExpr();
      return switch (token.tokenType()) {
        case MOD -> BinaryOpExpression.of(
            lhs,
            new ArithmeticOperator(
                token.tokenPosition(),
                DecafScanner.MOD
            ),
            rhs
        );
        case MULTIPLY -> BinaryOpExpression.of(
            lhs,
            new ArithmeticOperator(
                token.tokenPosition(),
                DecafScanner.MULTIPLY
            ),
            rhs
        );
        default -> BinaryOpExpression.of(
            lhs,
            new ArithmeticOperator(
                token.tokenPosition(),
                DecafScanner.DIVIDE
            ),
            rhs
        );
      };
    }
    return lhs;
  }

  private Expression parseUnaryOpExpr() throws DecafParserException {
    final Token unaryOpToken = consumeTokenNoCheck();
    return new UnaryOpExpression(
        new UnaryOperator(
            unaryOpToken.tokenPosition(),
            unaryOpToken.lexeme()
        ),
        parseExpr()
    );
  }

  private Expression parseParenthesizedExpression() throws DecafParserException {
    Token token = consumeTokenNoCheck();
    Expression expr = parseOrExpr();
    consumeToken(
        RIGHT_PARENTHESIS,
        DecafScanner.RIGHT_PARENTHESIS
    );
    return new ParenthesizedExpression(
        token.tokenPosition(),
        expr
    );
  }

  private Expression parseLocationOrMethodCall() throws DecafParserException {
    Token token = consumeToken(ID);
    Name nameId = new Name(
        token.lexeme(),
        token.tokenPosition(),
        ExprContext.LOAD
    );
    if (getCurrentTokenType() == LEFT_SQUARE_BRACKET) {
      consumeTokenNoCheck();
      Expression expr = parseOrExpr();
      consumeToken(RIGHT_SQUARE_BRACKET);
      return new LocationArray(
          nameId,
          expr
      );
    } else if (getCurrentTokenType() == LEFT_PARENTHESIS) {
      return parseMethodCall(token);
    } else {
      return new LocationVariable(nameId);
    }
  }

  private void processImportDeclarations(List<ImportDeclaration> importDeclarationList) throws DecafParserException {
    if (getCurrentToken().tokenType() == RESERVED_IMPORT)
      importDeclarationList.add(parseImportDeclaration());
    if (getCurrentToken().tokenType() == RESERVED_IMPORT) {
      processImportDeclarations(importDeclarationList);
    }
  }

  private void parseFieldDeclarations(List<FieldDeclaration> fieldDeclarationList) throws DecafParserException {
    if (getCurrentToken().tokenType() == RESERVED_BOOL || getCurrentToken().tokenType() == RESERVED_INT) {
      do {
        fieldDeclarationList.add(parseFieldDeclaration());
      } while ((getCurrentToken().tokenType() == RESERVED_BOOL || getCurrentToken().tokenType() == RESERVED_INT));
      consumeToken(
          SEMICOLON,
          (Token t) -> {
            if (t.tokenType() == ASSIGN) {
              return "initializers not allowed here";
            } else if (t.tokenType() == ID) {
              return "expected \";\" but found " + DecafScanner.IDENTIFIER +
                  " : maybe missing a comma between variables in field decl?";
            } else {
              return "expected " + DecafScanner.SEMICOLON + " received " + getCurrentTokenType().toString();
            }
          }
      );
      if ((getCurrentToken().tokenType() == RESERVED_BOOL || getCurrentToken().tokenType() == RESERVED_INT)) {
        parseFieldDeclarations(fieldDeclarationList);
      }
    }
  }

  private void parseFieldDeclarationGroup(
      List<Name> variables,
      List<Array> arrays,
      Name nameId
  ) throws DecafParserException {
    if (getCurrentToken().tokenType() == LEFT_SQUARE_BRACKET) {
      consumeToken(
          LEFT_SQUARE_BRACKET,
          DecafScanner.LEFT_SQUARE_BRACKET
      );
      final IntLiteral intLiteral = parseIntLiteral();
      consumeToken(
          RIGHT_SQUARE_BRACKET,
          DecafScanner.RIGHT_SQUARE_BRACKET
      );
      arrays.add(new Array(
          intLiteral,
          nameId
      ));
    } else {
      variables.add(nameId);
    }
  }

  private IntLiteral parseIntLiteral() throws DecafParserException {
    Token intLiteralToken;
    if (getCurrentToken().tokenType() == DECIMAL_LITERAL) {
      intLiteralToken = consumeTokenNoCheck();
      return new DecimalLiteral(
          intLiteralToken.tokenPosition(),
          intLiteralToken.lexeme()
      );
    } else if (getCurrentToken().tokenType() == HEX_LITERAL) {
      intLiteralToken = consumeTokenNoCheck();
      return new HexLiteral(
          intLiteralToken.tokenPosition(),
          intLiteralToken.lexeme()
      );
    } else {
      if (getCurrentTokenType() == RIGHT_SQUARE_BRACKET)
        throw getContextualException("missing array size");
      else
        throw getContextualException("invalid int literal");
    }
  }

  private void parseFieldDeclarationGroup(
      Program program,
      Name nameId,
      Type type,
      TokenPosition tokenPosition
  ) throws DecafParserException {
    // dealing with fieldDeclarations
    List<Name> variables = new ArrayList<>();
    List<Array> arrays = new ArrayList<>();
    parseFieldDeclarationGroup(
        variables,
        arrays,
        nameId
    );
    while (getCurrentToken().tokenType() == COMMA) {
      consumeToken(COMMA);
      nameId = parseName(
          DecafScanner.IDENTIFIER,
          ExprContext.DECLARE
      );
      parseFieldDeclarationGroup(
          variables,
          arrays,
          nameId
      );
    }
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    program.getFieldDeclarationList()
           .add(new FieldDeclaration(
               tokenPosition,
               type,
               variables,
               arrays
           ));
    processFieldOrMethod(program);
  }

  private void parseMethodDeclaration(
      List<MethodDefinition> methodDefinitionList,
      Name id,
      Type type
  ) throws DecafParserException {
    List<MethodDefinitionParameter> methodDefinitionParameterList = parseMethodArguments();
    final Block block = parseBlock();
    methodDefinitionList.add(new MethodDefinition(
        id.tokenPosition,
        type,
        methodDefinitionParameterList,
        id,
        block
    ));
    parseMethodDeclarations(methodDefinitionList);
  }

  private void processFieldOrMethod(Program program) throws DecafParserException {
    if (getCurrentTokenType() == RESERVED_INT || getCurrentTokenType() == RESERVED_BOOL) {
      // could be an int or bool
      final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
      final Type type = parseBuiltinFieldType();
      final Name id = parseName(
          DecafScanner.IDENTIFIER,
          ExprContext.DECLARE
      );
      if (getCurrentTokenType() == LEFT_PARENTHESIS) {
        // dealing with methodDeclarations
        parseMethodDeclaration(
            program.getMethodDefinitionList(),
            id,
            type
        );
      } else {
        parseFieldDeclarationGroup(
            program,
            id,
            type,
            tokenPosition
        );
      }
    } else if (getCurrentTokenType() == RESERVED_VOID) {
      parseMethodDeclarations(program.getMethodDefinitionList());
    } else {
      if (getCurrentTokenType() == ID && currentTokenIndex + 1 < tokens.size() && tokens.get(currentTokenIndex + 1)
                                                                                        .tokenType() ==
          LEFT_PARENTHESIS) {
        throw getContextualException("method \"" + getCurrentToken().lexeme() + "\" does not have a return type");
      } else if (getCurrentTokenType() == ID) {
        throw getContextualException("field \"" + getCurrentToken().lexeme() + "\" does not have a type");
      }
    }
    if (getCurrentTokenType() != EOF) {
      throw getContextualException("extra token: \"" + getCurrentToken().lexeme() + "\" found after program end");
    }
  }

  private DecafParserException getContextualException(String errMessage) {
    return decafExceptionProcessor.getContextualDecafParserException(
        getCurrentToken(),
        errMessage
    );
  }

  private void parseMethodDeclarations(List<MethodDefinition> methodDefinitionList) throws DecafParserException {
    while (getCurrentTokenType() == RESERVED_BOOL || getCurrentTokenType() == RESERVED_INT ||
        getCurrentTokenType() == RESERVED_VOID) {
      methodDefinitionList.add(parseMethodDeclaration());
    }
  }

  private Type parseMethodReturnType() throws DecafParserException {
    final Token token = consumeTokenNoCheck();
    return switch (token.tokenType()) {
      case RESERVED_BOOL -> Type.Bool;
      case RESERVED_INT -> Type.Int;
      case RESERVED_VOID -> Type.Void;
      default -> throw new DecafParserException(
          getCurrentToken(),
          "expected method return type"
      );
    };
  }

  private MethodDefinitionParameter parseMethodArgument() throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    final Type type = parseBuiltinFieldType();
    final Name nameId = parseName(
        DecafScanner.IDENTIFIER,
        ExprContext.DECLARE
    );
    return new MethodDefinitionParameter(
        tokenPosition,
        nameId.getLabel(),
        type
    );
  }

  private void parseMethodArguments(List<MethodDefinitionParameter> methodDefinitionParameterList) throws DecafParserException {
    methodDefinitionParameterList.add(parseMethodArgument());
    if (getCurrentTokenType() == COMMA) {
      consumeTokenNoCheck();
      parseMethodArguments(methodDefinitionParameterList);
    }
  }

  private List<MethodDefinitionParameter> parseMethodArguments() throws DecafParserException {
    consumeToken(
        LEFT_PARENTHESIS,
        (Token token) -> {
          if (token.tokenType() == COMMA || token.tokenType() == SEMICOLON ||
              token.tokenType() == LEFT_SQUARE_BRACKET) {
            return "field decls must be first";
          } else {
            return "invalid method decl syntax: expected " + "\"" + DecafScanner.LEFT_PARENTHESIS + "\"" +
                " received " + "\"" + token.lexeme() + "\"";
          }
        }
    );
    if (getCurrentToken().tokenType() == RESERVED_INT || getCurrentToken().tokenType() == RESERVED_BOOL) {
      List<MethodDefinitionParameter> methodDefinitionParameterList = new ArrayList<>();
      parseMethodArguments(methodDefinitionParameterList);
      consumeToken(RIGHT_PARENTHESIS);
      return methodDefinitionParameterList;
    } else if (getCurrentTokenType() != RIGHT_PARENTHESIS) {
      if (getCurrentTokenType() == ID)
        throw getContextualException(
            "method parameter " + "`" + getCurrentToken().lexeme() + "`" + " does not have a type");
      throw getContextualException("illegal method arg type: " + getCurrentToken().lexeme());
    }
    consumeToken(RIGHT_PARENTHESIS);
    return Collections.emptyList();
  }

  private MethodDefinition parseMethodDeclaration() throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    final Type methodReturnType = parseMethodReturnType();
    final Name nameId = parseName(
        "expected method to have an identifier",
        ExprContext.DECLARE
    );

    List<MethodDefinitionParameter> methodDefinitionParameterList = parseMethodArguments();

    Block block = parseBlock();

    return new MethodDefinition(
        tokenPosition,
        methodReturnType,
        methodDefinitionParameterList,
        nameId,
        block
    );
  }

  private void parseMethodCallArguments(List<MethodCallParameter> methodCallParameterList) throws DecafParserException {
    if (getCurrentTokenType() == STRING_LITERAL) {
      final Token token = consumeTokenNoCheck();
      methodCallParameterList.add(new StringLiteral(
          token.tokenPosition(),
          token.lexeme()
      ));
    } else {
      methodCallParameterList.add(new ExpressionParameter(parseOrExpr()));
    }
    if (getCurrentTokenType() == COMMA) {
      consumeToken(COMMA);
      parseMethodCallArguments(methodCallParameterList);
    }
  }

  private List<MethodCallParameter> parseMethodCallArguments() throws DecafParserException {
    if (getCurrentTokenType() == RIGHT_PARENTHESIS)
      return Collections.emptyList();
    List<MethodCallParameter> methodCallParameterList = new ArrayList<>();
    parseMethodCallArguments(methodCallParameterList);
    return methodCallParameterList;
  }

  private MethodCall parseMethodCall(Token token) throws DecafParserException {
    consumeToken(LEFT_PARENTHESIS);
    List<MethodCallParameter> methodCallParameterList = parseMethodCallArguments();
    consumeToken(RIGHT_PARENTHESIS);
    return new MethodCall(
        new Name(
            token.lexeme(),
            token.tokenPosition(),
            ExprContext.LOAD
        ),
        methodCallParameterList
    );
  }

  private Statement parseLocationAndAssignExprOrMethodCall() throws DecafParserException {
    final Token token = consumeToken(
        ID,
        DecafScanner.IDENTIFIER
    );
    Statement statement;
    if (getCurrentToken().tokenType() == LEFT_PARENTHESIS) {
      statement = new MethodCallStatement(
          token.tokenPosition(),
          parseMethodCall(token)
      );
    } else {
      statement = parseLocationAndAssignExpr(token);
    }
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    return statement;
  }

  private LocationAssignExpr parseLocationAndAssignExpr(Token token) throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    Location locationNode = parseLocation(token);
    AssignExpr assignExprNode = parseAssignExpr();
    return new LocationAssignExpr(
        tokenPosition,
        locationNode,
        assignExprNode
    );
  }

  private AssignOperator parseAssignOp(TokenType tokenType) throws DecafParserException {
    Token token = consumeToken(
        tokenType,
        tokenType.toString()
    );
    return switch (tokenType) {
      case ASSIGN, ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> new AssignOperator(
          token.tokenPosition(),
          token.lexeme()
      );
      default -> throw getContextualException("expected compound assignOp");
    };
  }

  private CompoundAssignOperator parseCompoundAssignOp(TokenType tokenType) throws DecafParserException {
    Token token = consumeToken(
        tokenType,
        tokenType.toString()
    );
    return switch (tokenType) {
      case ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> new CompoundAssignOperator(
          token.tokenPosition(),
          token.lexeme()
      );
      default -> throw getContextualException("expected compound assignOp");
    };
  }

  private AssignOpExpr parseAssignOpExpr(TokenType tokenType) throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    AssignOperator assignOp = parseAssignOp(tokenType);
    Expression expression = parseOrExpr();
    return new AssignOpExpr(
        tokenPosition,
        assignOp,
        expression
    );
  }

  private CompoundAssignOpExpr parseCompoundAssignOpExpr(TokenType tokenType) throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    CompoundAssignOperator assignOp = parseCompoundAssignOp(tokenType);
    Expression expression = parseOrExpr();
    return new CompoundAssignOpExpr(
        tokenPosition,
        assignOp,
        expression
    );
  }

  private AssignExpr parseIncrement() throws DecafParserException {
    TokenType expectedTokenType = getCurrentToken().tokenType();
    final Token token = consumeToken(expectedTokenType);
    if (expectedTokenType == INCREMENT)
      return new Increment(token.tokenPosition());
    else if (expectedTokenType == DECREMENT) {
      return new Decrement(token.tokenPosition());
    } else {
      throw new DecafParserException(
          token,
          "expected ++ or --, but received " + token.lexeme()
      );
    }
  }

  private AssignExpr parseAssignExpr() throws DecafParserException {
    switch (getCurrentToken().tokenType()) {
      case DECREMENT:
      case INCREMENT:
        return parseIncrement();
      case ASSIGN:
      case ADD_ASSIGN:
      case MINUS_ASSIGN:
      case MULTIPLY_ASSIGN:
        return parseAssignOpExpr(getCurrentToken().tokenType());
      default: {
        if (tokens.get(currentTokenIndex - 1)
                  .tokenType() == ID) {
          throw getContextualException("invalid type " + "\"" + tokens.get(currentTokenIndex - 1)
                                                                      .lexeme() + "\"");
        }
        throw getContextualException("invalid assign_expr");
      }
    }
  }

  private AssignExpr parseCompoundAssignExpr() throws DecafParserException {
    return switch (getCurrentToken().tokenType()) {
      case DECREMENT, INCREMENT -> parseIncrement();
      case ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> parseCompoundAssignOpExpr(getCurrentToken().tokenType());
      default -> throw new DecafParserException(
          getCurrentToken(),
          "expected compound_assign_expr"
      );
    };
  }

  private LocationArray parseLocationArray(Token token) throws DecafParserException {
    Expression expression = parseOrExpr();
    final LocationArray locationArray = new LocationArray(
        new Name(
            token.lexeme(),
            token.tokenPosition(),
            ExprContext.STORE
        ),
        expression
    );
    consumeToken(RIGHT_SQUARE_BRACKET);
    return locationArray;
  }

  private Location parseLocation(Token token) throws DecafParserException {
    if (getCurrentToken().tokenType() == LEFT_SQUARE_BRACKET) {
      consumeToken(LEFT_SQUARE_BRACKET);
      return parseLocationArray(token);
    }
    return new LocationVariable(new Name(
        token.lexeme(),
        token.tokenPosition(),
        ExprContext.STORE
    ));
  }

  private Expression parseExpr() throws DecafParserException {
    switch (getCurrentTokenType()) {
      case NOT:
      case MINUS: {
        return parseUnaryOpExpr();
      }
      case LEFT_PARENTHESIS:
        return parseParenthesizedExpression();
      case RESERVED_LEN:
        return parseLen();
      case DECIMAL_LITERAL:
        return parseLiteral(DECIMAL_LITERAL);
      case HEX_LITERAL:
        return parseLiteral(HEX_LITERAL);
      case CHAR_LITERAL:
        return parseLiteral(CHAR_LITERAL);
      case RESERVED_FALSE:
        return parseLiteral(RESERVED_FALSE);
      case RESERVED_TRUE:
        return parseLiteral(RESERVED_TRUE);
      case ID:
        return parseLocationOrMethodCall();
      default:
        throw new IllegalStateException();
    }
  }

  private Len parseLen() throws DecafParserException {
    final TokenPosition tokenPosition = consumeToken(
        RESERVED_LEN,
        DecafScanner.RESERVED_LEN
    ).tokenPosition();
    consumeToken(
        LEFT_PARENTHESIS,
        "expected a ( after " + DecafScanner.RESERVED_LEN
    );
    Name name = parseName((Token t) -> {
      if (t.tokenType() == RIGHT_PARENTHESIS) {
        return "cannot find len of nothing";
      } else {
        return "cannot find len of (..." + t.lexeme() + ")";
      }
    });
    consumeToken(
        RIGHT_PARENTHESIS,
        DecafScanner.RIGHT_PARENTHESIS
    );
    return new Len(
        tokenPosition,
        name
    );

  }

  private Literal parseLiteral(TokenType expectedLiteralType) throws DecafParserException {
    final Token token = consumeToken(expectedLiteralType);
    return switch (expectedLiteralType) {
      case CHAR_LITERAL -> new CharLiteral(
          token.tokenPosition(),
          token.lexeme()
      );
      case RESERVED_FALSE -> new BooleanLiteral(
          token.tokenPosition(),
          DecafScanner.RESERVED_FALSE
      );
      case RESERVED_TRUE -> new BooleanLiteral(
          token.tokenPosition(),
          DecafScanner.RESERVED_TRUE
      );
      case HEX_LITERAL -> new HexLiteral(
          token.tokenPosition(),
          token.lexeme()
      );
      case DECIMAL_LITERAL -> new DecimalLiteral(
          token.tokenPosition(),
          token.lexeme()
      );
      default -> throw new DecafParserException(
          getCurrentToken(),
          "unexpected literal"
      );
    };
  }

  private Return parseReturnStatement() throws DecafParserException {
    final TokenPosition tokenPosition = consumeToken(
        RESERVED_RETURN,
        DecafScanner.RESERVED_RETURN
    ).tokenPosition();
    Expression expression = null;
    if (getCurrentTokenType() != SEMICOLON) {
      expression = parseOrExpr();
    }
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    return new Return(
        tokenPosition,
        expression
    );
  }

  private Statement parseWhileStatement() throws DecafParserException {
    final TokenPosition tokenPosition = consumeToken(RESERVED_WHILE).tokenPosition();
    consumeToken(LEFT_PARENTHESIS);
    final Expression expression = parseOrExpr();
    consumeToken(RIGHT_PARENTHESIS);
    final Block block = parseBlock();
    return new While(
        tokenPosition,
        expression,
        block
    );
  }

  private If parseIfStatement() throws DecafParserException {
    final TokenPosition tokenPosition = consumeToken(
        RESERVED_IF,
        DecafScanner.RESERVED_IF
    ).tokenPosition();
    consumeToken(
        LEFT_PARENTHESIS,
        DecafScanner.LEFT_PARENTHESIS
    );
    Expression predicate;
    try {
      predicate = parseOrExpr();
    } catch (DecafException e) {
      if (getCurrentTokenType() == RIGHT_PARENTHESIS)
        throw getContextualException("if statement lacks a condition");
      throw e;
    }
    consumeToken(
        RIGHT_PARENTHESIS,
        DecafScanner.RIGHT_PARENTHESIS
    );
    Block ifBlock = parseBlock();
    Block elseBlock = null;
    if (getCurrentTokenType() == RESERVED_ELSE) {
      consumeToken(RESERVED_ELSE);
      elseBlock = parseBlock();
    }
    return new If(
        tokenPosition,
        predicate,
        ifBlock,
        elseBlock
    );
  }

  private Statement parseForStatement() throws DecafParserException {
    final TokenPosition tokenPosition = consumeToken(
        RESERVED_FOR,
        DecafScanner.RESERVED_FOR
    ).tokenPosition();
    consumeToken(
        LEFT_PARENTHESIS,
        DecafScanner.LEFT_PARENTHESIS
    );

    final Name initId = parseName(
        DecafScanner.IDENTIFIER,
        ExprContext.STORE
    );

    consumeToken(
        ASSIGN,
        DecafScanner.ASSIGN
    );
    final Expression initializationExpression = parseOrExpr();
    final Initialization initialization = new Initialization(
        initId,
        initializationExpression
    );

    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );

    final Expression terminatingCondition = parseOrExpr();

    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );

    final Location updateLocation = parseLocation(consumeToken(
        ID,
        DecafScanner.IDENTIFIER
    ));
    final AssignExpr updateAssignExpr = parseCompoundAssignExpr();
    final Assignment update = new Assignment(
        updateLocation,
        updateAssignExpr,
        updateAssignExpr.getOperator()
    );

    consumeToken(
        RIGHT_PARENTHESIS,
        DecafScanner.RIGHT_PARENTHESIS
    );

    final Block block = parseBlock();

    return new For(
        tokenPosition,
        initialization,
        terminatingCondition,
        update,
        block
    );
  }

  private Statement parseStatement() throws DecafParserException {
    switch (getCurrentTokenType()) {
      case RESERVED_BREAK:
        return parseBreak();
      case RESERVED_CONTINUE:
        return parseContinue();
      case RESERVED_RETURN:
        return parseReturnStatement();
      case RESERVED_WHILE:
        return parseWhileStatement();
      case RESERVED_IF:
        return parseIfStatement();
      case RESERVED_FOR:
        return parseForStatement();
      default: {
        if (getCurrentTokenType() == ID) {
          return parseLocationAndAssignExprOrMethodCall();
        } else {
          throw new IllegalStateException();
        }
      }
    }
  }

  private Break parseBreak() throws DecafParserException {
    final Break breakStatement = new Break(consumeToken(RESERVED_BREAK).tokenPosition());
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    return breakStatement;
  }

  private Continue parseContinue() throws DecafParserException {
    final Continue continueStatement = new Continue(consumeToken(RESERVED_CONTINUE).tokenPosition());
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    return continueStatement;
  }

  @SuppressWarnings("InfiniteLoopStatement")
  private void parseStatements(List<Statement> statementList) throws DecafParserException {
    try {
      while (true)
        statementList.add(parseStatement());
    } catch (IllegalStateException ignored) {
    }
  }

  private Block parseBlock() throws DecafParserException {
    consumeToken(
        LEFT_CURLY,
        DecafScanner.LEFT_CURLY
    );
    List<FieldDeclaration> fieldDeclarationList = new ArrayList<>();
    List<Statement> statementList = new ArrayList<>();
    parseFieldDeclarations(fieldDeclarationList);
    parseStatements(statementList);
    consumeToken(
        RIGHT_CURLY,
        DecafScanner.RIGHT_CURLY
    );
    return new Block(
        fieldDeclarationList,
        statementList
    );
  }

  private ImportDeclaration parseImportDeclaration() throws DecafParserException {
    consumeToken(
        RESERVED_IMPORT,
        DecafScanner.RESERVED_IMPORT
    );
    Name importName = parseName(
        DecafScanner.IDENTIFIER,
        ExprContext.IMPORT
    );
    consumeToken(
        SEMICOLON,
        DecafScanner.SEMICOLON
    );
    return new ImportDeclaration(importName);
  }

  private FieldDeclaration parseFieldDeclaration() throws DecafParserException {
    final TokenPosition tokenPosition = getCurrentToken().tokenPosition();
    Type type = parseBuiltinFieldType();
    List<Name> variables = new ArrayList<>();
    List<Array> arrays = new ArrayList<>();
    Name nameId = parseName(
        DecafScanner.IDENTIFIER,
        ExprContext.DECLARE
    );
    parseFieldDeclarationGroup(
        variables,
        arrays,
        nameId
    );

    while (getCurrentToken().tokenType() == COMMA) {
      consumeToken(
          COMMA,
          DecafScanner.COMMA
      );
      nameId = parseName(
          DecafScanner.IDENTIFIER,
          ExprContext.DECLARE
      );
      parseFieldDeclarationGroup(
          variables,
          arrays,
          nameId
      );
    }
    return new FieldDeclaration(
        tokenPosition,
        type,
        variables,
        arrays
    );
  }

  private Name parseName(
      String expected,
      ExprContext context
  ) throws DecafParserException {
    final Token idToken = consumeToken(
        TokenType.ID,
        expected
    );
    return new Name(
        idToken.lexeme(),
        idToken.tokenPosition(),
        context
    );
  }

  private Name parseName(Function<Token, String> errMessageProvider) throws DecafParserException {
    final Token token = consumeToken(
        TokenType.ID,
        errMessageProvider
    );
    return new Name(
        token.lexeme(),
        token.tokenPosition(),
        ExprContext.LOAD
    );
  }

  private Type parseBuiltinFieldType() throws DecafParserException {
    final Token token = consumeTokenNoCheck();
    return switch (token.tokenType()) {
      case RESERVED_INT -> Type.Int;
      case RESERVED_BOOL -> Type.Bool;
      default -> throw getContextualException(
          "expected " + "\"" + DecafScanner.RESERVED_INT + "\"" + " or " + "\"" + DecafScanner.RESERVED_BOOL + "\"");
    };
  }

  public void printParseTree() {
    List<String> tree = new ArrayList<>();
    treeBody(
        new Pair<>(
            ".",
            root
        ),
        tree,
        ""
    );
    while (tree.get(tree.size() - 1)
               .equals(""))
      tree.remove(tree.size() - 1);
    for (String s : tree) {
      System.out.println(s);
    }
  }

  public void setTrace(boolean showTrace) {
    this.showTrace = showTrace;
  }

  private static class PrintConstants {
    public static final String ELBOW = "└──";
    public static final String TEE = "├──";
    public static final String PIPE_PREFIX = "│   ";
    public static final String SPACE_PREFIX = "    ";
  }
}

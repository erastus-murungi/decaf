package decaf.analysis.syntax;

import static decaf.analysis.Token.Type.ASSIGN;
import static decaf.analysis.Token.Type.CHAR_LITERAL;
import static decaf.analysis.Token.Type.COMMA;
import static decaf.analysis.Token.Type.CONDITIONAL_AND;
import static decaf.analysis.Token.Type.CONDITIONAL_OR;
import static decaf.analysis.Token.Type.DIVIDE;
import static decaf.analysis.Token.Type.EOF;
import static decaf.analysis.Token.Type.EQ;
import static decaf.analysis.Token.Type.GEQ;
import static decaf.analysis.Token.Type.GT;
import static decaf.analysis.Token.Type.ID;
import static decaf.analysis.Token.Type.INT_LITERAL;
import static decaf.analysis.Token.Type.LEFT_CURLY;
import static decaf.analysis.Token.Type.LEFT_PARENTHESIS;
import static decaf.analysis.Token.Type.LEFT_SQUARE_BRACKET;
import static decaf.analysis.Token.Type.LEQ;
import static decaf.analysis.Token.Type.LT;
import static decaf.analysis.Token.Type.MINUS;
import static decaf.analysis.Token.Type.MOD;
import static decaf.analysis.Token.Type.MULTIPLY;
import static decaf.analysis.Token.Type.NEQ;
import static decaf.analysis.Token.Type.PLUS;
import static decaf.analysis.Token.Type.RESERVED_BOOL;
import static decaf.analysis.Token.Type.RESERVED_BREAK;
import static decaf.analysis.Token.Type.RESERVED_CONTINUE;
import static decaf.analysis.Token.Type.RESERVED_ELSE;
import static decaf.analysis.Token.Type.RESERVED_FALSE;
import static decaf.analysis.Token.Type.RESERVED_FOR;
import static decaf.analysis.Token.Type.RESERVED_IF;
import static decaf.analysis.Token.Type.RESERVED_IMPORT;
import static decaf.analysis.Token.Type.RESERVED_INT;
import static decaf.analysis.Token.Type.RESERVED_LEN;
import static decaf.analysis.Token.Type.RESERVED_RETURN;
import static decaf.analysis.Token.Type.RESERVED_TRUE;
import static decaf.analysis.Token.Type.RESERVED_VOID;
import static decaf.analysis.Token.Type.RESERVED_WHILE;
import static decaf.analysis.Token.Type.RIGHT_CURLY;
import static decaf.analysis.Token.Type.RIGHT_PARENTHESIS;
import static decaf.analysis.Token.Type.RIGHT_SQUARE_BRACKET;
import static decaf.analysis.Token.Type.SEMICOLON;
import static decaf.analysis.Token.Type.STRING_LITERAL;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import decaf.analysis.Token;
import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.ActualArgument;
import decaf.analysis.syntax.ast.ArithmeticOperator;
import decaf.analysis.syntax.ast.Array;
import decaf.analysis.syntax.ast.AssignExpr;
import decaf.analysis.syntax.ast.AssignOpExpr;
import decaf.analysis.syntax.ast.AssignOperator;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.Break;
import decaf.analysis.syntax.ast.CharLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.CompoundAssignOperator;
import decaf.analysis.syntax.ast.ConditionalOperator;
import decaf.analysis.syntax.ast.Continue;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.EqualityOperator;
import decaf.analysis.syntax.ast.Expression;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.For;
import decaf.analysis.syntax.ast.FormalArgument;
import decaf.analysis.syntax.ast.If;
import decaf.analysis.syntax.ast.ImportDeclaration;
import decaf.analysis.syntax.ast.Increment;
import decaf.analysis.syntax.ast.Initialization;
import decaf.analysis.syntax.ast.IntLiteral;
import decaf.analysis.syntax.ast.Len;
import decaf.analysis.syntax.ast.Literal;
import decaf.analysis.syntax.ast.Location;
import decaf.analysis.syntax.ast.LocationArray;
import decaf.analysis.syntax.ast.LocationAssignExpr;
import decaf.analysis.syntax.ast.LocationVariable;
import decaf.analysis.syntax.ast.MethodCall;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.RValue;
import decaf.analysis.syntax.ast.RelationalOperator;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.Statement;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.Type;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.analysis.syntax.ast.UnaryOperator;
import decaf.analysis.syntax.ast.VoidExpression;
import decaf.analysis.syntax.ast.While;
import decaf.shared.CompilationContext;
import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.errors.ParserError;

public class Parser {
  @NotNull
  private final CompilationContext context;
  @NotNull
  private final List<Token> tokens;
  @NotNull
  private final List<ParserError> errors;
  @NotNull
  private final Program root;
  private int currentTokenIndex;

  public Parser(@NotNull Scanner scanner, @NotNull CompilationContext context) {
    this.context = context;
    this.tokens = Lists.newArrayList(scanner);
    this.errors = new ArrayList<>();
    this.currentTokenIndex = 0;
    this.root = program();
  }

  private static void addTerminal(
      Pair<String, AST> labelAndNode, String prefix,
      String connector, List<String> tree
  ) {
    if (!labelAndNode.second()
                     .isTerminal())
      throw new IllegalArgumentException();
    tree.add(prefix + connector + " " +
                 Utils.coloredPrint(
                     labelAndNode.first(),
                     Utils.ANSIColorConstants.ANSI_BLUE
                 ) +
                 " = " + labelAndNode.second());
  }

  private static void addNonTerminal(
      Pair<String, AST> labelAndNode, int index,
      int numChildren, String prefix,
      String connector, List<String> tree
  ) {
    tree.add(String.format(
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
    prefix += (index != numChildren - 1) ? PrintConstants.PIPE_PREFIX
        : PrintConstants.SPACE_PREFIX;
    treeBody(
        labelAndNode,
        tree,
        prefix
    );
  }

  private static void treeBody(
      Pair<String, AST> parentNode, List<String> tree,
      String prefix
  ) {
    List<Pair<String, AST>> nodeList = parentNode.second()
                                                 .getChildren();
    for (int i = 0; i < nodeList.size(); i++) {
      final String connector = (i == nodeList.size() - 1) ? PrintConstants.ELBOW
          : PrintConstants.TEE;
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

  public @NotNull List<ParserError> getErrors() {
    return errors;
  }

  public String getPrettyErrorOutput() {
    return context.stringifyErrors(errors);
  }

  public @NotNull Program getRoot() {
    return root;
  }

  private Program program() {
    var program = new Program();
    processImportDeclarations(program);
    processFieldOrMethod(program);
    if (getCurrentTokenType() != EOF && !hasError()) {
      errors.add(new ParserError(
          ParserError.ErrorType.DID_NOT_FINISH_PARSING,
          getCurrentToken(),
          "did not finish parsing the program"
      ));
    }
    if (context.debugModeOn())
      printParseTree();
    return program;
  }

  private Token getCurrentToken() {
    return tokens.get(currentTokenIndex);
  }

  private Token.Type getCurrentTokenType() {
    return getCurrentToken().type;
  }

  private @NotNull Optional<Token> consumeToken(
      @NotNull Token.Type expectedType,
      @NotNull ParserError.ErrorType errorType,
      @NotNull String errorMessage
  ) {
    if (getCurrentTokenType() != expectedType) {
      errors.add(new ParserError(
          errorType,
          getCurrentToken(),
          errorMessage
      ));
      return Optional.empty();
    } else {
      var token = getCurrentToken();
      currentTokenIndex += 1;
      return Optional.of(token);
    }
  }

  private @NotNull Optional<Token> consumeToken(Token.Type expectedType) {
    String s = getCurrentToken().lexeme;
    if (getCurrentToken().type.toString()
                              .startsWith("RESERVED")) {
      s = "reserved keyword "
          + "\"" + s + "\"";
    }
    var errMessage = "expected "
        + "\"" + Token.getScannerSourceCode(expectedType) + "\""
        + " received " + s;
    return consumeToken(
        expectedType,
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        errMessage
    );
  }

  private Token consumeTokenNoCheck() {
    var token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
  }

  private void consumeToken(
      Token.Type expectedType,
      Function<Token, String> getErrMessage
  ) {
    if (getCurrentTokenType() != expectedType) {
      final String errMessage = getErrMessage.apply(getCurrentToken());
      errors.add(new ParserError(
          ParserError.ErrorType.UNEXPECTED_TOKEN,
          getCurrentToken(),
          errMessage
      ));
      return;
    }
    currentTokenIndex += 1;
  }

  public boolean hasError() {
    return !errors.isEmpty();
  }

  private Optional<Expression> parseOrExpr() {
    return parseAndExpr().flatMap(andExpr -> {
      if (getCurrentTokenType() == CONDITIONAL_OR) {
        var tokenPosition = consumeTokenNoCheck().tokenPosition;
        return parseOrExpr().map(
            orExpr
                -> BinaryOpExpression.of(
                andExpr,
                new ConditionalOperator(
                    tokenPosition,
                    Scanner.CONDITIONAL_OR
                ),
                orExpr
            ));
      }
      return Optional.of(andExpr);
    });
  }

  private Optional<Expression> parseAndExpr() {
    return parseEqualityExpr().flatMap(equalityExpr -> {
      if (getCurrentTokenType() == CONDITIONAL_AND) {
        return consumeToken(
            CONDITIONAL_AND,
            ParserError.ErrorType.IMPLEMENTATION_ERROR,
            "was promised conditional and ('&&') by implementation"
        )
            .flatMap(token
                         -> parseAndExpr().map(
                andExpr
                    -> BinaryOpExpression.of(
                    equalityExpr,
                    new ConditionalOperator(
                        token.tokenPosition,
                        Scanner.CONDITIONAL_AND
                    ),
                    andExpr
                )));
      }
      return Optional.of(equalityExpr);
    });
  }

  private Optional<Expression> parseEqualityExpr() {
    return parseRelationalExpr().flatMap(relationalExpr -> {
      if (getCurrentTokenType() == EQ || getCurrentTokenType() == NEQ) {
        return consumeToken(getCurrentTokenType())
            .flatMap(token
                         -> parseEqualityExpr().map(
                equalityExpr
                    -> BinaryOpExpression.of(
                    relationalExpr,
                    new EqualityOperator(
                        token.tokenPosition,
                        (token.type == EQ) ? Scanner.EQ: Scanner.NEQ
                    ),
                    equalityExpr
                )));
      }
      return Optional.of(relationalExpr);
    });
  }

  private Optional<Expression> parseRelationalExpr() {
    return parseAddSubExpr().flatMap(
        addSubExpr -> {
          if (getCurrentTokenType() == GT || getCurrentTokenType() == LT ||
              getCurrentTokenType() == GEQ || getCurrentTokenType() == LEQ) {
            return consumeToken(getCurrentTokenType()).flatMap(
                token -> parseRelationalExpr().map(
                    relationalExpr -> switch (token.type) {
                      case GT -> BinaryOpExpression.of(
                          addSubExpr,
                          new RelationalOperator(
                              token.tokenPosition,
                              Scanner.GT
                          ),
                          relationalExpr
                      );
                      case LT -> BinaryOpExpression.of(
                          addSubExpr,
                          new RelationalOperator(
                              token.tokenPosition,
                              Scanner.LT
                          ),
                          relationalExpr
                      );
                      case LEQ -> BinaryOpExpression.of(
                          addSubExpr,
                          new RelationalOperator(
                              token.tokenPosition,
                              Scanner.LEQ
                          ),
                          relationalExpr
                      );
                      default -> BinaryOpExpression.of(
                          addSubExpr,
                          new RelationalOperator(
                              token.tokenPosition,
                              Scanner.GEQ
                          ),
                          relationalExpr
                      );
                    }));
          }
          return Optional.of(addSubExpr);
        }
    );
  }

  private Optional<Expression> parseAddSubExpr() {
    return parseMulDivRemExpr().flatMap(
        mulDivRemExpr -> {
          if (getCurrentTokenType() == PLUS || getCurrentTokenType() == MINUS) {
            return consumeToken(getCurrentTokenType()).flatMap(
                token -> parseAddSubExpr().map(addSubExpr -> BinaryOpExpression.of(
                    mulDivRemExpr,
                    new ArithmeticOperator(
                        token.tokenPosition,
                        (token.type == PLUS) ? Scanner.PLUS: Scanner.MINUS
                    ),
                    addSubExpr
                )));
          }
          return Optional.of(mulDivRemExpr);
        }
    );
  }

  private Optional<Expression> parseMulDivRemExpr() {
    return parseExpr().flatMap(expression -> {
      if (getCurrentTokenType() == MULTIPLY ||
          getCurrentTokenType() == DIVIDE || getCurrentTokenType() == MOD) {
        return consumeToken(getCurrentTokenType())
            .flatMap(token
                         -> parseMulDivRemExpr().map(
                mulDivRemExpr
                    -> BinaryOpExpression.of(
                    expression,
                    new ArithmeticOperator(
                        token.tokenPosition,
                        (token.type == MULTIPLY) ? Scanner.MULTIPLY
                            : (token.type == DIVIDE) ? Scanner.DIVIDE
                            : Scanner.MOD
                    ),
                    mulDivRemExpr
                )));
      }
      return Optional.of(expression);
    });
  }

  private Optional<Expression> parseUnaryOpExpr() {
    return consumeToken(getCurrentTokenType())
        .flatMap(
            unaryOp
                -> parseExpr().map(
                expression
                    -> (Objects.equals(
                    unaryOp.lexeme,
                    Scanner.MINUS
                ) && expression instanceof IntLiteral intLiteral) ? intLiteral.negate(): new UnaryOpExpression(
                    new UnaryOperator(
                        unaryOp.tokenPosition,
                        unaryOp.lexeme
                    ),
                    expression
                )));
  }

  private Optional<ParenthesizedExpression> parseParenthesizedExpression() {
    return consumeToken(
        LEFT_PARENTHESIS,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "expected a left parenthesis to open a parenthesized expression"
    )
        .flatMap(
            tk
                -> parseOrExpr().flatMap(
                expr
                    -> consumeToken(
                    RIGHT_PARENTHESIS,
                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                    "expected a right parenthesis to close a parenthesized expression"
                )
                    .map(tk1
                             -> new ParenthesizedExpression(
                        tk.tokenPosition,
                        expr
                    ))));
  }

  private Optional<? extends Expression> parseLocationOrMethodCall() {
    return parseName("expected a valid identifier").flatMap(name -> {
      if (getCurrentTokenType() == LEFT_SQUARE_BRACKET) {
        return parseLocationArray(name);
      } else if (getCurrentTokenType() == LEFT_PARENTHESIS) {
        return parseMethodCall(name);
      } else {
        return Optional.of(new LocationVariable(name));
      }
    });
  }

  private void processImportDeclarations(Program program) {
    while (getCurrentTokenType() == RESERVED_IMPORT) {
      parseImportDeclaration().ifPresent(
          program.getImportDeclaration()::add);
    }
  }

  private void parseFieldDeclarations(List<FieldDeclaration> fieldDeclarationList) {
    if (getCurrentToken().type == RESERVED_BOOL ||
        getCurrentToken().type == RESERVED_INT) {
      do {
        var fieldDeclaration = parseFieldDeclaration();
        if (fieldDeclaration.isPresent()) {
          fieldDeclarationList.add(fieldDeclaration.get());
        } else {
          break;
        }
      } while ((getCurrentToken().type == RESERVED_BOOL ||
          getCurrentToken().type == RESERVED_INT));
      consumeToken(
          SEMICOLON,
          (Token t) -> {
            if (t.type == ASSIGN) {
              return "initializers not allowed here";
            } else if (t.type == ID) {
              return "expected \";\" but found " + Scanner.IDENTIFIER +
                  " : maybe missing a comma between variables in field decl?";
            } else {
              return "expected " + Scanner.SEMICOLON + " received " +
                  getCurrentTokenType().toString();
            }
          }
      );
      if ((getCurrentToken().type == RESERVED_BOOL ||
          getCurrentToken().type == RESERVED_INT)) {
        parseFieldDeclarations(fieldDeclarationList);
      }
    }
  }

  private void parseFieldDeclarationGroup(
      List<RValue> variables,
      List<Array> arrays,
      RValue rValueId
  ) {
    if (getCurrentToken().type == LEFT_SQUARE_BRACKET) {
      consumeToken(
          LEFT_SQUARE_BRACKET,
          ParserError.ErrorType.IMPLEMENTATION_ERROR,
          "was promised a left square bracket by the implementation"
      )
          .flatMap(
              tk
                  -> parseIntLiteral().flatMap(
                  intLiteral
                      -> consumeToken(
                      RIGHT_SQUARE_BRACKET,
                      ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                      "expected a right square bracket to close an array declaration, but found " +
                          getCurrentToken().lexeme
                  )
                      .map(tk1
                               -> arrays.add(new Array(
                          rValueId.tokenPosition,
                          intLiteral,
                          rValueId.getLabel()
                      )))));
    } else {
      variables.add(rValueId);
    }
  }

  private Optional<IntLiteral> parseIntLiteral() {
    Token intLiteralToken;
    if (getCurrentToken().type == INT_LITERAL) {
      intLiteralToken = consumeTokenNoCheck();
      return Optional.of(new IntLiteral(
          intLiteralToken.tokenPosition,
          intLiteralToken.lexeme
      ));
    } else {
      if (getCurrentTokenType() == RIGHT_SQUARE_BRACKET) {
        errors.add(new ParserError(
            ParserError.ErrorType.MISSING_ARRAY_SIZE,
            getCurrentToken(),
            "missing array size"
        ));
        consumeTokenNoCheck();
      } else {
        errors.add(new ParserError(
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            getCurrentToken(),
            "expected a valid int literal"
        ));
      }
      return Optional.empty();
    }
  }

  private void parseFieldDeclarationGroup(
      @NotNull Program program,
      @NotNull RValue RValueId,
      @NotNull Type type,
      @NotNull TokenPosition tokenPosition
  ) {
    // dealing with fieldDeclarations
    var variables = new ArrayList<RValue>();
    var arrays = new ArrayList<Array>();
    parseFieldDeclarationGroup(
        variables,
        arrays,
        RValueId
    );
    while (getCurrentToken().type == COMMA) {
      consumeToken(COMMA);
      parseName("expected a valid name but found " + getCurrentToken().lexeme)
          .map(nameId1 -> {
            parseFieldDeclarationGroup(
                variables,
                arrays,
                nameId1
            );
            return nameId1;
          })
          .orElseGet(() -> {
            errors.add(new ParserError(
                ParserError.ErrorType.MISSING_NAME,
                getCurrentToken(),
                String.format(
                    "expected a valid name but found %s",
                    getCurrentToken().lexeme
                )
            ));
            return RValue.DUMMY_R_VALUE;
          });
    }
    consumeToken(
        SEMICOLON,
        ParserError.ErrorType.MISSING_SEMICOLON,
        "expected a semicolon to terminate a field declaration"
    )
        .map(token
                 -> program.getFieldDeclaration()
                           .add(new FieldDeclaration(
                               tokenPosition,
                               type,
                               variables,
                               arrays
                           )));
    processFieldOrMethod(program);
  }

  private void parseMethodDeclaration(
      List<MethodDefinition> methodDefinitionList,
      RValue RValueId,
      Type type
  ) {
    var methodDefinitionParameterList = parseMethodArguments();
    parseBlock().ifPresent(
        block
            -> methodDefinitionList.add(new MethodDefinition(
            RValueId.tokenPosition,
            type,
            methodDefinitionParameterList,
            RValueId,
            block
        )));
    parseMethodDeclarations(methodDefinitionList);
  }

  private void processFieldOrMethod(Program program) {
    if (getCurrentTokenType() == RESERVED_INT ||
        getCurrentTokenType() == RESERVED_BOOL) {
      // could be an int or bool
      var position = getCurrentToken().tokenPosition;
      parseType().map(
          fieldType
              -> parseName("expected a valid name but found " +
                               getCurrentToken().lexeme)
              .map(nameId -> {
                if (getCurrentTokenType() == LEFT_PARENTHESIS) {
                  parseMethodDeclaration(
                      program.getMethodDefinitions(),
                      nameId,
                      fieldType
                  );
                } else {
                  parseFieldDeclarationGroup(
                      program,
                      nameId,
                      fieldType,
                      position
                  );
                }
                return fieldType;
              }));
    } else if (getCurrentTokenType() == RESERVED_VOID) {
      parseMethodDeclarations(program.getMethodDefinitions());
    } else {
      if (getCurrentTokenType() == ID &&
          currentTokenIndex + 1 < tokens.size() &&
          tokens.get(currentTokenIndex + 1).type == LEFT_PARENTHESIS) {
        errors.add(new ParserError(
            ParserError.ErrorType.MISSING_RETURN_TYPE,
            getCurrentToken(),
            String.format(
                "method `%s` missing return type",
                getCurrentToken().lexeme
            )
        ));
      } else if (getCurrentTokenType() == ID) {
        errors.add(new ParserError(
            ParserError.ErrorType.MISSING_FIELD_TYPE,
            getCurrentToken(),
            String.format(
                "field `%s` missing type",
                getCurrentToken().lexeme
            )
        ));
      }
    }
  }

  private void parseMethodDeclarations(List<MethodDefinition> methodDefinitionList) {
    while (getCurrentTokenType() == RESERVED_BOOL ||
        getCurrentTokenType() == RESERVED_INT ||
        getCurrentTokenType() == RESERVED_VOID) {
      parseMethodDeclaration().ifPresent(methodDefinitionList::add);
    }
  }

  private Type parseMethodReturnType() {
    final Token token = consumeTokenNoCheck();
    return switch (token.type) {
      case RESERVED_BOOL -> Type.Bool;
      case RESERVED_INT -> Type.Int;
      case RESERVED_VOID -> Type.Void;
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                token,
                "expected a valid return type, one of (int, bool, void) but found: " + token.lexeme
            )
        );
        yield Type.Undefined;
      }
    };
  }

  private Optional<FormalArgument> parseMethodArgument() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    final var typeOpt = parseType();
    return typeOpt.flatMap(type -> parseName(
        Scanner.IDENTIFIER
    ).map(
        nameId -> new FormalArgument(
            tokenPosition,
            nameId.getLabel(),
            type
        )
    ));
  }

  private void parseMethodArguments(List<FormalArgument> formalArgumentList) {
    parseMethodArgument().ifPresent(
        methodDefinitionParameter -> {
          formalArgumentList.add(methodDefinitionParameter);
          if (getCurrentTokenType() == COMMA) {
            consumeTokenNoCheck();
            parseMethodArguments(formalArgumentList);
          }
        }
    );
  }

  private List<FormalArgument> parseMethodArguments() {
    consumeToken(
        LEFT_PARENTHESIS,
        (Token token) -> {
          if (token.type == COMMA || token.type == SEMICOLON ||
              token.type == LEFT_SQUARE_BRACKET) {
            return "field decls must be first";
          } else {
            return "invalid method decl syntax: expected " + "\"" + Scanner.LEFT_PARENTHESIS + "\"" +
                " received " + "\"" + token.lexeme + "\"";
          }
        }
    );
    if (getCurrentToken().type == RESERVED_INT || getCurrentToken().type == RESERVED_BOOL) {
      var methodDefinitionParameterList = new ArrayList<FormalArgument>();
      parseMethodArguments(methodDefinitionParameterList);
      consumeToken(RIGHT_PARENTHESIS);
      return methodDefinitionParameterList;
    } else if (getCurrentTokenType() != RIGHT_PARENTHESIS) {
      if (getCurrentTokenType() == ID) {
        errors.add(
            new ParserError(
                ParserError.ErrorType.MISSING_METHOD_ARGUMENT_TYPE,
                getCurrentToken(),
                String.format(
                    "method parameter `%s` missing type",
                    getCurrentToken().lexeme
                )
            )
        );
      } else {
        errors.add(
            new ParserError(
                ParserError.ErrorType.ILLEGAL_ARGUMENT_TYPE,
                getCurrentToken(),
                String.format(
                    "illegal method arg type: %s",
                    getCurrentToken().lexeme
                )
            )
        );
      }
    }
    consumeToken(RIGHT_PARENTHESIS);
    return Collections.emptyList();
  }

  private Optional<MethodDefinition> parseMethodDeclaration() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    final var methodReturnType = parseMethodReturnType();
    return parseName("expected method to have an identifier").map(
        nameId -> {
          var methodDefinitionParameterList = parseMethodArguments();
          return parseBlock().map(
                                 block -> new MethodDefinition(
                                     tokenPosition,
                                     methodReturnType,
                                     methodDefinitionParameterList,
                                     nameId,
                                     block
                                 )
                             )
                             .orElseGet(
                                 () -> new MethodDefinition(
                                     tokenPosition,
                                     methodReturnType,
                                     methodDefinitionParameterList,
                                     nameId,
                                     new Block(
                                         Collections.emptyList(),
                                         Collections.emptyList()
                                     )
                                 )
                             );
        }
    );
  }

  private void parseMethodCallArguments(List<ActualArgument> actualArgumentList) {
    if (getCurrentTokenType() == STRING_LITERAL) {
      final Token token = consumeTokenNoCheck();
      actualArgumentList.add(new StringLiteral(
          token.tokenPosition,
          token.lexeme
      ));
    } else {
      var exprOpt = parseOrExpr();
      exprOpt.ifPresent(expression -> actualArgumentList.add(new ExpressionParameter(expression)));
    }
    if (getCurrentTokenType() == COMMA) {
      consumeToken(COMMA);
      parseMethodCallArguments(actualArgumentList);
    }
  }

  private List<ActualArgument> parseMethodCallArguments() {
    if (getCurrentTokenType() == RIGHT_PARENTHESIS)
      return Collections.emptyList();
    List<ActualArgument> actualArgumentList = new ArrayList<>();
    parseMethodCallArguments(actualArgumentList);
    return actualArgumentList;
  }

  private Optional<MethodCall> parseMethodCall(@NotNull Token token) {
    return consumeToken(
        LEFT_PARENTHESIS,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was expecting a method call to start with a left parenthesis: `(`"
    ).flatMap(
        tk -> {
          var methodCallParameterList = parseMethodCallArguments();
          return consumeToken(
              RIGHT_PARENTHESIS,
              ParserError.ErrorType.UNCLOSED_PARENTHESIS,
              "was expecting a method call to end with a right parenthesis"
          ).map(
              tk1 -> new MethodCall(
                  new RValue(
                      token.lexeme,
                      token.tokenPosition
                  ),
                  methodCallParameterList
              )
          );
        }
    );
  }

  private Optional<MethodCall> parseMethodCall(@NotNull RValue RValue) {
    return consumeToken(
        LEFT_PARENTHESIS,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was expecting a method call to start with a left parenthesis: `(`"
    ).flatMap(
        tk -> {
          var methodCallParameterList = parseMethodCallArguments();
          return consumeToken(
              RIGHT_PARENTHESIS,
              ParserError.ErrorType.UNCLOSED_PARENTHESIS,
              "was expecting a method call to end with a right parenthesis"
          ).map(
              tk1 -> new MethodCall(
                  RValue,
                  methodCallParameterList
              )
          );
        }
    );
  }

  private Optional<Statement> parseLocationAndAssignExprOrMethodCall() {
    return consumeToken(
        ID,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "Expected a valid identifier, `var` or fn_name()"
    ).flatMap(
        token -> {
          if (getCurrentTokenType() == LEFT_PARENTHESIS) {
            return parseMethodCall(token).flatMap(
                methodCall -> consumeToken(
                    SEMICOLON,
                    ParserError.ErrorType.MISSING_SEMICOLON,
                    "expected a semicolon to terminate a method call"
                ).map(
                    tk -> new MethodCallStatement(
                        token.tokenPosition,
                        methodCall
                    )
                )
            );
          } else {
            return parseLocationAndAssignExpr(token).flatMap(
                locationAssignExpr -> consumeToken(
                    SEMICOLON,
                    ParserError.ErrorType.MISSING_SEMICOLON,
                    "expected a semicolon to terminate an assignment expression"
                ).map(
                    tk -> locationAssignExpr
                )
            );
          }
        }
    );
  }

  private Optional<LocationAssignExpr> parseLocationAndAssignExpr(@NotNull Token token) {
    return parseLocation(token).flatMap(
        location -> parseAssignExpr().map(
            assignExpr -> new LocationAssignExpr(
                token.tokenPosition,
                location,
                assignExpr
            )
        )
    );
  }

  private Optional<AssignOperator> parseAssignOp() {
    var token = consumeTokenNoCheck();
    return switch (token.type) {
      case ASSIGN, ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> Optional.of(new AssignOperator(
          token.tokenPosition,
          token.lexeme
      ));
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                token,
                "expected assignOp"
            )
        );
        yield Optional.empty();
      }
    };
  }

  private Optional<CompoundAssignOperator> parseCompoundAssignOp() {
    var token = consumeTokenNoCheck();
    return switch (token.type) {
      case ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> Optional.of(new CompoundAssignOperator(
          token.tokenPosition,
          token.lexeme
      ));
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                token,
                "expected compound assignOp"
            )
        );
        yield Optional.empty();
      }
    };
  }

  private Optional<AssignOpExpr> parseAssignOpExpr() {
    return parseAssignOp().flatMap(
        assignOp -> parseOrExpr().map(
            expr -> new AssignOpExpr(
                assignOp.tokenPosition,
                assignOp,
                expr
            )
        )
    );
  }

  private Optional<CompoundAssignOpExpr> parseCompoundAssignOpExpr() {
    return parseCompoundAssignOp().flatMap(
        compoundAssignOp -> parseOrExpr().map(
            expr -> new CompoundAssignOpExpr(
                compoundAssignOp.tokenPosition,
                compoundAssignOp,
                expr
            )
        )
    );
  }

  private Optional<AssignExpr> parseIncrement() {
    var currentToken = consumeTokenNoCheck();
    return switch (currentToken.type) {
      case INCREMENT -> Optional.of(new Increment(currentToken.tokenPosition));
      case DECREMENT -> Optional.of(new Decrement(currentToken.tokenPosition));
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                currentToken,
                "expected ++ or --, but received " + currentToken.lexeme
            )
        );
        yield Optional.empty();
      }
    };
  }

  private Optional<? extends AssignExpr> parseAssignExpr() {
    return switch (getCurrentToken().type) {
      case ASSIGN, ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> parseAssignOpExpr();
      case DECREMENT, INCREMENT -> parseIncrement();
      default -> {
        if (tokens.get(currentTokenIndex - 1)
            .type == ID) {
          errors.add(
              new ParserError(
                  ParserError.ErrorType.INVALID_TYPE,
                  getCurrentToken(),
                  "invalid type " + "\"" + tokens.get(currentTokenIndex - 1)
                      .lexeme + "\""
              )
          );
        } else {
          errors.add(
              new ParserError(
                  ParserError.ErrorType.UNEXPECTED_TOKEN,
                  getCurrentToken(),
                  "expected assign expr"
              )
          );
        }
        yield Optional.empty();
      }
    };
  }

  private Optional<? extends AssignExpr> parseCompoundAssignExpr() {
    return switch (getCurrentToken().type) {
      case DECREMENT, INCREMENT -> parseIncrement();
      case ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> parseCompoundAssignOpExpr();
      default -> {
        errors.add(new ParserError(
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            getCurrentToken(),
            "expected compound assign expr"
        ));
        yield Optional.empty();
      }
    };
  }

  private Optional<LocationArray> parseLocationArray(@NotNull RValue RValue) {
    return consumeToken(
        LEFT_SQUARE_BRACKET,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "expected a left square bracket (`[`) to open array[expr]"
    )
        .flatMap(tk -> parseOrExpr().flatMap(
            expression -> consumeToken(
                RIGHT_SQUARE_BRACKET,
                ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                "expected a right square bracket (`]`) to close array[expr]"
            ).map(
                tk1 -> new LocationArray(
                    RValue,
                    expression
                )
            )
        ));
  }

  private Optional<? extends Location> parseLocation(@NotNull Token token) {
    if (getCurrentToken().type == LEFT_SQUARE_BRACKET) {
      return parseLocationArray(new RValue(
          token.lexeme,
          token.tokenPosition
      ));
    }
    return Optional.of(new LocationVariable(new RValue(
        token.lexeme,
        token.tokenPosition
    )));
  }

  private Optional<? extends Location> parseLocation() {
    return parseName("expected a valid identifier").flatMap(
        name -> {
          if (getCurrentTokenType() == LEFT_SQUARE_BRACKET) {
            return parseLocationArray(name);
          } else {
            return Optional.of(new LocationVariable(name));
          }
        }
    );
  }

  private Optional<? extends Expression> parseExpr() {
    return switch (getCurrentTokenType()) {
      case NOT, MINUS -> parseUnaryOpExpr();
      case LEFT_PARENTHESIS -> parseParenthesizedExpression();
      case RESERVED_LEN -> parseLen();
      case INT_LITERAL -> parseLiteral(INT_LITERAL);
      case CHAR_LITERAL -> parseLiteral(CHAR_LITERAL);
      case RESERVED_FALSE -> parseLiteral(RESERVED_FALSE);
      case RESERVED_TRUE -> parseLiteral(RESERVED_TRUE);
      case ID -> parseLocationOrMethodCall();
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                getCurrentToken(),
                "expected an expression"
            )
        );
        yield Optional.empty();
      }
    };
  }

  private Optional<Len> parseLen() {
    return consumeToken(
        RESERVED_LEN,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised a len statement by implementation"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            "expected a `(` to open a len statement but got " + getCurrentToken().lexeme
        ).flatMap(
            tk -> parseName("cannot find len of " + tk.lexeme).flatMap(
                name -> consumeToken(
                    RIGHT_PARENTHESIS,
                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                    "expected a `)` to close len statement"
                ).map(
                    tk1 -> new Len(
                        token.tokenPosition,
                        name
                    )
                )
            )
        )
    );
  }

  private Optional<Literal> parseLiteral(Token.Type expectedLiteralType) {
    return consumeToken(expectedLiteralType).map(
        token -> switch (expectedLiteralType) {
          case CHAR_LITERAL -> new CharLiteral(
              token.tokenPosition,
              token.lexeme
          );
          case RESERVED_FALSE -> new BooleanLiteral(
              token.tokenPosition,
              Scanner.RESERVED_FALSE
          );
          case RESERVED_TRUE -> new BooleanLiteral(
              token.tokenPosition,
              Scanner.RESERVED_TRUE
          );
          default -> new IntLiteral(
              token.tokenPosition,
              token.lexeme
          );
        });
  }

  private Optional<Return> parseReturnStatement() {
    return consumeToken(
        RESERVED_RETURN,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised a return statement by implementation"
    ).flatMap(
        token -> {
          if (getCurrentTokenType() == SEMICOLON) {
            consumeToken(
                SEMICOLON,
                ParserError.ErrorType.IMPLEMENTATION_ERROR,
                "was promised a semicolon by implementation"
            );
            return Optional.of(new Return(
                token.tokenPosition,
                new VoidExpression(
                    token.tokenPosition
                )
            ));
          } else {
            return parseOrExpr().flatMap(
                expression -> consumeToken(
                    SEMICOLON,
                    ParserError.ErrorType.MISSING_SEMICOLON,
                    "was expecting semicolon after return statement"
                ).flatMap(
                    tk -> Optional.of(new Return(
                        token.tokenPosition,
                        expression
                    ))
                )
            );
          }
        }
    );
  }

  private Optional<Statement> parseWhileStatement() {
    return consumeToken(
        RESERVED_WHILE,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised a `while statement` by implementation"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            "expected a `(` after " + Scanner.RESERVED_WHILE + " to denote beginning of while statement"
        ).flatMap(
            tk -> parseOrExpr().flatMap(
                expression -> consumeToken(
                    RIGHT_PARENTHESIS,
                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                    "expected `)` to close out while statement condition"
                ).flatMap(
                    tk1 -> parseBlock().map(
                        block -> new While(
                            token.tokenPosition,
                            expression,
                            block
                        )
                    )
                )
            )
        )
    );
  }

  private Optional<If> parseIfStatement() {
    return consumeToken(
        RESERVED_IF,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised an `if statement` by implementation"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            "expected a `(` after " + Scanner.RESERVED_IF + " to denote beginning of if statement"
        ).flatMap(
            tk -> parseOrExpr().flatMap(
                expression -> consumeToken(
                    RIGHT_PARENTHESIS,
                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                    "expected `)` to close out if statement condition"
                ).flatMap(
                    tk1 -> parseBlock().flatMap(
                        block -> {
                          if (getCurrentTokenType() == RESERVED_ELSE) {
                            return consumeToken(
                                RESERVED_ELSE,
                                ParserError.ErrorType.IMPLEMENTATION_ERROR,
                                "was promised an `else statement` by implementation"
                            ).flatMap(
                                tk2 -> parseBlock().map(
                                    elseBlock -> new If(
                                        token.tokenPosition,
                                        expression,
                                        block,
                                        elseBlock
                                    )
                                )
                            );
                          } else {
                            return Optional.of(new If(
                                token.tokenPosition,
                                expression,
                                block,
                                null
                            ));
                          }
                        }
                    )
                )
            )
        )
    );
  }

  private Optional<Statement> parseForStatement() {
    return consumeToken(
        RESERVED_FOR,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised a `for statement` by implementation"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            ParserError.ErrorType.UNEXPECTED_TOKEN,
            "expected a `(` after " + Scanner.RESERVED_FOR +
                " to denote beginning of for statement but found " + getCurrentToken().lexeme
        ).flatMap(
            tk -> parseName("expected initialization variable").flatMap(
                initId -> consumeToken(
                    ASSIGN,
                    ParserError.ErrorType.UNEXPECTED_TOKEN,
                    "expected `=` to split initialization variable and expression"
                ).flatMap(
                    tk1 -> parseOrExpr(
                    ).flatMap(
                        initExpr -> consumeToken(
                            SEMICOLON,
                            ParserError.ErrorType.MISSING_SEMICOLON,
                            "expected a `;` after for statement initializer"
                        ).flatMap(
                            tk2 -> parseOrExpr().flatMap(
                                terminatingCondition -> consumeToken(
                                    SEMICOLON,
                                    ParserError.ErrorType.MISSING_SEMICOLON,
                                    "expected a `;` after for statement terminating condition"
                                ).flatMap(
                                    tk3 -> parseLocation().flatMap(
                                        updateLocation -> parseCompoundAssignExpr().flatMap(
                                            updateAssignExpr ->
                                                consumeToken(
                                                    RIGHT_PARENTHESIS,
                                                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                                                    "expected a `)` to close out for statement"
                                                ).flatMap(
                                                    tk4 -> parseBlock().map(
                                                        block -> new For(
                                                            token.tokenPosition,
                                                            new Initialization(
                                                                initId,
                                                                initExpr
                                                            ),
                                                            terminatingCondition,
                                                            new Assignment(
                                                                updateLocation,
                                                                updateAssignExpr,
                                                                updateAssignExpr.getOperator()
                                                            ),
                                                            block
                                                        )
                                                    )
                                                )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
    );
  }

  private Optional<Break> parseBreak() {
    return consumeToken(
        RESERVED_BREAK,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised keyword `break` by implementation"
    ).flatMap(
        token -> consumeToken(
            SEMICOLON,
            ParserError.ErrorType.MISSING_SEMICOLON,
            "expected semicolon after break statement"
        ).map(
            tk -> new Break(
                token.tokenPosition
            )
        )
    );
  }

  private Optional<Continue> parseContinue() {
    return consumeToken(
        RESERVED_CONTINUE,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised keyword `continue` by implementation"
    ).flatMap(
        token -> consumeToken(
            SEMICOLON,
            ParserError.ErrorType.MISSING_SEMICOLON,
            "expected semicolon after continue statement"
        ).map(
            tk -> new Continue(
                token.tokenPosition
            )
        )
    );
  }

  private void parseStatements(List<Statement> statementList) {
    while (true) {
      if (getCurrentTokenType() == RESERVED_RETURN) {
        parseReturnStatement().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == RESERVED_BREAK) {
        parseBreak().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == RESERVED_CONTINUE) {
        parseContinue().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == RESERVED_WHILE) {
        parseWhileStatement().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == RESERVED_IF) {
        parseIfStatement().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == RESERVED_FOR) {
        parseForStatement().ifPresent(statementList::add);
      } else if (getCurrentTokenType() == ID) {
        parseLocationAndAssignExprOrMethodCall().ifPresent(statementList::add);
      } else {
        break;
      }
    }
  }

  private Optional<Block> parseBlock() {
    return consumeToken(
        LEFT_CURLY,
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected a left curly brace to start a block"
    ).flatMap(
        token -> {
          var fieldDeclarationList = new ArrayList<FieldDeclaration>();
          var statementList = new ArrayList<Statement>();
          parseFieldDeclarations(fieldDeclarationList);
          parseStatements(statementList);
          return consumeToken(
              RIGHT_CURLY,
              ParserError.ErrorType.UNCLOSED_PARENTHESIS,
              "expected a right curly brace to end a block"
          ).map(
              rightCurlyToken -> new Block(
                  fieldDeclarationList,
                  statementList
              )
          );
        }
    );
  }

  private Optional<ImportDeclaration> parseImportDeclaration() {
    return (consumeToken(
        RESERVED_IMPORT,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was promised keyword `import` by implementation"
    ).flatMap(token -> parseName(
        "expected valid import name not " + getCurrentToken().lexeme
    ).flatMap(
        importName -> consumeToken(
            SEMICOLON,
            ParserError.ErrorType.MISSING_SEMICOLON,
            "expected semicolon after import statement"
        ).map(
            tk -> new ImportDeclaration(
                importName
            )
        )
    )));
  }

  private Optional<FieldDeclaration> parseFieldDeclaration() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    return parseType().map(
                          type -> parseName(
                              Scanner.IDENTIFIER
                          ).map(
                              nameId -> {
                                var variables = new ArrayList<RValue>();
                                var arrays = new ArrayList<Array>();
                                parseFieldDeclarationGroup(
                                    variables,
                                    arrays,
                                    nameId
                                );
                                while (getCurrentTokenType() == COMMA) {
                                  consumeToken(
                                      COMMA,
                                      ParserError.ErrorType.UNEXPECTED_TOKEN,
                                      "expected a comma to separate field declarations"
                                  );
                                  nameId = parseName(
                                      Scanner.IDENTIFIER
                                  ).orElseGet(
                                      () -> {
                                        errors.add(
                                            new ParserError(
                                                ParserError.ErrorType.MISSING_NAME,
                                                getCurrentToken(),
                                                "expected valid field name"
                                            )
                                        );
                                        return new RValue(
                                            "`MISSING FIELD NAME`",
                                            getCurrentToken().tokenPosition
                                        );
                                      }
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
                          )
                      )
                      .orElseGet(
                          () -> {
                            errors.add(
                                new ParserError(
                                    ParserError.ErrorType.MISSING_FIELD_TYPE,
                                    getCurrentToken(),
                                    "expected valid field type"
                                )
                            );
                            return Optional.empty();
                          }
                      );
  }

  private Optional<RValue> parseName(
      String expected
  ) {
    return consumeToken(
        ID,
        ParserError.ErrorType.MISSING_NAME,
        expected
    ).map(
        token -> new RValue(
            token.lexeme,
            token.tokenPosition
        )
    );
  }

  private Optional<Type> parseType() {
    final Token token = consumeTokenNoCheck();
    return switch (token.type) {
      case RESERVED_INT -> Optional.of(Type.Int);
      case RESERVED_BOOL -> Optional.of(Type.Bool);
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.INVALID_FIELD_TYPE,
                token,
                "expected a valid builtin field type, one of (int, bool) but found: " + token.lexeme
            )
        );
        yield Optional.empty();
      }
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
               .isEmpty())
      tree.remove(tree.size() - 1);
    for (String s : tree) {
      System.out.println(s);
    }
  }

  private static class PrintConstants {
    public static final String ELBOW = "└──";
    public static final String TEE = "├──";
    public static final String PIPE_PREFIX = "│   ";
    public static final String SPACE_PREFIX = "    ";
  }
}

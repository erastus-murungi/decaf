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

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import decaf.ast.VoidExpression;
import decaf.ast.While;
import decaf.common.CompilationContext;
import decaf.common.Pair;
import decaf.common.Utils;
import decaf.errors.ParserError;

public class Parser {
  @NotNull
  public final Scanner scanner;
  @NotNull
  private final CompilationContext context;
  @NotNull
  private final List<Token> tokens;
  @NotNull
  private final List<ParserError> errors;
  private final Program root;
  private int currentTokenIndex;

  public Parser(
      @NotNull Scanner scanner,
      @NotNull CompilationContext context
  ) {
    this.scanner = scanner;
    this.context = context;
    this.tokens = Lists.newArrayList(scanner);
    this.errors = new ArrayList<>();
    this.currentTokenIndex = 0;
    this.root = program();
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

  public @NotNull List<ParserError> getErrors() {
    return errors;
  }

  public String getPrettyErrorOutput() {
    return context.stringifyErrors(errors);
  }

  public Program getRoot() {
    return root;
  }

  public final Program program() {
    var program = new Program();
    processImportDeclarations(program);
    processFieldOrMethod(program);
    if (getCurrentTokenType() != EOF && !hasError()) {
      errors.add(
          new ParserError(
              ParserError.ErrorType.DID_NOT_FINISH_PARSING,
              getCurrentToken(),
              "did not finish parsing the program"
          )
      );
    }
    if (context.debugModeOn())
      printParseTree();
    return program;
  }

  private Token getCurrentToken() {
    return tokens.get(currentTokenIndex);
  }

  private TokenType getCurrentTokenType() {
    return getCurrentToken().tokenType;
  }

  private @NotNull Optional<Token> consumeToken(
      TokenType expectedTokenType,
      String errorMessage
  ) {
    if (getCurrentTokenType() != expectedTokenType) {
      errors.add(
          new ParserError(
              ParserError.ErrorType.UNEXPECTED_TOKEN,
              getCurrentToken(),
              errorMessage
          )
      );
      return Optional.empty();
    } else {
      var token = getCurrentToken();
      currentTokenIndex += 1;
      return Optional.of(token);
    }
  }

  private @NotNull Optional<Token> consumeToken(
      TokenType expectedTokenType
  ) {
    String s = getCurrentToken().lexeme;
    if (getCurrentToken().tokenType
                         .toString()
                         .startsWith("RESERVED")) {
      s = "reserved keyword " + "\"" + s + "\"";
    }
    var errMessage = "expected " + "\"" + Token.getScannerSourceCode(expectedTokenType) + "\"" + " received " + s;
    return consumeToken(
        expectedTokenType,
        errMessage
    );
  }

  private Token consumeTokenNoCheck() {
    var token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
  }

  private Optional<Token> consumeToken(
      TokenType expectedTokenType,
      Function<Token, String> getErrMessage
  ) {
    if (getCurrentTokenType() != expectedTokenType) {
      final String errMessage = getErrMessage.apply(getCurrentToken());
      errors.add(
          new ParserError(
              ParserError.ErrorType.UNEXPECTED_TOKEN,
              getCurrentToken(),
              errMessage
          )
      );
      return Optional.empty();
    }
    var token = getCurrentToken();
    currentTokenIndex += 1;
    return Optional.of(token);
  }

  public boolean hasError() {
    return !errors.isEmpty();
  }

  private Optional<Expression> parseOrExpr() {
    return parseAndExpr().flatMap(
        andExpr -> {
          if (getCurrentTokenType() == CONDITIONAL_OR) {
            var tokenPosition = consumeTokenNoCheck().tokenPosition;
            return parseOrExpr().map(orExpr -> BinaryOpExpression.of(
                andExpr,
                new ConditionalOperator(
                    tokenPosition,
                    Scanner.CONDITIONAL_OR
                ),
                orExpr
            ));
          }
          return Optional.of(andExpr);
        }
    );
  }

  private Optional<Expression> parseAndExpr() {
    return parseEqualityExpr().flatMap(
        equalityExpr -> {
          if (getCurrentTokenType() == CONDITIONAL_AND) {
            return consumeToken(
                CONDITIONAL_AND,
                "was promised conditional and ('&&') by implementation"
            ).flatMap(
                token -> parseAndExpr().map(andExpr -> BinaryOpExpression.of(
                    equalityExpr,
                    new ConditionalOperator(
                        token.tokenPosition,
                        Scanner.CONDITIONAL_AND
                    ),
                    andExpr
                )));
          }
          return Optional.of(equalityExpr);
        }
    );
  }

  private Optional<Expression> parseEqualityExpr() {
    return parseRelationalExpr().flatMap(
        relationalExpr -> {
          if (getCurrentTokenType() == EQ || getCurrentTokenType() == NEQ) {
            return consumeToken(getCurrentTokenType()).flatMap(
                token -> parseEqualityExpr().map(equalityExpr -> BinaryOpExpression.of(
                    relationalExpr,
                    new EqualityOperator(
                        token.tokenPosition,
                        (token.tokenType == EQ) ? Scanner.EQ: Scanner.NEQ
                    ),
                    equalityExpr
                )));
          }
          return Optional.of(relationalExpr);
        }
    );
  }

  private Optional<Expression> parseRelationalExpr() {
    return parseAddSubExpr().flatMap(
        addSubExpr -> {
          if (getCurrentTokenType() == GT
              || getCurrentTokenType() == LT
              || getCurrentTokenType() == GEQ
              || getCurrentTokenType() == LEQ
          ) {
            return consumeToken(getCurrentTokenType()).flatMap(
                token -> parseRelationalExpr().map(
                    relationalExpr -> switch (token.tokenType) {
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
                        (token.tokenType == PLUS) ? Scanner.PLUS: Scanner.MINUS
                    ),
                    addSubExpr
                )));
          }
          return Optional.of(mulDivRemExpr);
        }
    );
  }

  private Optional<Expression> parseMulDivRemExpr() {
    return parseExpr().flatMap(
        expression -> {
          if (getCurrentTokenType() == MULTIPLY
              || getCurrentTokenType() == DIVIDE
              || getCurrentTokenType() == MOD
          ) {
            return consumeToken(getCurrentTokenType()).flatMap(
                token -> parseMulDivRemExpr().map(mulDivRemExpr -> BinaryOpExpression.of(
                    expression,
                    new ArithmeticOperator(
                        token.tokenPosition,
                        (token.tokenType == MULTIPLY) ? Scanner.MULTIPLY: (token.tokenType ==
                            DIVIDE) ? Scanner.DIVIDE: Scanner.MOD
                    ),
                    mulDivRemExpr
                )));
          }
          return Optional.of(expression);
        }
    );
  }

  private Optional<Expression> parseUnaryOpExpr() {
    return consumeToken(getCurrentTokenType()).flatMap(
        unaryOp -> parseExpr().map(expression -> new UnaryOpExpression(
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
        "expected a left parenthesis to open a parenthesized expression"
    ).flatMap(tk -> parseOrExpr().flatMap(
        expr -> consumeToken(
            RIGHT_PARENTHESIS,
            "expected a right parenthesis to close a parenthesized expression"
        ).map(tk1 -> new ParenthesizedExpression(
            tk.tokenPosition,
            expr
        ))));
  }

  private Optional<? extends Expression> parseLocationOrMethodCall() {
    return parseName("expected a valid identifier").flatMap(
        name -> {
          if (getCurrentTokenType() == LEFT_SQUARE_BRACKET) {
            return parseLocationArray(name);
          } else if (getCurrentTokenType() == LEFT_PARENTHESIS) {
            return parseMethodCall(name);
          } else {
            return Optional.of(new LocationVariable(name));
          }
        }
    );
  }

  private void processImportDeclarations(Program program) {
    while (getCurrentTokenType() == RESERVED_IMPORT) {
      parseImportDeclaration().ifPresent(program.getImportDeclarationList()::add);
    }
  }

  private void parseFieldDeclarations(List<FieldDeclaration> fieldDeclarationList) {
    if (getCurrentToken().tokenType == RESERVED_BOOL || getCurrentToken().tokenType == RESERVED_INT) {
      do {
        var fieldDeclaration = parseFieldDeclaration();
        if (fieldDeclaration.isPresent()) {
          fieldDeclarationList.add(fieldDeclaration.get());
        } else {
          break;
        }
      } while ((getCurrentToken().tokenType == RESERVED_BOOL || getCurrentToken().tokenType == RESERVED_INT));
      consumeToken(
          SEMICOLON,
          (Token t) -> {
            if (t.tokenType == ASSIGN) {
              return "initializers not allowed here";
            } else if (t.tokenType == ID) {
              return "expected \";\" but found " + Scanner.IDENTIFIER +
                  " : maybe missing a comma between variables in field decl?";
            } else {
              return "expected " + Scanner.SEMICOLON + " received " + getCurrentTokenType().toString();
            }
          }
      );
      if ((getCurrentToken().tokenType == RESERVED_BOOL || getCurrentToken().tokenType == RESERVED_INT)) {
        parseFieldDeclarations(fieldDeclarationList);
      }
    }
  }

  private void parseFieldDeclarationGroup(
      List<Name> variables,
      List<Array> arrays,
      Name nameId
  ) {
    if (getCurrentToken().tokenType == LEFT_SQUARE_BRACKET) {
      consumeToken(
          LEFT_SQUARE_BRACKET,
          "was promised a left square bracket by the implementation"
      ).flatMap(
          tk -> parseIntLiteral().flatMap(
              intLiteral -> consumeToken(
                  RIGHT_SQUARE_BRACKET,
                  "expected a right square bracket to close an array declaration"
              ).map(tk1 -> arrays.add(new Array(
                  intLiteral,
                  nameId
              ))))
      );
    } else {
      variables.add(nameId);
    }
  }

  private Optional<IntLiteral> parseIntLiteral() {
    Token intLiteralToken;
    if (getCurrentToken().tokenType == DECIMAL_LITERAL) {
      intLiteralToken = consumeTokenNoCheck();
      return Optional.of(new DecimalLiteral(
          intLiteralToken.tokenPosition,
          intLiteralToken.lexeme
      ));
    } else if (getCurrentToken().tokenType == HEX_LITERAL) {
      intLiteralToken = consumeTokenNoCheck();
      return Optional.of(new HexLiteral(
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
      @NotNull Name nameId,
      @NotNull Type type,
      @NotNull TokenPosition tokenPosition
  ) {
    // dealing with fieldDeclarations
    var variables = new ArrayList<Name>();
    var arrays = new ArrayList<Array>();
    parseFieldDeclarationGroup(
        variables,
        arrays,
        nameId
    );
    while (getCurrentToken().tokenType == COMMA) {
      consumeToken(COMMA);
      parseName(
          "expected a valid name but found " + getCurrentToken().lexeme
      ).map(
           nameId1 -> {
             parseFieldDeclarationGroup(
                 variables,
                 arrays,
                 nameId1
             );
             return nameId1;
           }
       )
       .orElseGet(
           () -> {
             errors.add(
                 new ParserError(
                     ParserError.ErrorType.MISSING_NAME,
                     getCurrentToken(),
                     String.format(
                         "expected a valid name but found %s",
                         getCurrentToken().lexeme
                     )
                 )
             );
             return Name.dummyName;
           }
       );
    }
    consumeToken(
        SEMICOLON,
        "expected a semicolon to terminate a field declaration"
    ).map(
        token -> program.getFieldDeclarationList()
                        .add(new FieldDeclaration(
                            tokenPosition,
                            type,
                            variables,
                            arrays
                        ))
    );
    processFieldOrMethod(program);
  }

  private void parseMethodDeclaration(
      List<MethodDefinition> methodDefinitionList,
      Name nameId,
      Type type
  ) {
    var methodDefinitionParameterList = parseMethodArguments();
    parseBlock().ifPresent(
        block -> methodDefinitionList.add(new MethodDefinition(
            nameId.tokenPosition,
            type,
            methodDefinitionParameterList,
            nameId,
            block
        ))
    );
    parseMethodDeclarations(methodDefinitionList);
  }

  private void processFieldOrMethod(Program program) {
    if (getCurrentTokenType() == RESERVED_INT || getCurrentTokenType() == RESERVED_BOOL) {
      // could be an int or bool
      var position = getCurrentToken().tokenPosition;
      parseBuiltinFieldType().map(
          fieldType -> parseName(
              "expected a valid name but found " + getCurrentToken().lexeme
          ).map(
              nameId -> {
                if (getCurrentTokenType() == LEFT_PARENTHESIS) {
                  parseMethodDeclaration(
                      program.getMethodDefinitionList(),
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
              })
      );
    } else if (getCurrentTokenType() == RESERVED_VOID) {
      parseMethodDeclarations(program.getMethodDefinitionList());
    } else {
      if (getCurrentTokenType() == ID && currentTokenIndex + 1 < tokens.size() && tokens.get(currentTokenIndex + 1)
                                                                                        .tokenType ==
          LEFT_PARENTHESIS) {
        errors.add(
            new ParserError(
                ParserError.ErrorType.MISSING_RETURN_TYPE,
                getCurrentToken(),
                String.format(
                    "method `%s` missing return type",
                    getCurrentToken().lexeme
                )
            )
        );
      } else if (getCurrentTokenType() == ID) {
        errors.add(
            new ParserError(
                ParserError.ErrorType.MISSING_FIELD_TYPE,
                getCurrentToken(),
                String.format(
                    "field `%s` missing type",
                    getCurrentToken().lexeme
                )
            )
        );
      }
    }
  }

  private void parseMethodDeclarations(List<MethodDefinition> methodDefinitionList) {
    while (getCurrentTokenType() == RESERVED_BOOL || getCurrentTokenType() == RESERVED_INT ||
        getCurrentTokenType() == RESERVED_VOID) {
      parseMethodDeclaration().ifPresent(methodDefinitionList::add);
    }
  }

  private Type parseMethodReturnType() {
    final Token token = consumeTokenNoCheck();
    return switch (token.tokenType) {
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

  private Optional<MethodDefinitionParameter> parseMethodArgument() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    final var typeOpt = parseBuiltinFieldType();
    return typeOpt.flatMap(type -> parseName(
        Scanner.IDENTIFIER
    ).map(
        nameId -> new MethodDefinitionParameter(
            tokenPosition,
            nameId.getLabel(),
            type
        )
    ));
  }

  private void parseMethodArguments(List<MethodDefinitionParameter> methodDefinitionParameterList) {
    parseMethodArgument().ifPresent(
        methodDefinitionParameter -> {
          methodDefinitionParameterList.add(methodDefinitionParameter);
          if (getCurrentTokenType() == COMMA) {
            consumeTokenNoCheck();
            parseMethodArguments(methodDefinitionParameterList);
          }
        }
    );
  }

  private List<MethodDefinitionParameter> parseMethodArguments() {
    consumeToken(
        LEFT_PARENTHESIS,
        (Token token) -> {
          if (token.tokenType == COMMA || token.tokenType == SEMICOLON ||
              token.tokenType == LEFT_SQUARE_BRACKET) {
            return "field decls must be first";
          } else {
            return "invalid method decl syntax: expected " + "\"" + Scanner.LEFT_PARENTHESIS + "\"" +
                " received " + "\"" + token.lexeme + "\"";
          }
        }
    );
    if (getCurrentToken().tokenType == RESERVED_INT || getCurrentToken().tokenType == RESERVED_BOOL) {
      var methodDefinitionParameterList = new ArrayList<MethodDefinitionParameter>();
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

  private void parseMethodCallArguments(List<MethodCallParameter> methodCallParameterList) {
    if (getCurrentTokenType() == STRING_LITERAL) {
      final Token token = consumeTokenNoCheck();
      methodCallParameterList.add(new StringLiteral(
          token.tokenPosition,
          token.lexeme
      ));
    } else {
      var exprOpt = parseOrExpr();
      exprOpt.ifPresent(expression -> methodCallParameterList.add(new ExpressionParameter(expression)));
    }
    if (getCurrentTokenType() == COMMA) {
      consumeToken(COMMA);
      parseMethodCallArguments(methodCallParameterList);
    }
  }

  private List<MethodCallParameter> parseMethodCallArguments() {
    if (getCurrentTokenType() == RIGHT_PARENTHESIS)
      return Collections.emptyList();
    List<MethodCallParameter> methodCallParameterList = new ArrayList<>();
    parseMethodCallArguments(methodCallParameterList);
    return methodCallParameterList;
  }

  private Optional<MethodCall> parseMethodCall(@NotNull Token token) {
    return consumeToken(
        LEFT_PARENTHESIS,
        "was expecting a method call to start with a left parenthesis: `(`"
    ).flatMap(
        tk -> {
          var methodCallParameterList = parseMethodCallArguments();
          return consumeToken(
              RIGHT_PARENTHESIS,
              "was expecting a method call to end with a right parenthesis"
          ).map(
              tk1 -> new MethodCall(
                  new Name(
                      token.lexeme,
                      token.tokenPosition
                  ),
                  methodCallParameterList
              )
          );
        }
    );
  }

  private Optional<MethodCall> parseMethodCall(@NotNull Name name) {
    return consumeToken(
        LEFT_PARENTHESIS,
        "was expecting a method call to start with a left parenthesis: `(`"
    ).flatMap(
        tk -> {
          var methodCallParameterList = parseMethodCallArguments();
          return consumeToken(
              RIGHT_PARENTHESIS,
              "was expecting a method call to end with a right parenthesis"
          ).map(
              tk1 -> new MethodCall(
                  name,
                  methodCallParameterList
              )
          );
        }
    );
  }

  private Optional<Statement> parseLocationAndAssignExprOrMethodCall() {
    return consumeToken(
        ID,
        "Expected x = `expr` or fn()"
    ).flatMap(
        token -> {
          if (getCurrentTokenType() == LEFT_PARENTHESIS) {
            return parseMethodCall(token).flatMap(
                methodCall -> consumeToken(
                    SEMICOLON,
                    Scanner.SEMICOLON
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
                    Scanner.SEMICOLON
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
    return switch (token.tokenType) {
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
    return switch (token.tokenType) {
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
    TokenType expectedTokenType = getCurrentToken().tokenType;
    final var tokenOpt = consumeToken(expectedTokenType);
    if (tokenOpt.isPresent()) {
      var token = tokenOpt.get();
      if (expectedTokenType == INCREMENT)
        return Optional.of(new Increment(token.tokenPosition));
      else if (expectedTokenType == DECREMENT) {
        return Optional.of(new Decrement(token.tokenPosition));
      } else {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                token,
                "expected ++ or --, but received " + token.lexeme
            )
        );
      }
    }
    return Optional.empty();
  }

  private Optional<? extends AssignExpr> parseAssignExpr() {
    return switch (getCurrentToken().tokenType) {
      case ASSIGN, ADD_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN -> parseAssignOpExpr();
      case DECREMENT, INCREMENT -> parseIncrement();
      default -> {
        if (tokens.get(currentTokenIndex - 1)
                  .tokenType == ID) {
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
    return switch (getCurrentToken().tokenType) {
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

  private Optional<LocationArray> parseLocationArray(@NotNull Name name) {
    return consumeToken(
        LEFT_SQUARE_BRACKET,
        "expected a left square bracket (`[`) to open array[expr]"
    )
        .flatMap(tk -> parseOrExpr().flatMap(
            expression -> consumeToken(
                RIGHT_SQUARE_BRACKET,
                "expected a right square bracket (`]`) to close array[expr]"
            ).map(
                tk1 -> new LocationArray(
                    name,
                    expression
                )
            )
        ));
  }

  private Optional<? extends Location> parseLocation(@NotNull Token token) {
    if (getCurrentToken().tokenType == LEFT_SQUARE_BRACKET) {
      return parseLocationArray(new Name(
          token.lexeme,
          token.tokenPosition
      ));
    }
    return Optional.of(new LocationVariable(new Name(
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
    switch (getCurrentTokenType()) {
      case NOT, MINUS -> {
        return parseUnaryOpExpr();
      }
      case LEFT_PARENTHESIS -> {
        return parseParenthesizedExpression();
      }
      case RESERVED_LEN -> {
        return parseLen();
      }
      case DECIMAL_LITERAL -> {
        return parseLiteral(DECIMAL_LITERAL);
      }
      case HEX_LITERAL -> {
        return parseLiteral(HEX_LITERAL);
      }
      case CHAR_LITERAL -> {
        return parseLiteral(CHAR_LITERAL);
      }
      case RESERVED_FALSE -> {
        return parseLiteral(RESERVED_FALSE);
      }
      case RESERVED_TRUE -> {
        return parseLiteral(RESERVED_TRUE);
      }
      case ID -> {
        return parseLocationOrMethodCall();
      }
      default -> {
        errors.add(
            new ParserError(
                ParserError.ErrorType.UNEXPECTED_TOKEN,
                getCurrentToken(),
                "expected an expression"
            )
        );
        return Optional.empty();
      }
    }
  }

  private Optional<Len> parseLen() {
    var lenTokenOpt = consumeToken(
        RESERVED_LEN,
        Scanner.RESERVED_LEN
    );
    if (lenTokenOpt.isPresent()) {
      if (
          consumeToken(
              LEFT_PARENTHESIS,
              "expected a ( after " + Scanner.RESERVED_LEN
          ).isPresent()) {
        var nameOpt = parseName((Token t) -> {
          if (t.tokenType == RIGHT_PARENTHESIS) {
            return "cannot find len of nothing";
          } else {
            return "cannot find len of (..." + t.lexeme + ")";
          }
        });
        if (nameOpt.isPresent()) {
          if (consumeToken(
              RIGHT_PARENTHESIS,
              Scanner.RIGHT_PARENTHESIS
          ).isPresent()) {
            return Optional.of(new Len(
                lenTokenOpt.get()
                           .tokenPosition,
                nameOpt.get()
            ));
          }
        }
      }
    }
    return Optional.empty();

  }

  private Optional<Literal> parseLiteral(TokenType expectedLiteralType) {
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
          case HEX_LITERAL -> new HexLiteral(
              token.tokenPosition,
              token.lexeme
          );
          default -> new DecimalLiteral(
              token.tokenPosition,
              token.lexeme
          );
        });
  }

  private Optional<Return> parseReturnStatement() {
    return consumeToken(
        RESERVED_RETURN,
        "was promised a return statement by implementation"
    ).flatMap(
        token -> {
          if (getCurrentTokenType() == SEMICOLON) {
            consumeToken(
                SEMICOLON,
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
        Scanner.RESERVED_WHILE
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            Scanner.LEFT_PARENTHESIS
        ).flatMap(
            tk -> parseOrExpr().flatMap(
                expression -> consumeToken(
                    RIGHT_PARENTHESIS,
                    Scanner.RIGHT_PARENTHESIS
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
        "expected the reserved keyword `if` to denote beginning of if statement"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            "expected a `(` after " + Scanner.RESERVED_IF + " to denote beginning of if statement"
        ).flatMap(
            tk -> parseOrExpr().flatMap(
                expression -> consumeToken(
                    RIGHT_PARENTHESIS,
                    "expected `)` to close out if statement condition"
                ).flatMap(
                    tk1 -> parseBlock().flatMap(
                        block -> {
                          if (getCurrentTokenType() == RESERVED_ELSE) {
                            return consumeToken(
                                RESERVED_ELSE,
                                "implementation error: expected else statement"
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
        "expected the reserved keyword `for` to denote beginning of for statement"
    ).flatMap(
        token -> consumeToken(
            LEFT_PARENTHESIS,
            "expected a `(` after " + Scanner.RESERVED_FOR + " to denote beginning of for statement"
        ).flatMap(
            tk -> parseName("expected initialization variable").flatMap(
                initId -> consumeToken(
                    ASSIGN,
                    "expected `=` to split initialization variable and expression"
                ).flatMap(
                    tk1 -> parseOrExpr(
                    ).flatMap(
                        initExpr -> consumeToken(
                            SEMICOLON,
                            "expected a `;` after for statement initializer"
                        ).flatMap(
                            tk2 -> parseOrExpr().flatMap(
                                terminatingCondition -> consumeToken(
                                    SEMICOLON,
                                    "expected a `;` after for statement terminating condition"
                                ).flatMap(
                                    tk3 -> parseLocation().flatMap(
                                        updateLocation -> parseCompoundAssignExpr().flatMap(
                                            updateAssignExpr ->
                                                consumeToken(
                                                    RIGHT_PARENTHESIS,
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
        Scanner.RESERVED_BREAK
    ).flatMap(
        token -> consumeToken(
            SEMICOLON,
            Scanner.SEMICOLON
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
        Scanner.RESERVED_CONTINUE
    ).flatMap(
        token -> consumeToken(
            SEMICOLON,
            Scanner.SEMICOLON
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
        "expected a left curly brace to start a block"
    ).flatMap(
        token -> {
          var fieldDeclarationList = new ArrayList<FieldDeclaration>();
          var statementList = new ArrayList<Statement>();
          parseFieldDeclarations(fieldDeclarationList);
          parseStatements(statementList);
          return consumeToken(
              RIGHT_CURLY,
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
        "was promised keyword `import` by implementation"
    ).flatMap(token -> parseName(
        "expected valid import name not " + getCurrentToken().lexeme
    ).flatMap(
        importName -> consumeToken(
            SEMICOLON,
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
    return parseBuiltinFieldType().map(
                                      type -> parseName(
                                          Scanner.IDENTIFIER
                                      ).map(
                                          nameId -> {
                                            var variables = new ArrayList<Name>();
                                            var arrays = new ArrayList<Array>();
                                            parseFieldDeclarationGroup(
                                                variables,
                                                arrays,
                                                nameId
                                            );
                                            while (getCurrentTokenType() == COMMA) {
                                              consumeToken(
                                                  COMMA,
                                                  Scanner.COMMA
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
                                                    return new Name(
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

  private Optional<Name> parseName(
      String expected
  ) {
    return consumeToken(
        TokenType.ID,
        expected
    ).map(
        token -> new Name(
            token.lexeme,
            token.tokenPosition
        )
    );
  }

  private Optional<Name> parseName(Function<Token, String> errMessageProvider) {
    final var optionalToken = consumeToken(
        TokenType.ID,
        errMessageProvider
    );
    if (optionalToken.isPresent()) {
      final Token token = optionalToken.get();
      return Optional.of(new Name(
          token.lexeme,
          token.tokenPosition
      ));
    } else {
      errors.add(
          new ParserError(
              ParserError.ErrorType.MISSING_NAME,
              getCurrentToken(),
              errMessageProvider.apply(getCurrentToken())
          )
      );
      return Optional.empty();
    }
  }

  private Optional<Type> parseBuiltinFieldType() {
    final Token token = consumeTokenNoCheck();
    return switch (token.tokenType) {
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

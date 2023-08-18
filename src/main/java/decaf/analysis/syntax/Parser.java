package decaf.analysis.syntax;

import static decaf.analysis.Token.Type.ADD_ASSIGN;
import static decaf.analysis.Token.Type.ASSIGN;
import static decaf.analysis.Token.Type.CHAR_LITERAL;
import static decaf.analysis.Token.Type.COMMA;
import static decaf.analysis.Token.Type.CONDITIONAL_AND;
import static decaf.analysis.Token.Type.CONDITIONAL_OR;
import static decaf.analysis.Token.Type.DECREMENT;
import static decaf.analysis.Token.Type.DIVIDE;
import static decaf.analysis.Token.Type.EOF;
import static decaf.analysis.Token.Type.EQ;
import static decaf.analysis.Token.Type.GEQ;
import static decaf.analysis.Token.Type.GT;
import static decaf.analysis.Token.Type.ID;
import static decaf.analysis.Token.Type.INCREMENT;
import static decaf.analysis.Token.Type.INT_LITERAL;
import static decaf.analysis.Token.Type.LEFT_CURLY;
import static decaf.analysis.Token.Type.LEFT_PARENTHESIS;
import static decaf.analysis.Token.Type.LEFT_SQUARE_BRACKET;
import static decaf.analysis.Token.Type.LEQ;
import static decaf.analysis.Token.Type.LT;
import static decaf.analysis.Token.Type.MINUS;
import static decaf.analysis.Token.Type.MINUS_ASSIGN;
import static decaf.analysis.Token.Type.MOD;
import static decaf.analysis.Token.Type.MULTIPLY;
import static decaf.analysis.Token.Type.MULTIPLY_ASSIGN;
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

import decaf.analysis.Token;
import decaf.analysis.lexical.Scanner;
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
    this.root = program().map(
        program -> {
          if (getCurrentTokenType() != EOF && !hasError()) {
            errors.add(new ParserError(
                ParserError.ErrorType.DID_NOT_FINISH_PARSING,
                getCurrentToken(),
                "did not finish parsing the program"
            ));
          }
          return program;
        }
    ).orElse(new Program(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList()
    ));
    if (context.debugModeOn()) {
      Utils.printParseTree(root);
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

  public boolean hasError() {
    return !errors.isEmpty();
  }

  private Token getCurrentToken() {
    return tokens.get(currentTokenIndex);
  }

  private Token.Type getCurrentTokenType() {
    return getCurrentToken().type;
  }

  private Token consumeTokenNoCheck() {
    var token = getCurrentToken();
    currentTokenIndex += 1;
    return token;
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
      return Optional.of(consumeTokenNoCheck());
    }
  }

  private @NotNull Optional<Token> consumeToken(@NotNull Token.Type expectedType) {
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

  private @NotNull Optional<Token> consumeOneOfTokens(
      @NotNull ParserError.ErrorType errorType,
      @NotNull String errorMessage,
      @NotNull Token.Type... expectedTypes
  ) {
    for (Token.Type expectedType : expectedTypes) {
      if (getCurrentTokenType() == expectedType) {
        var token = getCurrentToken();
        currentTokenIndex += 1;
        return Optional.of(token);
      }
    }
    errors.add(new ParserError(
        errorType,
        getCurrentToken(),
        errorMessage
    ));
    return Optional.empty();
  }

  private boolean canParseField() {
    return (getCurrentTokenType() == RESERVED_BOOL ||
        getCurrentTokenType() == RESERVED_INT) &&
        currentTokenIndex + 1 < tokens.size() &&
        tokens.get(currentTokenIndex + 1).type == ID &&
        currentTokenIndex + 2 < tokens.size() &&
        tokens.get(currentTokenIndex + 2).type != LEFT_PARENTHESIS;
  }

  private boolean canParseMethod() {
    return (getCurrentTokenType() == RESERVED_BOOL ||
        getCurrentTokenType() == RESERVED_INT ||
        getCurrentTokenType() == RESERVED_VOID) &&
        currentTokenIndex + 1 < tokens.size() &&
        tokens.get(currentTokenIndex + 1).type == ID &&
        currentTokenIndex + 2 < tokens.size() &&
        tokens.get(currentTokenIndex + 2).type == LEFT_PARENTHESIS;
  }

  private boolean canParseArray() {
    return getCurrentTokenType() == ID &&
        tokens.get(currentTokenIndex + 1).type == LEFT_SQUARE_BRACKET;
  }

  private Optional<Program> program() {
    return parseImportDeclarations().flatMap(
        importDeclarations -> parseFieldDeclarations().flatMap(
            fieldDeclarations -> parseMethodDefinitions().map(
                methodDefinitions -> new Program(
                    importDeclarations,
                    fieldDeclarations,
                    methodDefinitions
                )
            )
        )
    );
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

  private Optional<List<ImportDeclaration>> parseImportDeclarations() {
    var importDeclarationList = new ArrayList<ImportDeclaration>();
    while (getCurrentTokenType() == RESERVED_IMPORT) {
      var importDeclaration = parseImportDeclaration();
      if (importDeclaration.isPresent()) {
        importDeclarationList.add(importDeclaration.get());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(importDeclarationList);
  }

  private Optional<List<FieldDeclaration>> parseFieldDeclarations() {
    var fieldDeclarations = new ArrayList<FieldDeclaration>();
    while (canParseField()) {
      var fieldDeclaration = parseFieldDeclaration();
      if (fieldDeclaration.isPresent()) {
        fieldDeclarations.add(fieldDeclaration.get());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(fieldDeclarations);
  }

  private Optional<List<MethodDefinition>> parseMethodDefinitions() {
    var methodDefinitions = new ArrayList<MethodDefinition>();
    while (canParseMethod()) {
      var methodDefinition = parseMethodDefinition();
      if (methodDefinition.isPresent()) {
        methodDefinitions.add(methodDefinition.get());
      } else {
        return Optional.empty();
      }
    }
    return Optional.of(methodDefinitions);
  }

  private Optional<Array> parseArray() {
    return parseName(
        "expected a valid identifier for the array"
    ).flatMap(arrayNameId -> consumeToken(
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
                    .map(tk1 -> new Array(
                        arrayNameId.tokenPosition,
                        intLiteral,
                        arrayNameId.getLabel()
                    )))));
  }

  private Optional<IntLiteral> parseIntLiteral() {
    return consumeToken(INT_LITERAL, ParserError.ErrorType.IMPLEMENTATION_ERROR,
                        "was promised an int literal by the implementation")
          .map(token -> new IntLiteral(token.tokenPosition, token.lexeme));
  }

  private Optional<List<Statement>> parseStatements() {
      var statements = new ArrayList<Statement>();
      while (true) {
        if (getCurrentTokenType() == RESERVED_RETURN) {
          var returnStatement = parseReturnStatement();
          if (returnStatement.isPresent()) {
            statements.add(returnStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == RESERVED_BREAK) {
          var breakStatement = parseBreakStatement();
          if (breakStatement.isPresent()) {
            statements.add(breakStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == RESERVED_CONTINUE) {
          var continueStatement = parseContinueStatement();
          if (continueStatement.isPresent()) {
            statements.add(continueStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == RESERVED_WHILE) {
          var whileStatement = parseWhileStatement();
          if (whileStatement.isPresent()) {
            statements.add(whileStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == RESERVED_IF) {
          var ifStatement = parseIfStatement();
          if (ifStatement.isPresent()) {
            statements.add(ifStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == RESERVED_FOR) {
          var forStatement = parseForStatement();
          if (forStatement.isPresent()) {
            statements.add(forStatement.get());
          } else {
            return Optional.empty();
          }
        } else if (getCurrentTokenType() == ID) {
          var statement = parseLocationAndAssignExprOrMethodCall();
          if (statement.isPresent()) {
            statements.add(statement.get());
          } else {
            return Optional.empty();
          }
        } else {
          break;
        }
      }
      return Optional.of(statements);
    }

  private Optional<Type> parseMethodReturnType() {
    return consumeOneOfTokens(
        ParserError.ErrorType.MISSING_RETURN_TYPE,
        "expected a valid return type, one of (int, bool, void) but found " +
            getCurrentToken().lexeme,
        RESERVED_BOOL,
        RESERVED_INT,
        RESERVED_VOID
    ).map(token -> switch (token.type) {
      case RESERVED_BOOL -> Type.Bool;
      case RESERVED_INT -> Type.Int;
      case RESERVED_VOID -> Type.Void;
      default -> Type.Undefined;
    });
  }

  private Optional<FormalArgument> parseFormalArgument() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    return parseType("expected a valid type, one of (int, bool) but found ")
        .flatMap(type -> parseName(
            "expected a valid name for the method argument but found"
        ).map(
            nameId -> new FormalArgument(
                tokenPosition,
                nameId.getLabel(),
                type
            )
        ));
  }

  private Optional<List<FormalArgument>> parseFormalArguments() {
    return consumeToken(
        LEFT_PARENTHESIS,
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected a left parenthesis `(` to open a method definition"
    ).flatMap(tk ->
              {
                if (getCurrentTokenType() == RIGHT_PARENTHESIS) {
                  return consumeToken(
                      RIGHT_PARENTHESIS,
                      ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                      "expected a right parenthesis `)` to close a method definition"
                  ).map(tk1 -> Collections.emptyList());
                } else if (getCurrentTokenType() == RESERVED_INT ||
                    getCurrentTokenType() == RESERVED_BOOL) {
                  var formalArguments = new ArrayList<FormalArgument>();
                  do {
                    if (getCurrentTokenType() == COMMA) {
                      consumeToken(COMMA);
                    }
                    var formalArgument = parseFormalArgument();
                    if (formalArgument.isPresent()) {
                      formalArguments.add(formalArgument.get());
                    } else {
                      return Optional.empty();
                    }
                  } while (getCurrentTokenType() == COMMA);
                  return consumeToken(
                      RIGHT_PARENTHESIS,
                      ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                      "expected a right parenthesis `)` to close a method definition"
                  ).map(
                      tk1 -> formalArguments
                  );
                } else {
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
                  return Optional.empty();
                }
              });
  }

  private Optional<MethodDefinition> parseMethodDefinition() {
    final var tokenPosition = getCurrentToken().tokenPosition;
    return parseMethodReturnType().flatMap(
        returnType -> parseName("expected method to have an identifier").flatMap(
            nameId -> parseFormalArguments().flatMap(
                formalArguments -> parseBlock().map(
                    block -> new MethodDefinition(
                        tokenPosition,
                        returnType,
                        formalArguments,
                        nameId,
                        block
                    )
                )
            )));
  }

  private Optional<ActualArgument> parseActualArgument() {
    if (getCurrentTokenType() == STRING_LITERAL) {
      return consumeToken(
          STRING_LITERAL,
          ParserError.ErrorType.IMPLEMENTATION_ERROR,
          "was promised a string literal by implementation"
      ).map(
          token -> new StringLiteral(
              token.tokenPosition,
              token.lexeme
          ));
    } else {
      return parseOrExpr().map(ExpressionParameter::new);
    }
  }

  private Optional<List<ActualArgument>> parseActualArguments() {
    if (getCurrentTokenType() == RIGHT_PARENTHESIS)
      return Optional.of(Collections.emptyList());
    else {
      var actualArguments = new ArrayList<ActualArgument>();
      do {
        if (getCurrentTokenType() == COMMA) {
          consumeToken(COMMA);
        }
        var actualArgument = parseActualArgument();
        if (actualArgument.isPresent()) {
          actualArguments.add(actualArgument.get());
        } else {
          return Optional.empty();
        }
      } while (getCurrentTokenType() == COMMA);
      return Optional.of(actualArguments);
    }
  }

  private Optional<MethodCall> parseMethodCall(@NotNull RValue RValue) {
    return consumeToken(
        LEFT_PARENTHESIS,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "was expecting a method call to start with a left parenthesis: `(`"
    ).flatMap(
        tk -> parseActualArguments().flatMap(
            actualArguments -> consumeToken(
                RIGHT_PARENTHESIS,
                ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                "was expecting a method call to end with a right parenthesis `)`"
            ).map(
                tk1 -> new MethodCall(
                    RValue,
                    actualArguments
                )
            )
        ));
  }

  private Optional<Statement> parseLocationAndAssignExprOrMethodCall() {
    return consumeToken(
        ID,
        ParserError.ErrorType.IMPLEMENTATION_ERROR,
        "Expected a valid identifier, `var` or fn_name()"
    ).flatMap(
        token -> {
          if (getCurrentTokenType() == LEFT_PARENTHESIS) {
            return parseMethodCall(new RValue(
                token.lexeme,
                token.tokenPosition
            )).flatMap(
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
    return consumeOneOfTokens(
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected =, +=, -=, *= but found " + getCurrentToken().lexeme,
        ASSIGN,
        ADD_ASSIGN,
        MINUS_ASSIGN,
        MULTIPLY_ASSIGN
    ).map(
        token -> new AssignOperator(
            token.tokenPosition,
            token.lexeme
        )
    );
  }

  private Optional<CompoundAssignOperator> parseCompoundAssignOp() {
    return consumeOneOfTokens(
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected +=, -=, *= but found " + getCurrentToken().lexeme,
        ADD_ASSIGN,
        MINUS_ASSIGN,
        MULTIPLY_ASSIGN
    ).map(
        token -> new CompoundAssignOperator(
            token.tokenPosition,
            token.lexeme
        )
    );
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
    return consumeOneOfTokens(
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected ++ or -- but found " + getCurrentToken().lexeme,
        INCREMENT,
        DECREMENT
    ).map(
        token -> (token.type == INCREMENT) ? new Increment(
            token.tokenPosition): new Decrement(token.tokenPosition));
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
            tk -> parseName("len can only be applied to arrays, not " + getCurrentToken().type).flatMap(
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

  private Optional<Break> parseBreakStatement() {
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

  private Optional<Continue> parseContinueStatement() {
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

  private Optional<Block> parseBlock() {
    return consumeToken(
        LEFT_CURLY,
        ParserError.ErrorType.UNEXPECTED_TOKEN,
        "expected a left curly brace to start a block"
    ).flatMap(
        token -> parseFieldDeclarations().flatMap(
            fieldDeclarations -> parseStatements().flatMap(
                statements -> consumeToken(
                    RIGHT_CURLY,
                    ParserError.ErrorType.UNCLOSED_PARENTHESIS,
                    "expected a right curly brace to end a block"
                ).map(
                    rightCurlyToken -> new Block(
                        fieldDeclarations,
                        statements
                    )
                )
            )
        ));
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
    return parseType("expected a valid type, one of (int, bool) but found " + getCurrentToken().lexeme)
        .flatMap(this::parseFieldDeclarationWithType);
  }

  private Optional<FieldDeclaration> parseFieldDeclarationWithType(@NotNull Type type) {
    assert type == Type.Int || type == Type.Bool;

    var tokenPosition = getCurrentToken().tokenPosition;
    var variables = new ArrayList<RValue>();
    var arrays = new ArrayList<Array>();

    if (canParseArray()) {
      parseArray().ifPresent(arrays::add);
    } else {
      parseName("expected a valid identifier").ifPresent(variables::add);
    }

    while (getCurrentTokenType() == COMMA) {
      consumeToken(
          COMMA,
          ParserError.ErrorType.UNEXPECTED_TOKEN,
          "expected a comma to separate field declarations"
      );
       if (canParseArray()) {
         parseArray().ifPresent(arrays::add);
       } else {
         parseName("expected a valid identifier").ifPresent(variables::add);
       }
     }

    return consumeToken(
        SEMICOLON,
        ParserError.ErrorType.MISSING_SEMICOLON,
        "expected a semicolon to terminate a field declaration"
    ).map(tk -> new FieldDeclaration(
        tokenPosition,
        type,
        variables,
        arrays
    ));

  }

  private Optional<RValue> parseName(@NotNull String errorMessage) {
    return consumeToken(
        ID,
        ParserError.ErrorType.MISSING_NAME,
        errorMessage
    ).map(
        token -> new RValue(
            token.lexeme,
            token.tokenPosition
        )
    );
  }

  private Optional<Type> parseType(@NotNull String errorMessage) {
    return consumeOneOfTokens(
        ParserError.ErrorType.INVALID_FIELD_TYPE,
        errorMessage,
        RESERVED_INT,
        RESERVED_BOOL
    ).map(
        token -> switch (token.type) {
          case RESERVED_INT -> Type.Int;
          case RESERVED_BOOL -> Type.Bool;
          default -> Type.Undefined;
        }
    );
  }
}

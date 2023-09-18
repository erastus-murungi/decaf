package decaf.analysis.semantic;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.*;
import decaf.analysis.syntax.ast.types.ArrayType;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import decaf.shared.CompilationContext;
import decaf.shared.Utils;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static decaf.shared.errors.SemanticError.ErrorType;

public class SemanticChecker implements AstVisitor<Type> {
    @NotNull
    private final Scope globalScope = Scope.forGlobals();
    @NotNull
    private final CompilationContext context;

    // BEGIN: state variables

    // the statically resolved return type of the method we are currently visiting
    private Type inferredReturnType;
    // the number of nested while/for loops we are in
    private int loopDepth = 0;

    // END: state variables

    public SemanticChecker(@NotNull Program root, @NotNull CompilationContext context) {
        this.context = context;
        visit(root, getGlobalScope());
        if (context.debugModeOn()) {
            context.printSemanticErrors();
        }
        context.setGlobalScope(getGlobalScope());
    }

    private void logSemanticError(TokenPosition tokenPosition, ErrorType errorType, String message) {
        context.logSemanticError(tokenPosition, errorType, message);
    }

    @Override
    public Type visit(IntLiteral intLiteral, Scope scope) {
        checkIntBounds(intLiteral);
        return Type.getIntType();
    }

    @Override
    public Type visit(BooleanLiteral booleanLiteral, Scope scope) {
        return Type.getBoolType();
    }

    @Override
    public Type visit(@NotNull MethodDefinition methodDefinition, @NotNull Scope scope) {
        final var methodId = methodDefinition.getName();
        final var block = methodDefinition.getBody();

        var formalArgumentScope = Scope.forArguments(getGlobalScope());
        if (getGlobalScope().containsKey(methodId)) {

            logSemanticError(methodDefinition.getTokenPosition(),
                             ErrorType.METHOD_ALREADY_DEFINED,
                             String.format("method ``%s`` already defined", methodId)
                            );
            return Type.getUnsetType();
        } else {
            for (var formalArgument : methodDefinition.getFormalArguments()) {
                formalArgument.accept(this, formalArgumentScope);
            }
            getGlobalScope().addDescriptor(methodId,
                                           new MethodDescriptor(methodDefinition, formalArgumentScope)
                                          );
            setInferredReturnType(Type.getUnsetType());
            block.accept(this, formalArgumentScope);
            if (getInferredReturnType() != Type.getUnsetType() &&
                methodDefinition.getReturnType() != getInferredReturnType()) {
                logSemanticError(methodDefinition.getTokenPosition(),
                                 ErrorType.SHOULD_RETURN_VOID,
                                 String.format("method `%s` must not return a value of type `%s` in a void method",
                                               methodId,
                                               getInferredReturnType()
                                              )
                                );
            }
            setInferredReturnType(Type.getUnsetType());
            return methodDefinition.getReturnType();
        }
    }

    @Override
    public Type visit(@NotNull ImportDeclaration importDeclaration, @NotNull Scope scope) {
        if (scope.isShadowingParameter(importDeclaration.importName.getLabel())) {
            logSemanticError(importDeclaration.importName.tokenPosition,
                             ErrorType.SHADOWING_FORMAL_ARGUMENT,
                             String.format("Import identifier `%s` shadows a parameter",
                                           importDeclaration.importName.getLabel()
                                          )
                            );
        } else if (getGlobalScope().lookup(importDeclaration.importName.getLabel()).isPresent()) {
            logSemanticError(new TokenPosition(0, 0, 0),
                             ErrorType.IDENTIFIER_ALREADY_DECLARED,
                             String.format("import identifier `%s` already declared",
                                           importDeclaration.importName.getLabel()
                                          )
                            );
        }
        getGlobalScope().addDescriptor(importDeclaration.importName.getLabel(), Descriptor.forImport());
        return Type.getUnsetType();
    }

    @Override
    public Type visit(For forStatement, Scope scope) {
        return scope.lookup(forStatement.getInitialization().initLocation.getLabel()).map(initDescriptor -> {
            if (!initDescriptor.typeIs(Type.getIntType())) {
                logSemanticError(forStatement.tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 String.format("init location `%s` must have type int, not `%s`",
                                               forStatement.getInitialization().initLocation.getLabel(),
                                               initDescriptor.getType()
                                              )
                                );
            }

            var type = forStatement.getInitialization().initExpression.accept(this, scope);
            if (type != Type.getIntType()) {
                logSemanticError(forStatement.getInitialization().initExpression.tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 "init expression must evaluate to an int"
                                );
            }

            var testType = forStatement.getTerminatingCondition().accept(this, scope);
            if (testType != Type.getBoolType()) {
                logSemanticError(forStatement.getTerminatingCondition().tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 String.format("for-loop test must evaluate to %s not %s", Type.getBoolType(), testType)
                                );
            }
            ++loopDepth;
            forStatement.getBlock().accept(this, scope);
            --loopDepth;
            var updateId = forStatement.getUpdate().getLocation().getLabel();
            var updatingDescriptorOpt = scope.lookupNonMethod(updateId);
            if (updatingDescriptorOpt.isPresent()) {
                var updatingDescriptor = updatingDescriptorOpt.get();
                if (!updatingDescriptor.typeIs(Type.getIntType())) {
                    logSemanticError(forStatement.getInitialization().initExpression.tokenPosition,
                                     ErrorType.UNSUPPORTED_TYPE,
                                     String.format("The location `%s` in the for-loop `%s` must have type int, not `%s`",
                                                   updateId,
                                                   forStatement,
                                                   updatingDescriptor.getType()
                                                  )
                                    );
                }
                var updateExprType = forStatement.getUpdate().assignExpr.accept(this, scope);
                if (forStatement.getUpdate().assignExpr instanceof CompoundAssignOpExpr) {
                    updateExprType = forStatement.getUpdate().assignExpr.expression.getType();
                }
                if (updateExprType != Type.getIntType()) {
                    logSemanticError(forStatement.getUpdate().assignExpr.getTokenPosition(),
                                     ErrorType.UNSUPPORTED_TYPE,
                                     String.format(
                                             "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                                             "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                             updateId,
                                             updatingDescriptor.getType(),
                                             forStatement.getUpdate().assignExpr.getSourceCode(),
                                             updateExprType
                                                  )
                                    );
                }
            } else {
                logSemanticError(forStatement.getUpdate().getLocation().tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 String.format("The location `%s` in the for-loop `%s` must be declared in scope",
                                               updateId,
                                               forStatement
                                              )
                                );
            }
            return Type.getUnsetType();
        }).orElseGet(() -> {
            logSemanticError(forStatement.getInitialization().initLocation.tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             String.format("The location `%s` in the for-loop `%s` must be declared in scope",
                                           forStatement.getInitialization().initLocation.getLabel(),
                                           forStatement
                                          )
                            );
            return Type.getUnsetType();
        });
    }

    @Override
    public Type visit(@NotNull While whileStatement, @NotNull Scope scope) {
        var type = whileStatement.getTest().accept(this, scope);
        if (type != Type.getBoolType()) {
            logSemanticError(whileStatement.getTest().tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             String.format("while statement test must evaluate to a bool, not `%s`", type)
                            );
        }
        ++loopDepth;
        whileStatement.getBody().accept(this, scope);
        --loopDepth;
        return Type.getUnsetType();
    }


    private void checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
        for (var methodDefinition : methodDefinitionList) {
            if (methodDefinition.getName().equals("main")) {
                if (methodDefinition.getReturnType() == Type.getVoidType()) {
                    if (!(methodDefinition.getFormalArguments().isEmpty())) {
                        logSemanticError(methodDefinition.getTokenPosition(),
                                         ErrorType.INVALID_MAIN_METHOD,
                                         String.format("main method must have no parameters, yours has: %s",
                                                       Utils.prettyPrintMethodFormalArguments(methodDefinition.getFormalArguments().toList())
                                                      )
                                        );
                    }
                } else {
                    logSemanticError(methodDefinition.getTokenPosition(),
                                     ErrorType.INVALID_MAIN_METHOD,
                                     String.format("main method must have return type void, yours has: %s",
                                                   methodDefinition.getReturnType()
                                                  )
                                    );
                }
                return;
            }
        }
        logSemanticError(new TokenPosition(0, 0, 0), ErrorType.MISSING_MAIN_METHOD, "main method not found");
    }

    @Override
    public Type visit(Program program, Scope scope) {
        checkMainMethodExists(program.getMethodDefinitions());
        for (var importDeclaration : program.getImportDeclaration()) {
            importDeclaration.accept(this, scope);
        }
        for (var fieldDeclaration : program.getFieldDeclarations()) {
            fieldDeclaration.accept(this, scope);
        }
        for (var methodDefinition : program.getMethodDefinitions()) {
            methodDefinition.accept(this, scope);
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(UnaryOpExpression unaryOpExpression, Scope scope) {
        var operandType = unaryOpExpression.operand.accept(this, scope);
        if (operandType != Type.getBoolType() && operandType != Type.getIntType()) {
            logSemanticError(unaryOpExpression.tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             String.format("operand of unary operator must be either bool or int, not `%s`",
                                           operandType
                                          )
                            );
        }
        if (operandType != Type.getBoolType() && unaryOpExpression.getUnaryOperator().label.equals("!")) {
            logSemanticError(unaryOpExpression.tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             "can only use a not operator on booleans"
                            );
        }
        if (operandType != Type.getIntType() && unaryOpExpression.getUnaryOperator().label.equals("-")) {
            logSemanticError(unaryOpExpression.tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             "can only use a unary minus operator on ints"
                            );
        }
        unaryOpExpression.setType(operandType);
        return operandType;
    }

    @Override
    public Type visit(BinaryOpExpression binaryOpExpression, Scope scope) {
        var leftType = binaryOpExpression.lhs.accept(this, scope);
        var rightType = binaryOpExpression.rhs.accept(this, scope);
        if (leftType.isPrimitiveType()) {
            if (rightType.isPrimitiveType()) {
                if (binaryOpExpression.op instanceof ArithmeticOperator) {
                    if (leftType == Type.getIntType() && rightType == Type.getIntType()) {
                        binaryOpExpression.setType(Type.getIntType());
                    } else if (leftType != Type.getIntType()) {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type int not `%s`", leftType)
                                        );
                    } else {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type int not `%s`", rightType)
                                        );
                    }
                } else if (binaryOpExpression.op instanceof EqualityOperator) {
                    if (leftType.equals(rightType) &&
                        (leftType == Type.getIntType() || leftType == Type.getBoolType())) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format(
                                                 "operands of %s must have same type, either both bool or both int, not `%s` and `%s`",
                                                 binaryOpExpression.op.getSourceCode(),
                                                 leftType,
                                                 rightType
                                                      )
                                        );
                    }
                } else if (binaryOpExpression.op instanceof RelationalOperator) {
                    if (leftType.equals(Type.getIntType()) && rightType.equals(Type.getIntType())) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else if (leftType != Type.getIntType()) {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type int not `%s`", leftType)
                                        );
                    } else {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type int not `%s`", rightType)
                                        );
                    }
                } else if (binaryOpExpression.op instanceof ConditionalOperator) {
                    if (leftType == Type.getBoolType() && rightType == Type.getBoolType()) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else if (leftType != Type.getBoolType()) {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type bool not `%s`", leftType)
                                        );
                    } else {
                        logSemanticError(binaryOpExpression.tokenPosition,
                                         ErrorType.UNSUPPORTED_TYPE,
                                         String.format("RHS must have type bool not `%s`", rightType)
                                        );
                    }
                }
            } else {
                logSemanticError(binaryOpExpression.tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 String.format("RHS must be a primitive type, not `%s`", rightType)
                                );
            }
        } else {
            logSemanticError(binaryOpExpression.tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             String.format("RHS must be a primitive type, not `%s`", leftType)
                            );
        }
        return binaryOpExpression.getType();
    }

    @Override
    public Type visit(@NotNull Block block, @NotNull Scope parentScope) {
        var localScope = Scope.forBlock(block, parentScope);
        for (var fieldDeclaration : block.getFieldDeclarations()) {
            fieldDeclaration.accept(this, localScope);
        }
        for (Statement statement : block.getStatements()) {
            statement.accept(this, localScope);
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(ParenthesizedExpression parenthesizedExpression, Scope scope) {
        return parenthesizedExpression.expression.accept(this, scope);
    }

    @Override
    public Type visit(LocationArray locationArray, Scope scope) {
        if (locationArray.expression.accept(this, scope) != Type.getIntType()) {
            logSemanticError(locationArray.tokenPosition,
                             ErrorType.INVALID_ARRAY_INDEX,
                             "array index must evaluate to int"
                            );
        }
        return scope.lookup(locationArray.getLabel()).map(descriptor -> {
            if (descriptor.isForArray()) {
                var type = descriptor.getType();
                assert type.isDerivedArrayType();
                return ((ArrayType) type).getContainedType();
            } else {
                logSemanticError(locationArray.tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 String.format("`%s` was declared as %s but it being indexed like an array",
                                               locationArray.getLabel(),
                                               descriptor.getType()
                                              )
                                );
                return descriptor.getType();
            }
        }).orElseGet(() -> {
            logSemanticError(locationArray.tokenPosition,
                             ErrorType.IDENTIFIER_NOT_IN_SCOPE,
                             String.format("identifier `%s` in an array access must be declared in scope",
                                           locationArray.getLabel()
                                          )
                            );
            return Type.getUnsetType();
        });
    }

    @Override
    public Type visit(ExpressionParameter expressionParameter, Scope scope) {
        var type = expressionParameter.expression.accept(this, scope);
        expressionParameter.setType(type);
        return type;
    }

    @Override
    public Type visit(@NotNull If ifStatement, @NotNull Scope scope) {
        var typeTest = ifStatement.getCondition().accept(this, scope);
        if (typeTest != Type.getBoolType()) {
            logSemanticError(ifStatement.getCondition().tokenPosition,
                             ErrorType.UNSUPPORTED_TYPE,
                             String.format("if statement test must evaluate to a bool, not `%s`", typeTest)
                            );
        }
        ifStatement.getThenBlock().accept(this, scope);
        if (ifStatement.getElseBlock().isPresent()) {
            ifStatement.getElseBlock().get().accept(this, scope);
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(Return returnStatement, Scope scope) {
        var type = returnStatement.retExpression.accept(this, scope);
        setInferredReturnType(type);
        return Type.getVoidType();
    }

    @Override
    public Type visit(Array array, Scope scope) {
        if (array.getSize().convertToLong() <= 0) {
            logSemanticError(array.getSize().tokenPosition,
                             ErrorType.INVALID_ARRAY_INDEX,
                             "The int literal in an array declaration must be greater than 0"
                            );
        }
        return Type.getUnsetType();
    }

    public Type visit(@NotNull FormalArgument formalArgument, @NotNull Scope scope) {
        var formalArgumentId = formalArgument.getName();
        if (scope.isShadowingParameter(formalArgumentId)) {
            logSemanticError(formalArgument.getTokenPosition(),
                             ErrorType.SHADOWING_FORMAL_ARGUMENT,
                             String.format("Formal argument `%s` shadows a parameter", formalArgumentId)
                            );
        } else {
            scope.addDescriptor(formalArgumentId, Descriptor.forFormalArgument(formalArgument.getType()));
        }
        return formalArgument.getType();
    }

    private void checkNumberOfArgumentsAndTypesMatch(MethodDefinition methodDefinition, MethodCall methodCall) {
        if (methodCall.actualArgumentList.size() != methodDefinition.getFormalArguments().size()) {
            logSemanticError(methodCall.tokenPosition,
                             ErrorType.MISMATCHING_NUMBER_OR_ARGUMENTS,
                             String.format("method %s expects %d arguments but %d were passed in",
                                           methodCall.methodId.getLabel(),
                                           methodDefinition.getFormalArguments().size(),
                                           methodCall.actualArgumentList.size()
                                          )
                            );
            return;
        }
        for (int argIndex = 0; argIndex < methodCall.actualArgumentList.size(); argIndex++) {
            final var formalArgument = methodDefinition.getFormalArguments().get(argIndex);
            final var actualArgument = methodCall.actualArgumentList.get(argIndex);
            if (actualArgument.getType() != formalArgument.getType()) {
                logSemanticError(methodCall.tokenPosition,
                                 ErrorType.INCORRECT_ARG_TYPE,
                                 String.format("method %s expects argument %d to be of type %s but %s was passed in",
                                               methodCall.methodId.getLabel(),
                                               argIndex + 1,
                                               formalArgument.getType(),
                                               actualArgument.getType()
                                              )
                                );
            }
        }
    }

    private void visitMethodCallParameters(List<ActualArgument> actualArgumentList, Scope scope) {
        for (var actualArgument : actualArgumentList) {
            actualArgument.accept(this, scope);
        }
    }

    @Override
    public Type visit(@NotNull MethodCall methodCall, @NotNull Scope scope) {
        var methodName = methodCall.methodId.getLabel();
        return getGlobalScope().lookup(methodName).map(descriptor -> {
            if (scope.containsKey(methodName)) {
                logSemanticError(methodCall.tokenPosition,
                                 ErrorType.METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER,
                                 String.format("method call to `%s` conflicts with locally defined identifier",
                                               methodName
                                              )
                                );
            }
            visitMethodCallParameters(methodCall.actualArgumentList, scope);
            if (descriptor instanceof MethodDescriptor methodDescriptor) {
                checkNumberOfArgumentsAndTypesMatch(methodDescriptor.methodDefinition, methodCall);
            }
            methodCall.setType(descriptor.getType());
            return descriptor.getType();


        }).orElseGet(() -> {
            logSemanticError(methodCall.tokenPosition,
                             ErrorType.METHOD_DEFINITION_NOT_FOUND,
                             String.format("method `%s` not found in scope", methodName)
                            );
            return Type.getUnsetType();
        });
    }

    @Override
    public Type visit(MethodCallStatement methodCallStatement, Scope scope) {
        final var methodId = methodCallStatement.methodCall.methodId.getLabel();
        if (getGlobalScope().lookupMethod(methodId).isEmpty() && getGlobalScope().lookupImport(methodId).isEmpty()) {
            logSemanticError(methodCallStatement.tokenPosition,
                             ErrorType.IDENTIFIER_NOT_IN_SCOPE,
                             String.format("identifier `%s` in a method statement must be a declared method or import",
                                           methodId
                                          )
                            );
        } else {
            methodCallStatement.methodCall.accept(this, scope);
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(LocationAssignExpr locationAssignExpr, Scope scope) {
        final var location = locationAssignExpr.location;
        return scope.lookupNonMethod(location.getLabel()).map(descriptor -> {
            if (descriptor.isForArray()) {
                if (location instanceof LocationArray locationArray) {
                    final var indexType = locationArray.expression.accept(this, scope);
                    if (indexType != Type.getIntType()) {
                        logSemanticError(locationAssignExpr.tokenPosition,
                                         ErrorType.INVALID_ARRAY_INDEX,
                                         "array index must evaluate to an int"
                                        );
                    }
                }
            }
            final var locationType = location.accept(this, scope);
            final Type expressionType = locationAssignExpr.assignExpr.accept(this, scope);
            if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
                if (assignOpExpr.assignOp.label.equals(Scanner.ASSIGN)) {
                    if (locationType != expressionType) {
                        logSemanticError(locationAssignExpr.tokenPosition,
                                         ErrorType.TYPE_MISMATCH,
                                         String.format(
                                                 "The location and the expression in an assignment must have the same type, " +
                                                 "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                                 location.getLabel(),
                                                 locationType,
                                                 locationAssignExpr.assignExpr.expression.getSourceCode(),
                                                 expressionType
                                                      )
                                        );
                    }
                }
            }
            if (assignOperatorEquals(locationAssignExpr, Scanner.ADD_ASSIGN) ||
                assignOperatorEquals(locationAssignExpr, Scanner.MINUS_ASSIGN) ||
                assignOperatorEquals(locationAssignExpr, Scanner.MULTIPLY_ASSIGN) ||
                locationAssignExpr.assignExpr instanceof Decrement ||
                locationAssignExpr.assignExpr instanceof Increment) {
                if (!((descriptor.getType() == Type.getIntType()) && expressionType == Type.getIntType())) {
                    logSemanticError(locationAssignExpr.tokenPosition,
                                     ErrorType.UNSUPPORTED_TYPE,
                                     String.format(
                                             "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                                             "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                             locationAssignExpr.location.getLabel(),
                                             descriptor.getType(),
                                             locationAssignExpr.assignExpr.getSourceCode(),
                                             expressionType
                                                  )
                                    );
                }
            }
            return Type.getUnsetType();
        }).orElseGet(() -> {
            logSemanticError(locationAssignExpr.tokenPosition,
                             ErrorType.IDENTIFIER_NOT_IN_SCOPE,
                             String.format("id `%s` not in scope", location.getLabel())
                            );
            return Type.getUnsetType();
        });
    }

    private boolean assignOperatorEquals(LocationAssignExpr locationAssignExpr, String opStr) {
        return (locationAssignExpr.assignExpr instanceof AssignOpExpr &&
                ((AssignOpExpr) locationAssignExpr.assignExpr).assignOp.label.equals(opStr)) ||
               (locationAssignExpr.assignExpr instanceof CompoundAssignOpExpr &&
                ((CompoundAssignOpExpr) locationAssignExpr.assignExpr).compoundAssignOp.label.equals(opStr));
    }

    @Override
    public Type visit(AssignOpExpr assignOpExpr, Scope scope) {
        return assignOpExpr.expression.accept(this, scope);
    }

    @Override
    public Type visit(RValue RValue, Scope scope) {
        return Type.getUnsetType();
    }

    @Override
    public Type visit(@NotNull LocationVariable locationVariable, @NotNull Scope scope) {
        return scope.lookup(locationVariable.getLabel()).map(descriptor -> {
            locationVariable.setType(descriptor.getType());
            return locationVariable.getType();
        }).orElseGet(() -> {
            logSemanticError(locationVariable.tokenPosition,
                             ErrorType.IDENTIFIER_NOT_IN_SCOPE,
                             String.format("No identifier can be used before it is declared: %s not in scope",
                                           locationVariable.getLabel()
                                          )
                            );
            return Type.getUnsetType();
        });
    }

    @Override
    public Type visit(Len len, Scope scope) {
        final var arrayLabel = len.getArrayLabel();
        return scope.lookup(arrayLabel).map(descriptor -> {
            if (!(descriptor.isForArray())) {
                logSemanticError(len.tokenPosition,
                                 ErrorType.UNSUPPORTED_TYPE,
                                 "the argument of the len operator must be an array"
                                );
            }
            return Type.getIntType();
        }).orElseGet(() -> {
            logSemanticError(len.tokenPosition,
                             ErrorType.IDENTIFIER_NOT_IN_SCOPE,
                             String.format("No identifier can be used before it is declared: %s not in scope",
                                           arrayLabel
                                          )
                            );
            return Type.getIntType();
        });
    }

    @Override
    public Type visit(Increment increment, Scope scope) {
        return Type.getIntType();
    }

    @Override
    public Type visit(Decrement decrement, Scope scope) {
        return Type.getIntType();
    }

    @Override
    public Type visit(CharLiteral charLiteral, Scope scope) {
        charLiteral.setType(Type.getIntType());
        return Type.getIntType();
    }

    @Override
    public Type visit(StringLiteral stringLiteral, Scope scope) {
        return Type.getStringType();
    }

    @Override
    public Type visit(CompoundAssignOpExpr compoundAssignOpExpr, Scope curScope) {
        compoundAssignOpExpr.expression.setType(compoundAssignOpExpr.expression.accept(this, curScope));
        return Type.getUnsetType();
    }

    @Override
    public Type visit(Initialization initialization, Scope scope) {
        return Type.getUnsetType();
    }

    @Override
    public Type visit(Assignment assignment, Scope scope) {
        return Type.getUnsetType();
    }

    @Override
    public Type visit(VoidExpression voidExpression, Scope scope) {
        return Type.getVoidType();
    }

    @Override
    public Type visit(Type type, Scope scope) {
        throw new UnsupportedOperationException("what is the type of a type?");
    }

    @Override
    public Type visit(FormalArguments formalArguments, Scope scope) {
        formalArguments.accept(this, scope);
        return Type.getUnsetType();
    }

    private void checkIntBounds(@NotNull IntLiteral intLiteral) {
        try {
            intLiteral.convertToLong();
        } catch (Exception e) {
            logSemanticError(intLiteral.tokenPosition,
                             ErrorType.INT_LITERAL_TOO_BIG,
                             "Encountered int literal that's out of bounds; -9223372036854775808 <= x <= 9223372036854775807"
                            );
        }
    }

    public @Nullable Type getInferredReturnType() {
        return inferredReturnType;
    }

    public void setInferredReturnType(@Nullable Type inferredReturnType) {
        this.inferredReturnType = inferredReturnType;
    }

    public Type visit(@NotNull FieldDeclaration fieldDeclaration, @NotNull Scope scope) {
        var type = fieldDeclaration.getType();
        for (var rValue : fieldDeclaration.vars) {
            if (scope.isShadowingParameter(rValue.getLabel())) {
                logSemanticError(fieldDeclaration.getTokenPosition(),
                                 ErrorType.SHADOWING_FORMAL_ARGUMENT,
                                 String.format("`%s` shadows a parameter", rValue.getLabel())
                                );
            } else if (scope.containsKey(rValue.getLabel())) {
                logSemanticError(fieldDeclaration.getTokenPosition(),
                                 ErrorType.IDENTIFIER_ALREADY_DECLARED,
                                 String.format("`%s` already declared", rValue.getLabel())
                                );
            } else {
                scope.addDescriptor(rValue.getLabel(), Descriptor.forValue(type));
            }
        }

        for (var array : fieldDeclaration.arrays) {
            var arrayIdLabel = array.getLabel();
            if (scope.isShadowingParameter(array.getLabel())) {
                logSemanticError(fieldDeclaration.getTokenPosition(),
                                 ErrorType.SHADOWING_FORMAL_ARGUMENT,
                                 String.format("`%s` shadows a parameter", arrayIdLabel)
                                );
            } else if (scope.lookup(array.getLabel()).isPresent()) {
                logSemanticError(fieldDeclaration.getTokenPosition(),
                                 ErrorType.IDENTIFIER_ALREADY_DECLARED,
                                 String.format("`%s` already declared", arrayIdLabel)
                                );
            } else {
                if (Integer.parseInt(array.getSize().literal) <= 0) {
                    logSemanticError(array.getSize().tokenPosition,
                                     ErrorType.INVALID_ARRAY_SIZE,
                                     String.format("declared array size must be greater than 0, not `%s`",
                                                   array.getSize().literal
                                                  )
                                    );
                } else {
                    scope.addDescriptor(arrayIdLabel,
                                        Descriptor.forArray(ArrayType.get(type, array.getSize().convertToLong()))
                                       );
                }
            }
        }
        return Type.getUnsetType();
    }

    public @NotNull Type visit(@NotNull Break breakStatement, @NotNull Scope scope) {
        if (loopDepth < 1) {
            logSemanticError(breakStatement.tokenPosition,
                             ErrorType.BREAK_OUT_OF_CONTEXT,
                             "break statement must be enclosed within a loop"
                            );
        }
        return Type.getUnsetType();
    }

    public @NotNull Type visit(@NotNull Continue continueStatement, @NotNull Scope scope) {
        if (loopDepth < 1) {
            logSemanticError(continueStatement.tokenPosition,
                             ErrorType.CONTINUE_OUT_OF_CONTEXT,
                             "continue statement must be enclosed within a loop"
                            );
        }
        return Type.getUnsetType();
    }

    public @NotNull Scope getGlobalScope() {
        return globalScope;
    }
}

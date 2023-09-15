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
import decaf.shared.env.TypingContext;
import decaf.shared.errors.SemanticError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SemanticChecker implements AstVisitor<Type> {
    @NotNull
    public final Scope globalScope = Scope.forGlobals();
    @NotNull
    private final List<SemanticError> errors = new ArrayList<>();
    @NotNull
    private final CompilationContext context;

    // BEGIN: state variables

    // the statically return type of the method we are currently visiting
    private Type inferredReturnType;
    // the number of nested while/for loops we are in
    private int loopDepth = 0;

    // END: state variables

    public SemanticChecker(@NotNull Program root, @NotNull CompilationContext context) {
        this.context = context;
        visit(root, globalScope);
        if (context.debugModeOn()) {
            context.stringifyErrors(errors);
        }
    }

    public Optional<TypingContext> getTypingContext() {
        if (hasErrors()) {
            return Optional.empty();
        }
        return Optional.of(new TypingContext(globalScope));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getPrettyErrorOutput() {
        return context.stringifyErrors(errors);
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
        final var methodId = methodDefinition.getMethodName().getLabel();
        final var block = methodDefinition.getBody();

        var formalArgumentScope = new Scope(globalScope, Scope.For.Arguments, block);
        var localScope = new Scope(formalArgumentScope, Scope.For.Field, block);
        if (globalScope.containsKey(methodId)) {

            errors.add(new SemanticError(methodDefinition.getTokenPosition(),
                                         SemanticError.SemanticErrorType.METHOD_ALREADY_DEFINED,
                                         String.format("Method ``%s`` already defined", methodId)
            ));
            return Type.getUnsetType();
        } else {
            for (var formalArgument : methodDefinition.getFormalArguments()) {
                formalArgument.accept(this, formalArgumentScope);
            }
            // visit the method definition and populate the local symbol table
            globalScope.put(methodId, new MethodDescriptor(methodDefinition, formalArgumentScope, localScope));
            setInferredReturnType(Type.getUnsetType());
            block.scope = localScope;
            block.accept(this, localScope);
            if (getInferredReturnType() != Type.getUnsetType() &&
                methodDefinition.getReturnType() != getInferredReturnType()) {
                errors.add(new SemanticError(methodDefinition.getMethodName().tokenPosition,
                                             SemanticError.SemanticErrorType.SHOULD_RETURN_VOID,
                                             String.format(
                                                     "method `%s` must not return a value of type `%s` in a void method",
                                                     methodId,
                                                     getInferredReturnType()
                                                          )
                ));
            }
            setInferredReturnType(Type.getUnsetType());
            return methodDefinition.getReturnType();
        }
    }

    @Override
    public Type visit(@NotNull ImportDeclaration importDeclaration, @NotNull Scope scope) {
        if (scope.isShadowingParameter(importDeclaration.value.getLabel())) {
            errors.add(new SemanticError(importDeclaration.value.tokenPosition,
                                         SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
                                         "Import identifier " +
                                         importDeclaration.value.getLabel() +
                                         " shadows a parameter"
            ));
        } else if (globalScope.lookup(importDeclaration.value.getLabel()).isPresent()) {
            errors.add(new SemanticError(new TokenPosition(0, 0, 0),
                                         SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
                                         "Import identifier " + importDeclaration.value.getLabel() + " already declared"
            ));
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(For forStatement, Scope scope) {
        Optional<Descriptor> optionalDescriptor = scope.lookup(forStatement.initialization.initLocation.getLabel());
        if (optionalDescriptor.isEmpty()) {
            errors.add(new SemanticError(forStatement.initialization.initLocation.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         forStatement.initialization.initLocation + " must be declared in scope"
            ));
        } else {
            Descriptor initDescriptor = optionalDescriptor.get();
            if (!initDescriptor.typeIs(Type.getIntType())) {
                errors.add(new SemanticError(forStatement.tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             forStatement.initialization.initLocation +
                                             " must type must be " +
                                             Type.getIntType() +
                                             " not " +
                                             initDescriptor.getType()
                ));
            }

            Type type = forStatement.initialization.initExpression.accept(this, scope);
            if (type != Type.getIntType()) {
                errors.add(new SemanticError(forStatement.initialization.initExpression.tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             "init expression must evaluate to an int"
                ));
            }

            Type testType = forStatement.terminatingCondition.accept(this, scope);
            if (testType != Type.getBoolType()) {
                errors.add(new SemanticError(forStatement.terminatingCondition.tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             "for-loop test must evaluate to " + Type.getBoolType() + " not " + testType
                ));
            }
            ++loopDepth;
            forStatement.block.accept(this, scope);
            --loopDepth;
            var updateId = forStatement.update.getLocation().getLabel();
            optionalDescriptor = scope.lookupNonMethod(updateId);
            if (optionalDescriptor.isEmpty()) {
                errors.add(new SemanticError(forStatement.update.getLocation().tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             updateId + " must be declared in scope"
                ));
            } else {
                var updatingDescriptor = optionalDescriptor.get();
                if (!updatingDescriptor.typeIs(Type.getIntType())) {
                    errors.add(new SemanticError(forStatement.initialization.initExpression.tokenPosition,
                                                 SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                 "update location must have type int, not " +
                                                 updatingDescriptor.getType()
                    ));
                }
                var updateExprType = forStatement.update.assignExpr.accept(this, scope);
                if (forStatement.update.assignExpr instanceof CompoundAssignOpExpr) {
                    updateExprType = forStatement.update.assignExpr.expression.getType();
                }
                if (updateExprType != Type.getIntType()) {
                    errors.add(new SemanticError(forStatement.update.assignExpr.tokenPosition,
                                                 SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                 String.format(
                                                         "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                                                         "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                                         updateId,
                                                         updatingDescriptor.getType(),
                                                         forStatement.update.assignExpr.getSourceCode(),
                                                         updateExprType
                                                              )
                    ));
                }
            }
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(@NotNull While whileStatement, @NotNull Scope scope) {
        Type type = whileStatement.test.accept(this, scope);
        if (type != Type.getBoolType()) {
            errors.add(new SemanticError(whileStatement.test.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "while statement test must evaluate to a bool, not " + type
            ));
        }
        ++loopDepth;
        whileStatement.body.accept(this, scope);
        --loopDepth;
        return Type.getUnsetType();
    }


    private void checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
        for (var methodDefinition : methodDefinitionList) {
            if (methodDefinition.getMethodName().getLabel().equals("main")) {
                if (methodDefinition.getReturnType() == Type.getVoidType()) {
                    if (!(methodDefinition.getFormalArguments().isEmpty())) {
                        errors.add(new SemanticError(methodDefinition.getTokenPosition(),
                                                     SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
                                                     "main method must have no parameters, yours has: " +
                                                     Utils.prettyPrintMethodFormalArguments(methodDefinition.getFormalArguments())
                        ));
                    }
                } else {
                    errors.add(new SemanticError(methodDefinition.getTokenPosition(),
                                                 SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
                                                 "main method return type must be void, not `" +
                                                 methodDefinition.getReturnType() +
                                                 "`"
                    ));
                }
                return;
            }
        }
        errors.add(new SemanticError(new TokenPosition(0, 0, 0),
                                     SemanticError.SemanticErrorType.MISSING_MAIN_METHOD,
                                     "main method not found"
        ));
    }

    @Override
    public Type visit(Program program, Scope scope) {
        checkMainMethodExists(program.getMethodDefinitions());
        for (var importDeclaration : program.getImportDeclaration()) {
            globalScope.put(importDeclaration.value.getLabel(), Descriptor.forImport());
        }
        for (var fieldDeclaration : program.getFieldDeclaration()) {
            fieldDeclaration.accept(this, globalScope);
        }
        for (var methodDefinition : program.getMethodDefinitions()) {
            methodDefinition.accept(this, scope);
        }
        return Type.getUnsetType();
    }

    @Override
    public Type visit(UnaryOpExpression unaryOpExpression, Scope scope) {
        Type operandType = unaryOpExpression.operand.accept(this, scope);
        if (operandType != Type.getBoolType() && operandType != Type.getIntType()) {
            errors.add(new SemanticError(unaryOpExpression.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "expected either bool or int, not `" + operandType + "`"
            ));
        }
        if (operandType != Type.getBoolType() && unaryOpExpression.getUnaryOperator().label.equals("!")) {
            errors.add(new SemanticError(unaryOpExpression.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "Can only use a not operator on booleans"
            ));
        }
        if (operandType != Type.getIntType() && unaryOpExpression.getUnaryOperator().label.equals("-")) {
            errors.add(new SemanticError(unaryOpExpression.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "Can only use an unary minus operator on Integers"
            ));
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
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("lhs must have type int not `%s`", leftType)
                        ));
                    } else {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("rhs must have type int not `%s`", rightType)
                        ));
                    }
                } else if (binaryOpExpression.op instanceof EqualityOperator) {
                    if (leftType.equals(rightType) &&
                        (leftType == Type.getIntType() || leftType == Type.getBoolType())) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format(
                                                             "operands of %s must have same type, either both bool or both int, not `%s` and `%s`",
                                                             binaryOpExpression.op.getSourceCode(),
                                                             leftType,
                                                             rightType
                                                                  )
                        ));
                    }
                } else if (binaryOpExpression.op instanceof RelationalOperator) {
                    if (leftType.equals(Type.getIntType()) && rightType.equals(Type.getIntType())) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else if (leftType != Type.getIntType()) {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("lhs must have type int not `%s`", leftType)
                        ));
                    } else {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("rhs must have type int not `%s`", rightType)
                        ));
                    }
                } else if (binaryOpExpression.op instanceof ConditionalOperator) {
                    if (leftType == Type.getBoolType() && rightType == Type.getBoolType()) {
                        binaryOpExpression.setType(Type.getBoolType());
                    } else if (leftType != Type.getBoolType()) {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("lhs must have type bool not `%s`", leftType)
                        ));
                    } else {
                        errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                                     SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                     String.format("rhs must have type bool not `%s`", rightType)
                        ));
                    }
                }
            } else {
                errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             "RHS must be a primitive type, not `" + rightType + "`"
                ));
            }
        } else {
            errors.add(new SemanticError(binaryOpExpression.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "LHS must be a primitive type, not `" + leftType + "`"
            ));
        }
        return binaryOpExpression.getType();
    }

    @Override
    public Type visit(@NotNull Block block, @NotNull Scope parentScope) {
        var localScope = new Scope(parentScope, Scope.For.Field, block);
        block.scope = localScope;
        parentScope.children.add(localScope);

        for (var fieldDeclaration : block.getFieldDeclarations()) {
            fieldDeclaration.accept(this, block.scope);
        }
        for (Statement statement : block.getStatements()) {
            statement.accept(this, block.scope);
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
            errors.add(new SemanticError(locationArray.tokenPosition,
                                         SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
                                         "array index must evaluate to int"
            ));
        }
        return scope.lookup(locationArray.getLabel()).map(descriptor -> {
            if (descriptor.isForArray()) {
                var type = descriptor.getType();
                assert type.isDerivedArrayType();
                return ((ArrayType) type).getContainedType();
            } else {
                errors.add(new SemanticError(locationArray.tokenPosition,
                                             SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                             String.format("`%s` was declared as %s but it being indexed like an array",
                                                           locationArray.getLabel(),
                                                           descriptor.getType()
                                                          )
                ));
                return descriptor.getType();
            }
        }).orElseGet(() -> {
            errors.add(new SemanticError(locationArray.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         locationArray.getLabel() + " must be declared in scope"
            ));
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
        var typeTest = ifStatement.test.accept(this, scope);
        if (typeTest != Type.getBoolType()) {
            errors.add(new SemanticError(ifStatement.test.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "if statement test must evaluate to a bool, not " + typeTest
            ));
        }
        ifStatement.ifBlock.accept(this, scope);
        if (ifStatement.elseBlock != null) {
            ifStatement.elseBlock.accept(this, scope);
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
            errors.add(new SemanticError(array.getSize().tokenPosition,
                                         SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
                                         "The int literal in an array declaration must be greater than 0"
            ));
        }
        return Type.getUnsetType();
    }

    public Type visit(@NotNull FormalArgument formalArgument, @NotNull Scope scope) {
        var formalArgumentId = formalArgument.getName();
        if (scope.isShadowingParameter(formalArgumentId)) {
            errors.add(new SemanticError(formalArgument.tokenPosition,
                                         SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
                                         String.format("Formal argument `%s` shadows a parameter", formalArgumentId)
            ));
        } else {
            scope.put(formalArgumentId, Descriptor.forFormalArgument(formalArgument.getType()));
        }
        return formalArgument.getType();
    }

    // The number and types of arguments in a method call
    private void checkNumberOfArgumentsAndTypesMatch(MethodDefinition methodDefinition, MethodCall methodCall) {
        if (methodCall.actualArgumentList.size() != methodDefinition.getFormalArguments().size()) {
            errors.add(new SemanticError(methodCall.tokenPosition,
                                         SemanticError.SemanticErrorType.MISMATCHING_NUMBER_OR_ARGUMENTS,
                                         String.format("method %s expects %d arguments but %d were passed in",
                                                       methodCall.methodId.getLabel(),
                                                       methodDefinition.getFormalArguments().size(),
                                                       methodCall.actualArgumentList.size()
                                                      )
            ));
            return;
        }
        for (int i = 0; i < methodCall.actualArgumentList.size(); i++) {
            final FormalArgument formalArgument = methodDefinition.getFormalArguments().get(i);
            final ActualArgument actualArgument = methodCall.actualArgumentList.get(i);
            if (actualArgument.getType() != formalArgument.getType()) {
                errors.add(new SemanticError(methodCall.tokenPosition,
                                             SemanticError.SemanticErrorType.INCORRECT_ARG_TYPE,
                                             String.format(
                                                     "method %s expects argument %d to be of type %s but %s was passed in",
                                                     methodCall.methodId.getLabel(),
                                                     i + 1,
                                                     formalArgument.getType(),
                                                     actualArgument.getType()
                                                          )
                ));
            }
        }
    }

    private void visitMethodCallParameters(List<ActualArgument> actualArgumentList, Scope scope) {
        for (ActualArgument actualArgument : actualArgumentList) {
            actualArgument.accept(this, scope);
        }
    }

    @Override
    public Type visit(@NotNull MethodCall methodCall, @NotNull Scope scope) {
        var methodName = methodCall.methodId.getLabel();
        return globalScope.lookup(methodName).map(descriptor -> {
            if (scope.containsKey(methodName)) {
                errors.add(new SemanticError(methodCall.tokenPosition,
                                             SemanticError.SemanticErrorType.METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER,
                                             methodName + " refers to locally defined identifier"
                ));
            }
            visitMethodCallParameters(methodCall.actualArgumentList, scope);
            if (descriptor instanceof MethodDescriptor methodDescriptor) {
                checkNumberOfArgumentsAndTypesMatch(methodDescriptor.methodDefinition, methodCall);
            }
            methodCall.setType(descriptor.getType());
            return descriptor.getType();


        }).orElseGet(() -> {
            errors.add(new SemanticError(methodCall.tokenPosition,
                                         SemanticError.SemanticErrorType.METHOD_DEFINITION_NOT_FOUND,
                                         "method " + methodName + " not found in scope"
            ));
            return Type.getUnsetType();
        });
    }

    @Override
    public Type visit(MethodCallStatement methodCallStatement, Scope scope) {
        final var methodId = methodCallStatement.methodCall.methodId.getLabel();
        if (globalScope.lookupMethod(methodId).isEmpty() && globalScope.lookupImport(methodId).isEmpty()) {
            errors.add(new SemanticError(methodCallStatement.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         String.format(
                                                 "identifier `%s` in a method statement must be a declared method or import.",
                                                 methodId
                                                      )
            ));
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
                        errors.add(new SemanticError(locationAssignExpr.tokenPosition,
                                                     SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
                                                     "array index must evaluate to an int"
                        ));
                    }
                }
            }
            final var locationType = location.accept(this, scope);
            final Type expressionType = locationAssignExpr.assignExpr.accept(this, scope);
            if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
                if (assignOpExpr.assignOp.label.equals(Scanner.ASSIGN)) {
                    if (locationType != expressionType) {
                        errors.add(new SemanticError(locationAssignExpr.tokenPosition,
                                                     SemanticError.SemanticErrorType.TYPE_MISMATCH,
                                                     String.format(
                                                             "The location and the expression in an assignment must have the same type, " +
                                                             "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                                             location.getLabel(),
                                                             locationType,
                                                             locationAssignExpr.assignExpr.expression.getSourceCode(),
                                                             expressionType
                                                                  )
                        ));
                    }
                }
            }
            if (assignOperatorEquals(locationAssignExpr, Scanner.ADD_ASSIGN) ||
                assignOperatorEquals(locationAssignExpr, Scanner.MINUS_ASSIGN) ||
                assignOperatorEquals(locationAssignExpr, Scanner.MULTIPLY_ASSIGN) ||
                locationAssignExpr.assignExpr instanceof Decrement ||
                locationAssignExpr.assignExpr instanceof Increment) {
                // both must be of type int
                if (!((descriptor.getType() == Type.getIntType()) && expressionType == Type.getIntType())) {
                    errors.add(new SemanticError(locationAssignExpr.tokenPosition,
                                                 SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                                 String.format(
                                                         "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                                                         "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                                                         locationAssignExpr.location.getLabel(),
                                                         descriptor.getType(),
                                                         locationAssignExpr.assignExpr.getSourceCode(),
                                                         expressionType
                                                              )
                    ));
                }
            }
            return Type.getUnsetType();
        }).orElseGet(() -> {
            errors.add(new SemanticError(locationAssignExpr.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         String.format("id `%s` not in scope", location.getLabel())
            ));
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
        var descriptorOptional = scope.lookup(locationVariable.getLabel());
        if (descriptorOptional.isPresent()) {
            locationVariable.setType(descriptorOptional.get().getType());
            return locationVariable.getType();
        } else {
            errors.add(new SemanticError(locationVariable.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         "No identifier can be used before it is declared: " +
                                         locationVariable.getLabel() +
                                         " not in scope"
            ));
            return Type.getUnsetType();
        }
    }

    @Override
    public Type visit(Len len, Scope scope) {
        var arrayName = len.rValue.getLabel();
        var descriptor = scope.lookup(arrayName);
        if (descriptor.isEmpty()) {
            errors.add(new SemanticError(len.rValue.tokenPosition,
                                         SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
                                         arrayName + " not in scope"
            ));
        } else if (!(descriptor.get().isForArray())) {
            errors.add(new SemanticError(len.rValue.tokenPosition,
                                         SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                                         "the argument of the len operator must be an array"
            ));
        }
        return Type.getIntType();
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
        return null;
    }

    private void checkIntBounds(@NotNull IntLiteral intLiteral) {
        try {
            intLiteral.convertToLong();
        } catch (Exception e) {
            errors.add(new SemanticError(intLiteral.tokenPosition,
                                         SemanticError.SemanticErrorType.INT_LITERAL_TOO_BIG,
                                         "Encountered int literal that's out of bounds; -9223372036854775808 <= x <= 9223372036854775807"
            ));
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
                errors.add(new SemanticError(fieldDeclaration.tokenPosition,
                                             SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
                                             "Field " + rValue.getLabel() + " shadows a parameter"
                ));
            } else if (scope.containsKey(rValue.getLabel())) {
                // field already declared in scope
                errors.add(new SemanticError(fieldDeclaration.tokenPosition,
                                             SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
                                             "Field " + rValue.getLabel() + " already declared"
                ));
            } else {
                scope.put(rValue.getLabel(), Descriptor.forValue(type));
            }
        }

        for (Array array : fieldDeclaration.arrays) {
            var arrayIdLabel = array.getLabel();
            if (scope.isShadowingParameter(array.getLabel())) {
                errors.add(new SemanticError(fieldDeclaration.tokenPosition,
                                             SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
                                             "Field " + arrayIdLabel + " shadows a parameter"
                ));
            } else if (scope.lookup(array.getLabel()).isPresent()) {
                errors.add(new SemanticError(fieldDeclaration.tokenPosition,
                                             SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
                                             "Field " + arrayIdLabel + " already declared"
                ));
            } else {
                if (Integer.parseInt(array.getSize().literal) <= 0) {
                    errors.add(new SemanticError(array.getSize().tokenPosition,
                                                 SemanticError.SemanticErrorType.INVALID_ARRAY_SIZE,
                                                 "array declaration size " +
                                                 array.getLabel() +
                                                 "[" +
                                                 array.getSize().literal +
                                                 "]" +
                                                 " must be greater than 0"
                    ));
                } else {
                    scope.put(arrayIdLabel, Descriptor.forArray(ArrayType.get(type, array.getSize().convertToLong())));
                }
            }
        }
        return Type.getUnsetType();
    }

    public @NotNull Type visit(@NotNull Break breakStatement, @NotNull Scope scope) {
        if (loopDepth < 1) {
            errors.add(new SemanticError(breakStatement.tokenPosition,
                                         SemanticError.SemanticErrorType.BREAK_OUT_OF_CONTEXT,
                                         "break statement must be enclosed within a loop"
            ));
        }
        return Type.getUnsetType();
    }

    public @NotNull Type visit(@NotNull Continue continueStatement, @NotNull Scope scope) {
        if (loopDepth < 1) {
            errors.add(new SemanticError(continueStatement.tokenPosition,
                                         SemanticError.SemanticErrorType.CONTINUE_OUT_OF_CONTEXT,
                                         "continue statement must be enclosed within a loop"

            ));
        }
        return Type.getUnsetType();
    }
}

package decaf.analysis.semantic;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.ActualArgument;
import decaf.analysis.syntax.ast.ArithmeticOperator;
import decaf.analysis.syntax.ast.Array;
import decaf.analysis.syntax.ast.AssignOpExpr;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.Break;
import decaf.analysis.syntax.ast.CharLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.ConditionalOperator;
import decaf.analysis.syntax.ast.Continue;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.EqualityOperator;
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
import decaf.analysis.syntax.ast.VoidExpression;
import decaf.analysis.syntax.ast.While;
import decaf.shared.AstVisitor;
import decaf.shared.CompilationContext;
import decaf.shared.Utils;
import decaf.shared.descriptors.ArrayDescriptor;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.env.TypingContext;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.descriptors.ValueDescriptor;
import decaf.shared.env.Scope;
import decaf.shared.errors.SemanticError;

public class SemanticChecker implements AstVisitor<Type> {
  @NotNull
  public final TreeSet<String> imports = new TreeSet<>();
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

  public SemanticChecker(
      @NotNull Program root, @NotNull CompilationContext context
  ) {
    this.context = context;
    visit(root, globalScope);
    if (context.debugModeOn()) {
      context.stringifyErrors(errors);
    }
  }

  public Optional<TypingContext> getTypingContext() {
    if (hasErrors()) return Optional.empty();
    return Optional.of(
        new TypingContext(
            globalScope,
            imports
        ));
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public String getPrettyErrorOutput() {
    return context.stringifyErrors(errors);
  }

  @Override
  public Type visit(
      IntLiteral intLiteral,
      Scope scope
  ) {
    checkIntBounds(intLiteral);
    return Type.Int;
  }

  @Override
  public Type visit(
      BooleanLiteral booleanLiteral,
      Scope scope
  ) {
    return Type.Bool;
  }

  @Override
  public Type visit(
      @NotNull MethodDefinition methodDefinition,
      @NotNull Scope scope
  ) {
    final var methodId = methodDefinition.getMethodName()
                                         .getLabel();
    final var block = methodDefinition.getBody();

    var formalArgumentScope = new Scope(
        globalScope,
        Scope.For.Arguments,
        block
    );
    var localScope =
        new Scope(
            formalArgumentScope,
            Scope.For.Field,
            block
        );
    if (globalScope.containsKey(methodId) ||
        imports.contains(methodId)) {

      errors.add(new SemanticError(
          methodDefinition.getTokenPosition(),
          SemanticError.SemanticErrorType.METHOD_ALREADY_DEFINED,
          String.format("Method ``%s`` already defined", methodId)
      ));
      return Type.Undefined;
    } else {
      for (var formalArgument : methodDefinition.getFormalArguments()) {
        formalArgument.accept(
            this,
            formalArgumentScope
        );
      }
      // visit the method definition and populate the local symbol table
      globalScope.put(
          methodId,
          new MethodDescriptor(
              methodDefinition,
              formalArgumentScope,
              localScope
          )
      );
      setInferredReturnType(Type.Undefined);
      block.scope = localScope;
      block.accept(this, localScope);
      if (getInferredReturnType() != Type.Undefined && methodDefinition.getReturnType() != getInferredReturnType()) {
        errors.add(new SemanticError(
            methodDefinition.getMethodName().tokenPosition,
            SemanticError.SemanticErrorType.SHOULD_RETURN_VOID,
            String.format(
                "method `%s` must not return a value of type `%s` in a void method",
                methodId,
                getInferredReturnType()
            )
        ));
      }
      setInferredReturnType(Type.Undefined);
      return methodDefinition.getReturnType();
    }
  }

  @Override
  public Type visit(
      @NotNull ImportDeclaration importDeclaration,
      @NotNull Scope scope
  ) {
    if (scope.isShadowingParameter(importDeclaration.value.getLabel())) {
      errors.add(new SemanticError(
          importDeclaration.value.tokenPosition,
          SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
          "Import identifier " + importDeclaration.value.getLabel() +
              " shadows a parameter"
      ));
    } else if (imports.contains(importDeclaration.value.getLabel())) {
      errors.add(new SemanticError(
          new TokenPosition(
              0,
              0,
              0
          ),
          SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
          "Import identifier " + importDeclaration.value.getLabel() +
              " already declared"
      ));
    } else {
      imports.add(importDeclaration.value.getLabel());
    }
    return Type.Undefined;
  }

  @Override
  public Type visit(
      For forStatement,
      Scope scope
  ) {
    Optional<Descriptor> optionalDescriptor = scope.lookup(forStatement.initialization.initLocation.getLabel());
    if (optionalDescriptor.isEmpty()) {
      errors.add(new SemanticError(
          forStatement.initialization.initLocation.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          forStatement.initialization.initLocation + " must be declared in scope"
      ));
    } else {
      Descriptor initDescriptor = optionalDescriptor.get();
      if (!initDescriptor.typeIs(Type.Int)) {
        errors.add(new SemanticError(
            forStatement.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            forStatement.initialization.initLocation + " must type must be " + Type.Int + " not " + initDescriptor.getType()
        ));
      }

      Type type = forStatement.initialization.initExpression.accept(
          this,
          scope
      );
      if (type != Type.Int) {
        errors.add(new SemanticError(
            forStatement.initialization.initExpression.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            "init expression must evaluate to an int"
        ));
      }

      Type testType = forStatement.terminatingCondition.accept(
          this,
          scope
      );
      if (testType != Type.Bool) {
        errors.add(new SemanticError(
            forStatement.terminatingCondition.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            "for-loop test must evaluate to " + Type.Bool + " not " + testType
        ));
      }
      ++loopDepth;
      forStatement.block.accept(
          this,
          scope
      );
      --loopDepth;
      var updateId = forStatement.update.getLocation().getLabel();
      optionalDescriptor = scope.lookupNonMethod(updateId);
      if (optionalDescriptor.isEmpty()) {
        errors.add(new SemanticError(
            forStatement.update.getLocation().tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            updateId + " must be declared in scope"
        ));
      } else {
        var updatingDescriptor = optionalDescriptor.get();
        if (!updatingDescriptor.typeIs(Type.Int)) {
          errors.add(new SemanticError(
              forStatement.initialization.initExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "update location must have type int, not " + updatingDescriptor.getType()
          ));
        }
        var updateExprType = forStatement.update.assignExpr.accept(
            this,
            scope
        );
        if (forStatement.update.assignExpr instanceof CompoundAssignOpExpr)
          updateExprType = forStatement.update.assignExpr.expression.getType();
        if (updateExprType != Type.Int) {
          errors.add(new SemanticError(
              forStatement.update.assignExpr.tokenPosition,
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
    return Type.Undefined;
  }

  @Override
  public Type visit(
      @NotNull While whileStatement,
      @NotNull Scope scope
  ) {
    Type type = whileStatement.test.accept(
        this,
        scope
    );
    if (type != Type.Bool) {
      errors.add(new SemanticError(
          whileStatement.test.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "while statement test must evaluate to a bool, not " + type
      ));
    }
    ++loopDepth;
    whileStatement.body.accept(
        this,
        scope
    );
    --loopDepth;
    return Type.Undefined;
  }


  private void
  checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
    for (var methodDefinition : methodDefinitionList) {
      if (methodDefinition.getMethodName()
                          .getLabel()
                          .equals("main")) {
        if (methodDefinition.getReturnType() == Type.Void) {
          if (!(methodDefinition.getFormalArguments()
                                .isEmpty())) {
            errors.add(new SemanticError(
                methodDefinition.getTokenPosition(),
                SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
                "main method must have no parameters, yours has: " +
                    Utils.prettyPrintMethodFormalArguments(methodDefinition.getFormalArguments())
            ));
          }
        } else {
          errors.add(new SemanticError(
              methodDefinition.getTokenPosition(),
              SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
              "main method return type must be void, not `" +
                  methodDefinition.getReturnType() + "`"
          ));
        }
        return;
      }
    }
    errors.add(
        new SemanticError(
            new TokenPosition(
                0,
                0,
                0
            ),
            SemanticError.SemanticErrorType.MISSING_MAIN_METHOD,
            "main method not found"
        ));
  }

  @Override
  public Type visit(
      Program program,
      Scope scope
  ) {
    checkMainMethodExists(program.getMethodDefinitions());
    for (var importDeclaration : program.getImportDeclaration())
      imports.add(importDeclaration.value.getLabel());
    for (var fieldDeclaration : program.getFieldDeclaration())
      fieldDeclaration.accept(
          this,
          globalScope
      );
    for (var methodDefinition : program.getMethodDefinitions())
      methodDefinition.accept(
          this,
          scope
      );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      UnaryOpExpression unaryOpExpression,
      Scope scope
  ) {
    Type operandType = unaryOpExpression.operand.accept(
        this,
        scope
    );
    if (operandType != Type.Bool && operandType != Type.Int) {
      errors.add(new SemanticError(
          unaryOpExpression.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "expected either bool or int, not `" + operandType + "`"
      ));
    }
    if (operandType != Type.Bool && unaryOpExpression.getUnaryOperator().label.equals("!")) {
      errors.add(new SemanticError(
          unaryOpExpression.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "Can only use a not operator on booleans"
      ));
    }
    if (operandType != Type.Int && unaryOpExpression.getUnaryOperator().label.equals("-")) {
      errors.add(new SemanticError(
          unaryOpExpression.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "Can only use an unary minus operator on Integers"
      ));
    }
    unaryOpExpression.setType(operandType);
    return operandType;
  }

  @Override
  public Type visit(
      BinaryOpExpression binaryOpExpression,
      Scope scope
  ) {
    Type leftType = binaryOpExpression.lhs.accept(
        this,
        scope
    );
    Type rightType = binaryOpExpression.rhs.accept(
        this,
        scope
    );
    Type binaryOpExpressionType = Type.Undefined;
    if (leftType != Type.Undefined && rightType != Type.Undefined) {
      if (binaryOpExpression.op instanceof ConditionalOperator) {
        if (leftType == Type.Bool && rightType == Type.Bool) {
          binaryOpExpressionType = Type.Bool;
        } else if (leftType != Type.Bool) {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "lhs must have type bool not `" + leftType + " `"
          ));
        } else {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "rhs must have type bool not `" + rightType + " `"
          ));
        }
      } else if (binaryOpExpression.op instanceof ArithmeticOperator) {
        if (leftType == Type.Int && rightType == Type.Int) {
          binaryOpExpressionType = Type.Int;
        } else if (leftType != Type.Int) {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "lhs must have type int not `" + leftType + " `"
          ));
        } else {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "rhs must have type int not `" + rightType + " `"
          ));
        }
      } else if (binaryOpExpression.op instanceof EqualityOperator) {
        if (leftType.equals(rightType) && (leftType == Type.Int || leftType == Type.Bool))
          binaryOpExpressionType = Type.Bool;
        else errors.add(new SemanticError(
            binaryOpExpression.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            "operands of " + binaryOpExpression.op.getSourceCode() +
                " must have same type, either both bool or both int, not `" +
                leftType + "` and `" + rightType
        ));
      } else if (binaryOpExpression.op instanceof RelationalOperator) {
        if (leftType.equals(Type.Int) && rightType.equals(Type.Int)) {
          binaryOpExpressionType = Type.Bool;
        } else if (leftType != Type.Int) {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "lhs must have type int not `" + leftType + " `"
          ));
        } else {
          errors.add(new SemanticError(
              binaryOpExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "rhs must have type int not `" + rightType + " `"
          ));
        }
      }
    }
    binaryOpExpression.setType(binaryOpExpressionType);
    return binaryOpExpressionType;
  }

  @Override
  public Type visit(
      @NotNull Block block,
      @NotNull Scope parentScope
  ) {
    var localScope =
        new Scope(
            parentScope,
            Scope.For.Field,
            block
        );
    block.scope = localScope;
    parentScope.children.add(localScope);

    for (var fieldDeclaration : block.getFieldDeclarations())
      fieldDeclaration.accept(
          this,
          block.scope
      );
    for (Statement statement : block.getStatements())
      statement.accept(
          this,
          block.scope
      );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      ParenthesizedExpression parenthesizedExpression,
      Scope scope
  ) {
    return parenthesizedExpression.expression.accept(
        this,
        scope
    );
  }

  @Override
  public Type visit(
      LocationArray locationArray,
      Scope scope
  ) {
    if (locationArray.expression.accept(
        this,
        scope
    ) != Type.Int) {
      errors.add(new SemanticError(
          locationArray.tokenPosition,
          SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
          "array index must evaluate to int"
      ));
    }
    Type type = Type.Undefined;
    Optional<Descriptor> descriptor = scope.lookup(locationArray.getLabel());
    if (descriptor.isPresent()) {
      if ((descriptor.get() instanceof ArrayDescriptor)) {
        switch (descriptor.get().getType()) {
          case Bool, BoolArray -> type = Type.Bool;
          case Int, IntArray -> type = Type.Int;
          default -> errors.add(new SemanticError(
              locationArray.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              locationArray.getLabel() + " must be an array"
          ));
        }
      }
    }
    locationArray.setType(type);
    return type;
  }

  @Override
  public Type visit(
      ExpressionParameter expressionParameter,
      Scope scope
  ) {
    var type = expressionParameter.expression.accept(
        this,
        scope
    );
    expressionParameter.type = type;
    return type;
  }

  @Override
  public Type visit(
      @NotNull If ifStatement,
      @NotNull Scope scope
  ) {
    var typeTest = ifStatement.test.accept(
        this,
        scope
    );
    if (typeTest != Type.Bool) {
      errors.add(new SemanticError(
          ifStatement.test.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "if statement test must evaluate to a bool, not " + typeTest
      ));
    }
    ifStatement.ifBlock.accept(
        this,
        scope
    );
    if (ifStatement.elseBlock != null) {
      ifStatement.elseBlock.accept(
          this,
          scope
      );
    }
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Return returnStatement,
      Scope scope
  ) {
    Type type = returnStatement.retExpression.accept(
        this,
        scope
    );
    setInferredReturnType(type);
    return Type.Void;
  }

  @Override
  public Type visit(
      Array array,
      Scope scope
  ) {
    if (array.getSize()
             .convertToLong() <= 0) {
      errors.add(new SemanticError(
          array.getSize().tokenPosition,
          SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
          "The int_literal in an array declaration must be greater than 0"
      ));
    }
    return Type.Undefined;
  }

  public Type visit(
      @NotNull FormalArgument formalArgument,
      @NotNull Scope scope
  ) {
    var formalArgumentId = formalArgument.getName();
    if (scope.isShadowingParameter(formalArgumentId))
      errors.add(
          new SemanticError(
              formalArgument.tokenPosition,
              SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
              String.format(
                  "Formal argument `%s` shadows a parameter",
                  formalArgumentId
              )
          ));
    else
      scope.put(
          formalArgumentId,
          new ValueDescriptor(
              formalArgument.getType(),
              true
          )
      );
    return formalArgument.getType();
  }

  // The number and types of arguments in a method call
  private void checkNumberOfArgumentsAndTypesMatch(
      MethodDefinition methodDefinition,
      MethodCall methodCall
  ) {
    if (methodCall.actualArgumentList.size() != methodDefinition.getFormalArguments()
                                                                .size()) {
      errors.add(new SemanticError(
          methodCall.tokenPosition,
          SemanticError.SemanticErrorType.MISMATCHING_NUMBER_OR_ARGUMENTS,
          String.format(
              "method %s expects %d arguments but %d were passed in",
              methodCall.RValueId.getLabel(),
              methodDefinition.getFormalArguments()
                              .size(),
              methodCall.actualArgumentList.size()
          )
      ));
      return;
    }
    for (int i = 0; i < methodCall.actualArgumentList.size(); i++) {
      final FormalArgument formalArgument = methodDefinition.getFormalArguments()
                                                            .get(i);
      final ActualArgument actualArgument = methodCall.actualArgumentList.get(i);
      if (actualArgument.type != formalArgument.getType()) {
        errors.add(new SemanticError(
            methodCall.tokenPosition,
            SemanticError.SemanticErrorType.INCORRECT_ARG_TYPE,
            "method param " + formalArgument.getName() + " is defined with type " +
                formalArgument.getType() + " but " + actualArgument.type + " is passed in"
        ));
      }
    }
  }

  private void visitMethodCallParameters(
      List<ActualArgument> actualArgumentList,
      Scope scope
  ) {
    for (ActualArgument actualArgument : actualArgumentList) {
      actualArgument.accept(
          this,
          scope
      );
    }
  }

  @Override
  public Type visit(
      @NotNull MethodCall methodCall,
      @NotNull Scope scope
  ) {

    final Optional<Descriptor> optionalMethodDescriptor = globalScope.lookup(methodCall.RValueId.getLabel());
    final Descriptor descriptor;
    if (scope.containsKey(methodCall.RValueId.getLabel())) {
      errors.add(new SemanticError(
          methodCall.tokenPosition,
          SemanticError.SemanticErrorType.METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER,
          methodCall.RValueId.getLabel() + " refers to locally defined identifier"
      ));
      return Type.Undefined;
    }
    if (imports.contains(methodCall.RValueId.getLabel())) {
      // All external functions are treated as if they return int
      methodCall.setType(Type.Int);
      visitMethodCallParameters(
          methodCall.actualArgumentList,
          scope
      );
      methodCall.isImported = true;
      return Type.Int;
    }
    if (optionalMethodDescriptor.isPresent()) {
      descriptor = optionalMethodDescriptor.get();
    } else {
      errors.add(new SemanticError(
          methodCall.tokenPosition,
          SemanticError.SemanticErrorType.METHOD_DEFINITION_NOT_FOUND,
          "method " + methodCall.RValueId.getLabel() + " not found"
      ));
      return Type.Undefined;
    }
    visitMethodCallParameters(
        methodCall.actualArgumentList,
        scope
    );
    checkNumberOfArgumentsAndTypesMatch(
        ((MethodDescriptor) descriptor).methodDefinition,
        methodCall
    );

    methodCall.setType(descriptor.getType());
    return descriptor.getType();
  }

  @Override
  public Type visit(
      MethodCallStatement methodCallStatement,
      Scope scope
  ) {
    final var methodId = methodCallStatement.methodCall.RValueId.getLabel();
    if (!imports.contains(methodId) && globalScope.lookupMethod(methodId).isEmpty()) {
      errors.add(new SemanticError(
          methodCallStatement.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          String.format("identifier `%s` in a method statement must be a declared method or import.", methodId)
      ));
    } else {
      methodCallStatement.methodCall.accept(
          this,
          scope
      );
    }
    return Type.Undefined;
  }

  @Override
  public Type visit(
      LocationAssignExpr locationAssignExpr,
      Scope scope
  ) {
    var optionalDescriptor = scope.lookupNonMethod(locationAssignExpr.location.getLabel());
    if (optionalDescriptor.isEmpty() || (locationAssignExpr.location instanceof LocationVariable &&
        optionalDescriptor.get() instanceof ArrayDescriptor)) {
      if (optionalDescriptor.isEmpty()) {
        errors.add(new SemanticError(
            locationAssignExpr.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
            "id `" + locationAssignExpr.location.getLabel() +
                "` being assigned to must be a declared local/global irAssignableValue or formal parameter."
        ));
      } else {
        errors.add(new SemanticError(
            locationAssignExpr.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            String.format(
                "`%s` is an array and cannot be modified directly like a variable",
                locationAssignExpr.location.getLabel()
            )
        ));
      }
    } else {
      if (locationAssignExpr.location instanceof final LocationArray locationArray) {
        if (optionalDescriptor.get().typeIsNotAnyOf(Type.IntArray, Type.BoolArray)) {
          errors.add(new SemanticError(
              locationAssignExpr.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              String.format(
                  "The location `%s` is of type `%s` and cannot be indexed like an array",
                  locationArray.getLabel(),
                  optionalDescriptor.get().getType()
              )
          ));
        }
        locationArray.expression.setType(locationArray.expression.accept(
            this,
            scope
        ));
        if (locationArray.expression.getType() != Type.Int) {
          errors.add(new SemanticError(
              locationAssignExpr.tokenPosition,
              SemanticError.SemanticErrorType.INVALID_ARRAY_INDEX,
              "array index must evaluate to an int"
          ));
        }
      }
      final Type expressionType = locationAssignExpr.assignExpr.accept(
          this,
          scope
      );
      final Descriptor locationDescriptor = optionalDescriptor.get();

      if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
        if (assignOpExpr.assignOp.label.equals(Scanner.ASSIGN)) {
          if ((locationDescriptor.getType() == Type.Int || locationDescriptor.getType() == Type.IntArray) &&
              expressionType != Type.Int) {
            errors.add(new SemanticError(
                locationAssignExpr.tokenPosition,
                SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                "lhs is type " + locationDescriptor.getType() + " rhs must be type Int, not " + expressionType
            ));
          }
          if ((locationDescriptor.typeIsAnyOf(Type.Bool, Type.BoolArray)) &&
              expressionType != Type.Bool) {
            errors.add(new SemanticError(
                locationAssignExpr.tokenPosition,
                SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                "lhs is type " + locationDescriptor.getType() + " rhs must be type Bool, not " + expressionType
            ));
          }
        }
      }
      if (assignOperatorEquals(
          locationAssignExpr,
          Scanner.ADD_ASSIGN
      ) || assignOperatorEquals(
          locationAssignExpr,
          Scanner.MINUS_ASSIGN
      ) || assignOperatorEquals(
          locationAssignExpr,
          Scanner.MULTIPLY_ASSIGN
      ) || locationAssignExpr.assignExpr instanceof Decrement || locationAssignExpr.assignExpr instanceof Increment) {
        // both must be of type int
        if (!((locationDescriptor.getType() == Type.Int || locationDescriptor.getType() == Type.IntArray) &&
            expressionType == Type.Int)) {
          errors.add(new SemanticError(
              locationAssignExpr.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              String.format(
                  "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                      "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                  locationAssignExpr.location.getLabel(),
                  locationDescriptor.getType(),
                  locationAssignExpr.assignExpr.getSourceCode(),
                  expressionType
              )
          ));
        }
      }
    }
    return Type.Undefined;
  }

  private boolean assignOperatorEquals(
      LocationAssignExpr locationAssignExpr,
      String opStr
  ) {
    return (locationAssignExpr.assignExpr instanceof AssignOpExpr &&
        ((AssignOpExpr) locationAssignExpr.assignExpr).assignOp.label.equals(opStr)) ||
        (locationAssignExpr.assignExpr instanceof CompoundAssignOpExpr &&
            ((CompoundAssignOpExpr) locationAssignExpr.assignExpr).compoundAssignOp.label.equals(opStr));
  }

  @Override
  public Type visit(
      AssignOpExpr assignOpExpr,
      Scope scope
  ) {
    return assignOpExpr.expression.accept(
        this,
        scope
    );
  }

  @Override
  public Type visit(
      RValue RValue,
      Scope scope
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      @NotNull LocationVariable locationVariable,
      @NotNull Scope scope
  ) {
    var descriptorOptional = scope.lookup(locationVariable.getLabel());
    if (descriptorOptional.isPresent()) {
      locationVariable.setType(descriptorOptional.get().getType());
      return descriptorOptional.get().getType();
    } else {
      errors.add(new SemanticError(
          locationVariable.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "No identifier can be used before it is declared: " + locationVariable.getLabel() + " not in scope"
      ));
      return Type.Undefined;
    }
  }

  @Override
  public Type visit(
      Len len,
      Scope scope
  ) {
    var arrayName = len.rValue.getLabel();
    var descriptor = scope.lookup(arrayName);
    if (descriptor.isEmpty()) {
      errors.add(new SemanticError(
          len.rValue.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          arrayName + " not in scope"
      ));
    } else if (!(descriptor.get() instanceof ArrayDescriptor)) {
      errors.add(new SemanticError(
          len.rValue.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "the argument of the len operator must be an array"
      ));
    }
    return Type.Int;
  }

  @Override
  public Type visit(
      Increment increment,
      Scope scope
  ) {
    return Type.Int;
  }

  @Override
  public Type visit(
      Decrement decrement,
      Scope scope
  ) {
    return Type.Int;
  }

  @Override
  public Type visit(
      CharLiteral charLiteral,
      Scope scope
  ) {
    charLiteral.setType(Type.Int);
    return Type.Int;
  }

  @Override
  public Type visit(
      StringLiteral stringLiteral,
      Scope scope
  ) {
    stringLiteral.type = Type.String;
    return Type.String;
  }

  @Override
  public Type visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      Scope curScope
  ) {
    compoundAssignOpExpr.expression.setType(compoundAssignOpExpr.expression.accept(
        this,
        curScope
    ));
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Initialization initialization,
      Scope scope
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Assignment assignment,
      Scope scope
  ) {
   return Type.Undefined;
  }

  @Override
  public Type visit(
      VoidExpression voidExpression,
      Scope scope
  ) {
    return Type.Void;
  }

  private void checkIntBounds(@NotNull IntLiteral intLiteral) {
    try {
      intLiteral.convertToLong();
    } catch (Exception e) {
      errors.add(new SemanticError(
          intLiteral.tokenPosition,
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
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
            "Field " + rValue.getLabel() + " shadows a parameter"
        ));
      } else if (scope.containsKey(rValue.getLabel())) {
        // field already declared in scope
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
            "Field " + rValue.getLabel() + " already declared"
        ));
      } else if (imports.contains(rValue.getLabel())) {
        // field already declared in scope
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_IMPORT,
            String.format(
                "Field `%s` shadows an import",
                rValue.getLabel()
            )
        ));
      } else {
        scope.put(
            rValue.getLabel(),
            new ValueDescriptor(
                type, false
            )
        );
      }
    }

    for (Array array : fieldDeclaration.arrays) {
      var arrayIdLabel = array.getLabel();
      if (scope.isShadowingParameter(array.getLabel())) {
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
            "Field " + arrayIdLabel + " shadows a parameter"
        ));
      } else if (scope
          .lookup(array.getLabel())
          .isPresent()) {
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
            "Field " + arrayIdLabel + " already declared"
        ));
      } else if (imports.contains(array.getLabel())) {
        // field already declared in scope
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_IMPORT,
            String.format(
                "Field `%s` shadows an import",
                array.getLabel()
            )
        ));
      } else {
        if (Integer.parseInt(array.getSize().literal) <= 0) {
          errors.add(
              new SemanticError(
                  array.getSize().tokenPosition,
                  SemanticError.SemanticErrorType.INVALID_ARRAY_SIZE,
                  "array declaration size " + array.getLabel() + "[" +
                      array.getSize().literal + "]"
                      + " must be greater than 0"
              ));
        } else {
          scope.put(
              arrayIdLabel,
              new ArrayDescriptor(
                  array.getSize()
                       .convertToLong(),
                  type == Type.Int ? Type.IntArray
                      : (type == Type.Bool) ? Type.BoolArray
                      : Type.Undefined
              )
          );
        }
      }
    }
    return Type.Undefined;
  }

  public @NotNull Type visit(@NotNull Break breakStatement, @NotNull Scope scope) {
    if (loopDepth < 1) {
      errors.add(new SemanticError(
          breakStatement.tokenPosition,
          SemanticError.SemanticErrorType.BREAK_OUT_OF_CONTEXT,
          "break statement must be enclosed within a loop"
      ));
    }
    return Type.Undefined;
  }

  public @NotNull Type visit(@NotNull Continue continueStatement, @NotNull Scope scope) {
    if (loopDepth < 1) {
      errors.add(new SemanticError(
          continueStatement.tokenPosition,
          SemanticError.SemanticErrorType.CONTINUE_OUT_OF_CONTEXT,
          "continue statement must be enclosed within a loop"

      ));
    }
    return Type.Undefined;
  }
}

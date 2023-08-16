package decaf.analysis.semantic;


import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import decaf.analysis.lexical.Scanner;
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
import decaf.analysis.syntax.ast.DecimalLiteral;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.EqualityOperator;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.For;
import decaf.analysis.syntax.ast.HexLiteral;
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
import decaf.analysis.syntax.ast.MethodCallParameter;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.MethodDefinitionParameter;
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
import decaf.shared.descriptors.ArrayDescriptor;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.env.Scope;
import decaf.shared.errors.SemanticError;

public class TypeChecker implements AstVisitor<Type> {
  private final List<SemanticError> errors;
  IntLiteral intLiteral = null;
  boolean negInt = false;
  private Scope methods;
  private Scope globalFields;
  private TreeSet<String> imports;
  private Type returnTypeSeen;

  public TypeChecker(
      Program root,
      Scope methods,
      Scope globalFields,
      TreeSet<String> imports,
      List<SemanticError> errors
  ) {
    this.setMethods(methods);
    this.setGlobalFields(globalFields);
    this.setImports(imports);
    this.errors = errors;
    visit(
        root,
        globalFields
    );
  }

  @Override
  public Type visit(
      IntLiteral intLiteral,
      Scope scope
  ) {
    this.intLiteral = intLiteral;
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
      DecimalLiteral decimalLiteral,
      Scope scope
  ) {
    intLiteral = decimalLiteral;
    return Type.Int;
  }

  @Override
  public Type visit(
      HexLiteral hexLiteral,
      Scope scope
  ) {
    intLiteral = hexLiteral;
    return Type.Int;
  }

  @Override
  public Type visit(
      FieldDeclaration fieldDeclaration,
      Scope scope
  ) {
    for (Array array : fieldDeclaration.arrays)
      array.accept(
          this,
          scope
      );
    return fieldDeclaration.getType();
  }

  @Override
  public Type visit(
      MethodDefinition methodDefinition,
      Scope scope
  ) {
    setReturnTypeSeen(Type.Undefined);
    methodDefinition.getBlock()
                    .accept(
                        this,
                        methodDefinition.getBlock().blockScope
                    );
    if (getMethods().lookup(methodDefinition.getMethodName()
                                            .getLabel())
                    .isPresent()) {
      Scope parameterScope = ((MethodDescriptor) getMethods().lookup(methodDefinition.getMethodName()
                                                                                     .getLabel())
                                                             .get()).parameterScope;
      for (MethodDefinitionParameter methodDefinitionParameter : methodDefinition.getParameterList())
        methodDefinitionParameter.accept(
            this,
            parameterScope
        );
      if (getReturnTypeSeen() != Type.Undefined && methodDefinition.getReturnType() != getReturnTypeSeen()) {
        errors.add(new SemanticError(
            methodDefinition.getMethodName().tokenPosition,
            SemanticError.SemanticErrorType.SHOULD_RETURN_VOID,
            String.format(
                "method `%s` must not return a value of type `%s` in a void method",
                methodDefinition.getMethodName()
                                .getLabel(),
                getReturnTypeSeen()
            )
        ));
      }
      setReturnTypeSeen(Type.Undefined);
    } else {
      errors.add(new SemanticError(
          methodDefinition.getTokenPosition(),
          SemanticError.SemanticErrorType.METHOD_DEFINITION_NOT_FOUND,
          "method definition not found"
      ));
    }
    return methodDefinition.getReturnType();
  }

  @Override
  public Type visit(
      ImportDeclaration importDeclaration,
      Scope scope
  ) {
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
      if (initDescriptor.type != Type.Int) {
        errors.add(new SemanticError(
            forStatement.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            forStatement.initialization.initLocation + " must type must be " + Type.Int + " not " + initDescriptor.type
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
      forStatement.block.accept(
          this,
          scope
      );
      optionalDescriptor = scope.lookup(forStatement.update.getLocation().RValue.getLabel());
      if (optionalDescriptor.isEmpty()) {
        errors.add(new SemanticError(
            forStatement.update.getLocation().tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            forStatement.update.getLocation().RValue + " must be declared in scope"
        ));
      } else {
        Descriptor updatingDescriptor = optionalDescriptor.get();
        if (updatingDescriptor.type != Type.Int) {
          errors.add(new SemanticError(
              forStatement.initialization.initExpression.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              "update location must have type int, not " + updatingDescriptor.type
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
                  updatingDescriptor.id,
                  updatingDescriptor.type,
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
      Break breakStatement,
      Scope scope
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Continue continueStatement,
      Scope scope
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      While whileStatement,
      Scope scope
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
    whileStatement.body.accept(
        this,
        scope
    );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Program program,
      Scope scope
  ) {
    for (FieldDeclaration fieldDeclaration : program.getFieldDeclarationList()) {
      fieldDeclaration.accept(
          this,
          scope
      );
    }
    for (MethodDefinition methodDefinition : program.getMethodDefinitionList()) {
      methodDefinition.accept(
          this,
          getMethods()
      );
    }
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
          "Can only use a not operator on Bools"
      ));
    }
    if (operandType != Type.Int && unaryOpExpression.getUnaryOperator().label.equals("-")) {
      errors.add(new SemanticError(
          unaryOpExpression.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "Can only use an unary minus operator on Integers"
      ));
    }
    if (operandType == Type.Int && unaryOpExpression.getUnaryOperator().label.equals("-")) negInt = !negInt;


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
    checkIntBounds();
    Type rightType = binaryOpExpression.rhs.accept(
        this,
        scope
    );
    checkIntBounds();

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
      Block block,
      Scope scope
  ) {
    for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
      fieldDeclaration.accept(
          this,
          block.blockScope
      );
    for (Statement statement : block.statementList)
      statement.accept(
          this,
          block.blockScope
      );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      ParenthesizedExpression parenthesizedExpression,
      Scope scope
  ) {
    Type type = parenthesizedExpression.expression.accept(
        this,
        scope
    );
    checkIntBounds();
    return type;
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
    Optional<Descriptor> descriptor = scope.lookup(locationArray.RValue.getLabel());
    if (descriptor.isPresent()) {
      if ((descriptor.get() instanceof ArrayDescriptor)) {
        switch (descriptor.get().type) {
          case Bool, BoolArray -> type = Type.Bool;
          case Int, IntArray -> type = Type.Int;
          default -> errors.add(new SemanticError(
              locationArray.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              locationArray.RValue.getLabel() + " must be an array"
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
    Type type = expressionParameter.expression.accept(
        this,
        scope
    );
    expressionParameter.type = type;
    checkIntBounds();
    return type;
  }

  @Override
  public Type visit(
      If ifStatement,
      Scope scope
  ) {
    Type type = ifStatement.test.accept(
        this,
        scope
    );
    if (type != Type.Bool) {
      errors.add(new SemanticError(
          ifStatement.test.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "if statement test must evaluate to a bool, not " + type
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
    checkIntBounds();
    setReturnTypeSeen(type);
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

  // The number and types of arguments in a method call
  private void checkNumberOfArgumentsAndTypesMatch(
      MethodDefinition methodDefinition,
      MethodCall methodCall
  ) {
    if (methodCall.methodCallParameterList.size() != methodDefinition.getParameterList()
                                                                     .size()) {
      errors.add(new SemanticError(
          methodCall.tokenPosition,
          SemanticError.SemanticErrorType.MISMATCHING_NUMBER_OR_ARGUMENTS,
          String.format(
              "method %s expects %d arguments but %d were passed in",
              methodCall.RValueId.getLabel(),
              methodDefinition.getParameterList()
                              .size(),
              methodCall.methodCallParameterList.size()
          )
      ));
      return;
    }
    for (int i = 0; i < methodCall.methodCallParameterList.size(); i++) {
      final MethodDefinitionParameter methodDefinitionParameter = methodDefinition.getParameterList()
                                                                                  .get(i);
      final MethodCallParameter methodCallParameter = methodCall.methodCallParameterList.get(i);
      if (methodCallParameter.type != methodDefinitionParameter.getType()) {
        errors.add(new SemanticError(
            methodCall.tokenPosition,
            SemanticError.SemanticErrorType.INCORRECT_ARG_TYPE,
            "method param " + methodDefinitionParameter.getName() + " is defined with type " +
                methodDefinitionParameter.getType() + " but " + methodCallParameter.type + " is passed in"
        ));
      }
    }
  }

  private void visitMethodCallParameters(
      List<MethodCallParameter> methodCallParameterList,
      Scope scope
  ) {
    for (MethodCallParameter methodCallParameter : methodCallParameterList) {
      methodCallParameter.accept(
          this,
          scope
      );
    }
  }

  @Override
  public Type visit(
      MethodCall methodCall,
      Scope scope
  ) {

    final Optional<Descriptor> optionalMethodDescriptor = getMethods().lookup(methodCall.RValueId.getLabel());
    final Descriptor descriptor;
    if (scope.containsKey(methodCall.RValueId.getLabel())) {
      errors.add(new SemanticError(
          methodCall.tokenPosition,
          SemanticError.SemanticErrorType.METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER,
          methodCall.RValueId.getLabel() + " refers to locally defined identifier"
      ));
      return Type.Undefined;
    }
    if (getImports().contains(methodCall.RValueId.getLabel())) {
      // All external functions are treated as if they return int
      methodCall.setType(Type.Int);
      visitMethodCallParameters(
          methodCall.methodCallParameterList,
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
        methodCall.methodCallParameterList,
        scope
    );
    checkNumberOfArgumentsAndTypesMatch(
        ((MethodDescriptor) descriptor).methodDefinition,
        methodCall
    );

    methodCall.setType(descriptor.type);
    return descriptor.type;
  }

  @Override
  public Type visit(
      MethodCallStatement methodCallStatement,
      Scope scope
  ) {
    if (!getImports().contains(methodCallStatement.methodCall.RValueId.getLabel()) &&
        !getMethods().containsKey(methodCallStatement.methodCall.RValueId.getLabel())) {
      errors.add(new SemanticError(
          methodCallStatement.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "identifier `" + methodCallStatement.methodCall.RValueId.getLabel() +
              "` in a method statement must be a declared method or import."
      ));
    }
    methodCallStatement.methodCall.accept(
        this,
        scope
    );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      LocationAssignExpr locationAssignExpr,
      Scope scope
  ) {
    Optional<Descriptor> optionalDescriptor = scope.lookup(locationAssignExpr.location.RValue.getLabel());
    if (optionalDescriptor.isEmpty() || (locationAssignExpr.location instanceof LocationVariable &&
        optionalDescriptor.get() instanceof ArrayDescriptor)) {
      if (optionalDescriptor.isEmpty()) {
        errors.add(new SemanticError(
            locationAssignExpr.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
            "id `" + locationAssignExpr.location.RValue.getLabel() +
                "` being assigned to must be a declared local/global irAssignableValue or formal parameter."
        ));
      } else {
        errors.add(new SemanticError(
            locationAssignExpr.tokenPosition,
            SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
            String.format(
                "`%s` is an array and cannot be modified directly like a variable",
                locationAssignExpr.location.RValue.getLabel()
            )
        ));
      }
    } else {
      if (locationAssignExpr.location instanceof final LocationArray locationArray) {
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
          if ((locationDescriptor.type == Type.Int || locationDescriptor.type == Type.IntArray) &&
              expressionType != Type.Int) {
            errors.add(new SemanticError(
                locationAssignExpr.tokenPosition,
                SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                "lhs is type " + locationDescriptor.type + " rhs must be type Int, not " + expressionType
            ));
          }
          if ((locationDescriptor.type == Type.Bool || locationDescriptor.type == Type.BoolArray) &&
              expressionType != Type.Bool) {
            errors.add(new SemanticError(
                locationAssignExpr.tokenPosition,
                SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
                "lhs is type " + locationDescriptor.type + " rhs must be type Bool, not " + expressionType
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
        if (!((locationDescriptor.type == Type.Int || locationDescriptor.type == Type.IntArray) &&
            expressionType == Type.Int)) {
          errors.add(new SemanticError(
              locationAssignExpr.tokenPosition,
              SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
              String.format(
                  "The location and the expression in an incrementing/decrementing assignment must be of type `int`, " +
                      "currently the location `%s` is of type `%s` and the expression `%s` is of type `%s`",
                  locationDescriptor.id,
                  locationDescriptor.type,
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
    Type type = assignOpExpr.expression.accept(
        this,
        scope
    );
    checkIntBounds();
    return type;
  }

  @Override
  public Type visit(
      MethodDefinitionParameter methodDefinitionParameter,
      Scope scope
  ) {
    return methodDefinitionParameter.getType();
  }

  @Override
  public Type visit(
      RValue RValue,
      Scope scope
  ) {
    return null;
  }

  @Override
  public Type visit(
      LocationVariable locationVariable,
      Scope scope
  ) {
    Optional<Descriptor> descriptorOptional = scope.lookup(locationVariable.RValue.getLabel());
    if (descriptorOptional.isPresent()) {
      locationVariable.setType(descriptorOptional.get().type);
      return descriptorOptional.get().type;
    } else {
      errors.add(new SemanticError(
          locationVariable.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "No identifier can be used before it is declared: " + locationVariable.RValue.getLabel() + " not in scope"
      ));
      return Type.Undefined;
    }
  }

  @Override
  public Type visit(
      Len len,
      Scope scope
  ) {
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
    return null;
  }

  @Override
  public Type visit(
      Assignment assignment,
      Scope scope
  ) {
    return null;
  }

  @Override
  public Type visit(
      VoidExpression voidExpression,
      Scope scope
  ) {
    return Type.Void;
  }


  private void checkIntBounds() {
    if (intLiteral != null) {
      try {
        if (negInt) {
          if (intLiteral instanceof HexLiteral) Long.parseLong(
              "-" + intLiteral.literal.substring(2),
              16
          );
          else Long.parseLong("-" + intLiteral.literal);
        } else {
          if (intLiteral instanceof HexLiteral) Long.parseLong(
              intLiteral.literal.substring(2),
              16
          );
          else Long.parseLong(intLiteral.literal);
        }

      } catch (Exception e) {
        errors.add(new SemanticError(
            intLiteral.tokenPosition,
            SemanticError.SemanticErrorType.INT_LITERAL_TOO_BIG,
            "Encountered int literal that's out of bounds; -9223372036854775808 <= x <= 9223372036854775807"
        ));
      }
    }

    intLiteral = null;
    negInt = false;
  }

  public Scope getMethods() {
    return methods;
  }

  public void setMethods(Scope methods) {
    this.methods = methods;
  }

  public Scope getGlobalFields() {
    return globalFields;
  }

  public void setGlobalFields(Scope globalFields) {
    this.globalFields = globalFields;
  }

  public TreeSet<String> getImports() {
    return imports;
  }

  public void setImports(TreeSet<String> imports) {
    this.imports = imports;
  }

  public Type getReturnTypeSeen() {
    return returnTypeSeen;
  }

  public void setReturnTypeSeen(Type returnTypeSeen) {
    this.returnTypeSeen = returnTypeSeen;
  }
}

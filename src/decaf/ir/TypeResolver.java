package decaf.ir;


import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import decaf.ast.ArithmeticOperator;
import decaf.ast.Array;
import decaf.ast.AssignOpExpr;
import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.Break;
import decaf.ast.CharLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.ConditionalOperator;
import decaf.ast.Continue;
import decaf.ast.DecimalLiteral;
import decaf.ast.Decrement;
import decaf.ast.EqualityOperator;
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
import decaf.ast.While;
import decaf.descriptors.ArrayDescriptor;
import decaf.descriptors.Descriptor;
import decaf.descriptors.MethodDescriptor;
import decaf.exceptions.DecafSemanticException;
import decaf.grammar.DecafScanner;
import decaf.symboltable.SymbolTable;

public class TypeResolver implements AstVisitor<Type> {
  IntLiteral intLiteral = null;
  boolean negInt = false;
  private SymbolTable methods;
  private SymbolTable globalFields;
  private TreeSet<String> imports;
  private Type returnTypeSeen;

  public TypeResolver(
      Program root,
      SymbolTable methods,
      SymbolTable globalFields,
      TreeSet<String> imports
  ) {
    this.setMethods(methods);
    this.setGlobalFields(globalFields);
    this.setImports(imports);
    visit(
        root,
        globalFields
    );
  }

  @Override
  public Type visit(
      IntLiteral intLiteral,
      SymbolTable symbolTable
  ) {
    this.intLiteral = intLiteral;
    return Type.Int;
  }

  @Override
  public Type visit(
      BooleanLiteral booleanLiteral,
      SymbolTable symbolTable
  ) {
    return Type.Bool;
  }

  @Override
  public Type visit(
      DecimalLiteral decimalLiteral,
      SymbolTable symbolTable
  ) {
    intLiteral = decimalLiteral;
    return Type.Int;
  }

  @Override
  public Type visit(
      HexLiteral hexLiteral,
      SymbolTable symbolTable
  ) {
    intLiteral = hexLiteral;
    return Type.Int;
  }

  @Override
  public Type visit(
      FieldDeclaration fieldDeclaration,
      SymbolTable symbolTable
  ) {
    for (Array array : fieldDeclaration.arrays)
      array.accept(
          this,
          symbolTable
      );
    return fieldDeclaration.getType();
  }

  @Override
  public Type visit(
      MethodDefinition methodDefinition,
      SymbolTable symbolTable
  ) {
    setReturnTypeSeen(Type.Undefined);
    methodDefinition.getBlock()
                    .accept(
                        this,
                        methodDefinition.getBlock().blockSymbolTable
                    );
    if (getMethods().getDescriptorFromCurrentScope(methodDefinition.getMethodName()
                                                                   .getLabel())
                    .isPresent()) {
      SymbolTable parameterSymbolTable = ((MethodDescriptor) getMethods().getDescriptorFromCurrentScope(methodDefinition.getMethodName()
                                                                                                                        .getLabel())
                                                                         .get()).parameterSymbolTable;
      for (MethodDefinitionParameter methodDefinitionParameter : methodDefinition.getParameterList())
        methodDefinitionParameter.accept(
            this,
            parameterSymbolTable
        );
      if (getReturnTypeSeen() != Type.Undefined && methodDefinition.getReturnType() != getReturnTypeSeen()) {
        exceptions.add(new DecafSemanticException(
            methodDefinition.getTokenPosition(),
            methodDefinition.getMethodName()
                            .getLabel() + " does not have a declared type " + methodDefinition.getReturnType() +
                " instead it returns type " + getReturnTypeSeen()
        ));
      }
      setReturnTypeSeen(Type.Undefined);
    } else {
      exceptions.add(new DecafSemanticException(
          methodDefinition.getTokenPosition(),
          "method definition not found"
      ));
    }
    return methodDefinition.getReturnType();
  }

  @Override
  public Type visit(
      ImportDeclaration importDeclaration,
      SymbolTable symbolTable
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      For forStatement,
      SymbolTable symbolTable
  ) {
    Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(forStatement.initialization.initLocation.getLabel());
    if (optionalDescriptor.isEmpty()) exceptions.add(new DecafSemanticException(
        forStatement.initialization.initLocation.tokenPosition,
        forStatement.initialization.initLocation + " must be declared in scope"
    ));
    else {
      Descriptor initDescriptor = optionalDescriptor.get();
      if (initDescriptor.type != Type.Int) exceptions.add(new DecafSemanticException(
          forStatement.tokenPosition,
          forStatement.initialization.initLocation + " must type must be " + Type.Int + " not " + initDescriptor.type
      ));

      Type type = forStatement.initialization.initExpression.accept(
          this,
          symbolTable
      );
      if (type != Type.Int) exceptions.add(new DecafSemanticException(
          forStatement.initialization.initExpression.tokenPosition,
          "init expression must evaluate to an int"
      ));

      Type testType = forStatement.terminatingCondition.accept(
          this,
          symbolTable
      );
      if (testType != Type.Bool) exceptions.add(new DecafSemanticException(
          forStatement.terminatingCondition.tokenPosition,
          "for-loop test must evaluate to " + Type.Bool + " not " + testType
      ));
      forStatement.block.accept(
          this,
          symbolTable
      );
      optionalDescriptor = symbolTable.getDescriptorFromValidScopes(forStatement.update.getLocation().name.getLabel());
      if (optionalDescriptor.isEmpty()) exceptions.add(new DecafSemanticException(
          forStatement.update.getLocation().tokenPosition,
          forStatement.update.getLocation().name + " must be declared in scope"
      ));
      else {
        Descriptor updatingDescriptor = optionalDescriptor.get();
        if (updatingDescriptor.type != Type.Int) exceptions.add(new DecafSemanticException(
            forStatement.initialization.initExpression.tokenPosition,
            "update location must have type int, not " + updatingDescriptor.type
        ));
        Type updateExprType = forStatement.update.assignExpr.accept(
            this,
            symbolTable
        );
        if (forStatement.update.assignExpr instanceof CompoundAssignOpExpr)
          updateExprType = forStatement.update.assignExpr.expression.getType();
        if (updateExprType != Type.Int) exceptions.add(new DecafSemanticException(
            forStatement.update.assignExpr.tokenPosition,
            "incrementing/decrementing must have type int, not " + updateExprType
        ));
      }
    }
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Break breakStatement,
      SymbolTable symbolTable
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Continue continueStatement,
      SymbolTable symbolTable
  ) {
    return Type.Undefined;
  }

  @Override
  public Type visit(
      While whileStatement,
      SymbolTable symbolTable
  ) {
    Type type = whileStatement.test.accept(
        this,
        symbolTable
    );
    if (type != Type.Bool) {
      exceptions.add(new DecafSemanticException(
          whileStatement.test.tokenPosition,
          "while statement test must evaluate to a bool, not " + type
      ));
    }
    whileStatement.body.accept(
        this,
        symbolTable
    );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Program program,
      SymbolTable symbolTable
  ) {
    for (FieldDeclaration fieldDeclaration : program.getFieldDeclarationList()) {
      fieldDeclaration.accept(
          this,
          symbolTable
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
      SymbolTable symbolTable
  ) {
    Type operandType = unaryOpExpression.operand.accept(
        this,
        symbolTable
    );
    if (operandType != Type.Bool && operandType != Type.Int) {
      exceptions.add(new DecafSemanticException(
          unaryOpExpression.tokenPosition,
          "could not infer"
      ));
    }
    if (operandType != Type.Bool && unaryOpExpression.getUnaryOperator().label.equals("!")) {
      exceptions.add(new DecafSemanticException(
          unaryOpExpression.tokenPosition,
          "Can only use a not operator on Bools"
      ));
    }
    if (operandType != Type.Int && unaryOpExpression.getUnaryOperator().label.equals("-")) {
      exceptions.add(new DecafSemanticException(
          unaryOpExpression.tokenPosition,
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
      SymbolTable symbolTable
  ) {
    Type leftType = binaryOpExpression.lhs.accept(
        this,
        symbolTable
    );
    checkIntBounds();
    Type rightType = binaryOpExpression.rhs.accept(
        this,
        symbolTable
    );
    checkIntBounds();

    Type binaryOpExpressionType = Type.Undefined;
    if (leftType != Type.Undefined && rightType != Type.Undefined) {
      if (binaryOpExpression.op instanceof ConditionalOperator) {
        if (leftType == Type.Bool && rightType == Type.Bool) {
          binaryOpExpressionType = Type.Bool;
        } else if (leftType != Type.Bool) {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
              "lhs must have type bool not `" + leftType + " `"
          ));
        } else {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
              "rhs must have type bool not `" + rightType + " `"
          ));
        }
      } else if (binaryOpExpression.op instanceof ArithmeticOperator) {
        if (leftType == Type.Int && rightType == Type.Int) {
          binaryOpExpressionType = Type.Int;
        } else if (leftType != Type.Int) {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
              "lhs must have type int not `" + leftType + " `"
          ));
        } else {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
              "rhs must have type int not `" + rightType + " `"
          ));
        }
      } else if (binaryOpExpression.op instanceof EqualityOperator) {
        if (leftType.equals(rightType) && (leftType == Type.Int || leftType == Type.Bool))
          binaryOpExpressionType = Type.Bool;
        else exceptions.add(new DecafSemanticException(
            binaryOpExpression.tokenPosition,
            "operands of " + binaryOpExpression.op + " must have same type, either both bool or both int, not `" +
                leftType + "` and `" + rightType
        ));
      } else if (binaryOpExpression.op instanceof RelationalOperator) {
        if (leftType.equals(Type.Int) && rightType.equals(Type.Int)) {
          binaryOpExpressionType = Type.Bool;
        } else if (leftType != Type.Int) {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
              "lhs must have type int not `" + leftType + " `"
          ));
        } else {
          exceptions.add(new DecafSemanticException(
              binaryOpExpression.tokenPosition,
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
      SymbolTable symbolTable
  ) {
    for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
      fieldDeclaration.accept(
          this,
          block.blockSymbolTable
      );
    for (Statement statement : block.statementList)
      statement.accept(
          this,
          block.blockSymbolTable
      );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      ParenthesizedExpression parenthesizedExpression,
      SymbolTable symbolTable
  ) {
    Type type = parenthesizedExpression.expression.accept(
        this,
        symbolTable
    );
    checkIntBounds();
    return type;
  }

  @Override
  public Type visit(
      LocationArray locationArray,
      SymbolTable symbolTable
  ) {
    if (locationArray.expression.accept(
        this,
        symbolTable
    ) != Type.Int) {
      exceptions.add(new DecafSemanticException(
          locationArray.tokenPosition,
          "array index must evaluate to int"
      ));
    }
    Type type = Type.Undefined;
    Optional<Descriptor> descriptor = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
    if (descriptor.isPresent()) {
      if ((descriptor.get() instanceof ArrayDescriptor)) {
        switch (descriptor.get().type) {
          case Bool, BoolArray -> type = Type.Bool;
          case Int, IntArray -> type = Type.Int;
          default -> exceptions.add(new DecafSemanticException(
              locationArray.tokenPosition,
              locationArray.name.getLabel() + " must be an array"
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
      SymbolTable symbolTable
  ) {
    Type type = expressionParameter.expression.accept(
        this,
        symbolTable
    );
    expressionParameter.type = type;
    checkIntBounds();
    return type;
  }

  @Override
  public Type visit(
      If ifStatement,
      SymbolTable symbolTable
  ) {
    Type type = ifStatement.test.accept(
        this,
        symbolTable
    );
    if (type != Type.Bool) {
      exceptions.add(new DecafSemanticException(
          ifStatement.test.tokenPosition,
          "if statement test must evaluate to a bool, not " + type
      ));
    }
    ifStatement.ifBlock.accept(
        this,
        symbolTable
    );
    if (ifStatement.elseBlock != null) {
      ifStatement.elseBlock.accept(
          this,
          symbolTable
      );
    }
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Return returnStatement,
      SymbolTable symbolTable
  ) {
    Type type = Type.Void;
    if (returnStatement.retExpression != null) {
      type = returnStatement.retExpression.accept(
          this,
          symbolTable
      );
      checkIntBounds();
    }
    setReturnTypeSeen(type);
    return Type.Void;
  }

  @Override
  public Type visit(
      Array array,
      SymbolTable symbolTable
  ) {
    if (array.getSize()
             .convertToLong() <= 0) {
      exceptions.add(new DecafSemanticException(
          array.getSize().tokenPosition,
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
      exceptions.add(new DecafSemanticException(
          methodCall.tokenPosition,
          "unequal number of args"
      ));
      return;
    }
    for (int i = 0; i < methodCall.methodCallParameterList.size(); i++) {
      final MethodDefinitionParameter methodDefinitionParameter = methodDefinition.getParameterList()
                                                                                  .get(i);
      final MethodCallParameter methodCallParameter = methodCall.methodCallParameterList.get(i);
      if (methodCallParameter.type != methodDefinitionParameter.getType()) {
        exceptions.add(new DecafSemanticException(
            methodCall.tokenPosition,
            "method param " + methodDefinitionParameter.getName() + " is defined with type " +
                methodDefinitionParameter.getType() + " but " + methodCallParameter.type + " is passed in"
        ));
      }
    }
  }

  private void visitMethodCallParameters(
      List<MethodCallParameter> methodCallParameterList,
      SymbolTable symbolTable
  ) {
    for (MethodCallParameter methodCallParameter : methodCallParameterList) {
      methodCallParameter.accept(
          this,
          symbolTable
      );
    }
  }

  @Override
  public Type visit(
      MethodCall methodCall,
      SymbolTable symbolTable
  ) {

    final Optional<Descriptor> optionalMethodDescriptor = getMethods().getDescriptorFromCurrentScope(methodCall.nameId.getLabel());
    final Descriptor descriptor;
    if (symbolTable.containsEntry(methodCall.nameId.getLabel())) {
      exceptions.add(new DecafSemanticException(
          methodCall.tokenPosition,
          methodCall.nameId.getLabel() + " refers to locally defined irAssignableValue"
      ));
      return Type.Undefined;
    }
    if (getImports().contains(methodCall.nameId.getLabel())) {
      // All external functions are treated as if they return int
      methodCall.setType(Type.Int);
      visitMethodCallParameters(
          methodCall.methodCallParameterList,
          symbolTable
      );
      methodCall.isImported = true;
      return Type.Int;
    }
    if (optionalMethodDescriptor.isPresent()) {
      descriptor = optionalMethodDescriptor.get();
    } else {
      exceptions.add(new DecafSemanticException(
          methodCall.tokenPosition,
          "method " + methodCall.nameId.getLabel() + " not found"
      ));
      return Type.Undefined;
    }
    visitMethodCallParameters(
        methodCall.methodCallParameterList,
        symbolTable
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
      SymbolTable symbolTable
  ) {
    if (!getImports().contains(methodCallStatement.methodCall.nameId.getLabel()) &&
        !getMethods().containsEntry(methodCallStatement.methodCall.nameId.getLabel())) {
      exceptions.add(new DecafSemanticException(
          methodCallStatement.tokenPosition,
          "identifier `" + methodCallStatement.methodCall.nameId.getLabel() +
              "` in a method statement must be a declared method or import."
      ));
    }
    methodCallStatement.methodCall.accept(
        this,
        symbolTable
    );
    return Type.Undefined;
  }

  @Override
  public Type visit(
      LocationAssignExpr locationAssignExpr,
      SymbolTable symbolTable
  ) {
    Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(locationAssignExpr.location.name.getLabel());
    if (optionalDescriptor.isEmpty() || (locationAssignExpr.location instanceof LocationVariable &&
        optionalDescriptor.get() instanceof ArrayDescriptor)) exceptions.add(new DecafSemanticException(
        locationAssignExpr.tokenPosition,
        "id `" + locationAssignExpr.location.name.getLabel() +
            "` being assigned to must be a declared local/global irAssignableValue or formal parameter."
    ));
    else {
      if (locationAssignExpr.location instanceof final LocationArray locationArray) {
        locationArray.expression.setType(locationArray.expression.accept(
            this,
            symbolTable
        ));
        if (locationArray.expression.getType() != Type.Int) {
          exceptions.add(new DecafSemanticException(
              locationAssignExpr.tokenPosition,
              "array index must evaluate to an int"
          ));
        }
      }
      final Type expressionType = locationAssignExpr.assignExpr.accept(
          this,
          symbolTable
      );
      final Descriptor locationDescriptor = optionalDescriptor.get();

      if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
        if (assignOpExpr.assignOp.label.equals(DecafScanner.ASSIGN)) {
          if ((locationDescriptor.type == Type.Int || locationDescriptor.type == Type.IntArray) &&
              expressionType != Type.Int) {
            exceptions.add(new DecafSemanticException(
                locationAssignExpr.tokenPosition,
                "lhs is type " + locationDescriptor.type + " rhs must be type Int, not " + expressionType
            ));
          }
          if ((locationDescriptor.type == Type.Bool || locationDescriptor.type == Type.BoolArray) &&
              expressionType != Type.Bool) {
            exceptions.add(new DecafSemanticException(
                locationAssignExpr.tokenPosition,
                "lhs is type " + locationDescriptor.type + " rhs must be type Bool, not " + expressionType
            ));
          }
        }
      }
      if (assignOperatorEquals(
          locationAssignExpr,
          DecafScanner.ADD_ASSIGN
      ) || assignOperatorEquals(
          locationAssignExpr,
          DecafScanner.MINUS_ASSIGN
      ) || assignOperatorEquals(
          locationAssignExpr,
          DecafScanner.MULTIPLY_ASSIGN
      ) || locationAssignExpr.assignExpr instanceof Decrement || locationAssignExpr.assignExpr instanceof Increment) {
        // both must be of type int
        if (!((locationDescriptor.type == Type.Int || locationDescriptor.type == Type.IntArray) &&
            expressionType == Type.Int)) {
          exceptions.add(new DecafSemanticException(
              locationAssignExpr.tokenPosition,
              "The location and the expression in an incrementing/decrementing assignment must be of type int"
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
      SymbolTable symbolTable
  ) {
    Type type = assignOpExpr.expression.accept(
        this,
        symbolTable
    );
    checkIntBounds();
    return type;
  }

  @Override
  public Type visit(
      MethodDefinitionParameter methodDefinitionParameter,
      SymbolTable symbolTable
  ) {
    return methodDefinitionParameter.getType();
  }

  @Override
  public Type visit(
      Name name,
      SymbolTable symbolTable
  ) {
    return null;
  }

  @Override
  public Type visit(
      LocationVariable locationVariable,
      SymbolTable symbolTable
  ) {
    Optional<Descriptor> descriptorOptional = symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel());
    if (descriptorOptional.isPresent()) {
      locationVariable.setType(descriptorOptional.get().type);
      return descriptorOptional.get().type;
    } else {
      exceptions.add(new DecafSemanticException(
          locationVariable.tokenPosition,
          "No identifier can be used before it is declared: " + locationVariable.name.getLabel() + " not in scope"
      ));
      return Type.Undefined;
    }
  }

  @Override
  public Type visit(
      Len len,
      SymbolTable symbolTable
  ) {
    return Type.Int;
  }

  @Override
  public Type visit(
      Increment increment,
      SymbolTable symbolTable
  ) {
    return Type.Int;
  }

  @Override
  public Type visit(
      Decrement decrement,
      SymbolTable symbolTable
  ) {
    return Type.Int;
  }

  @Override
  public Type visit(
      CharLiteral charLiteral,
      SymbolTable symbolTable
  ) {
    charLiteral.setType(Type.Int);
    return Type.Int;
  }

  @Override
  public Type visit(
      StringLiteral stringLiteral,
      SymbolTable symbolTable
  ) {
    stringLiteral.type = Type.String;
    return Type.String;
  }

  @Override
  public Type visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      SymbolTable curSymbolTable
  ) {
    compoundAssignOpExpr.expression.setType(compoundAssignOpExpr.expression.accept(
        this,
        curSymbolTable
    ));
    return Type.Undefined;
  }

  @Override
  public Type visit(
      Initialization initialization,
      SymbolTable symbolTable
  ) {
    return null;
  }

  @Override
  public Type visit(
      Assignment assignment,
      SymbolTable symbolTable
  ) {
    return null;
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
        exceptions.add(new DecafSemanticException(
            intLiteral.tokenPosition,
            "Encountered int literal that's out of bounds"
        ));
      }
    }

    intLiteral = null;
    negInt = false;
  }

  public SymbolTable getMethods() {
    return methods;
  }

  public void setMethods(SymbolTable methods) {
    this.methods = methods;
  }

  public SymbolTable getGlobalFields() {
    return globalFields;
  }

  public void setGlobalFields(SymbolTable globalFields) {
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

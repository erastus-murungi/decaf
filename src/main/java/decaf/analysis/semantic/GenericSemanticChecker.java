package decaf.analysis.semantic;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Array;
import decaf.analysis.syntax.ast.AssignOpExpr;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.Break;
import decaf.analysis.syntax.ast.CharLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.Continue;
import decaf.analysis.syntax.ast.DecimalLiteral;
import decaf.analysis.syntax.ast.Decrement;
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
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.Statement;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.Type;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.analysis.syntax.ast.VoidExpression;
import decaf.analysis.syntax.ast.While;
import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.descriptors.ArrayDescriptor;
import decaf.shared.descriptors.Descriptor;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.descriptors.ParameterDescriptor;
import decaf.shared.descriptors.VariableDescriptor;
import decaf.shared.env.Scope;
import decaf.shared.errors.SemanticError;

public class GenericSemanticChecker implements AstVisitor<Void> {
  private final TreeSet<String> imports = new TreeSet<>();
  private final List<SemanticError> errors;
  private final Scope fields =
      new Scope(
          null,
          Scope.For.Field,
          null
      );
  private final Scope methods =
      new Scope(
          null,
          Scope.For.Method,
          null
      );
  int depth = 0; // the number of nested while/for loops we are in

  public GenericSemanticChecker(List<SemanticError> errors) {
    this.errors = errors;
  }

  public Void visit(IntLiteral intLiteral, Scope scope) {
    return null;
  }

  public Void visit(BooleanLiteral booleanLiteral, Scope scope) {
    return null;
  }

  public Void visit(DecimalLiteral decimalLiteral, Scope scope) {
    return null;
  }

  public Void visit(HexLiteral hexLiteral, Scope scope) {
    return null;
  }

  public Void visit(
      FieldDeclaration fieldDeclaration,
      Scope scope
  ) {
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
        // fields just declared do not have a value.
        scope.put(
            rValue.getLabel(),
            new VariableDescriptor(
                rValue.getLabel(),
                type
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
        // TODO: Check hex parse long
        scope.put(
            arrayIdLabel,
            new ArrayDescriptor(
                arrayIdLabel,
                array.getSize()
                     .convertToLong(),
                type == Type.Int ? Type.IntArray
                    : (type == Type.Bool) ? Type.BoolArray
                    : Type.Undefined
            )
        );
      }
    }
    return null;
  }

  private String
  simplify(List<MethodDefinitionParameter> methodDefinitionParameterList) {
    if (methodDefinitionParameterList.isEmpty()) {
      return "";
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (MethodDefinitionParameter methodDefinitionParameter :
        methodDefinitionParameterList) {
      stringBuilder.append(
          Utils.coloredPrint(
              methodDefinitionParameter.getType()
                                       .toString(),
              Utils.ANSIColorConstants.ANSI_PURPLE
          ));
      stringBuilder.append(" ");
      stringBuilder.append(
          Utils.coloredPrint(
              methodDefinitionParameter.getName(),
              Utils.ANSIColorConstants.ANSI_WHITE
          ));
      stringBuilder.append(", ");
    }
    stringBuilder.delete(
        stringBuilder.length() - 2,
        stringBuilder.length()
    );
    return stringBuilder.toString();
  }

  private void
  checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
    for (MethodDefinition methodDefinition : methodDefinitionList) {
      if (methodDefinition.getMethodName()
                          .getLabel()
                          .equals("main")) {
        if (methodDefinition.getReturnType() == Type.Void) {
          if (!(methodDefinition.getParameterList()
                                .isEmpty())) {
            errors.add(new SemanticError(
                methodDefinition.getTokenPosition(),
                SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
                "main method must have no parameters, yours has: " +
                    simplify(methodDefinition.getParameterList())
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

  public Void visit(
      MethodDefinition methodDefinition,
      Scope scope
  ) {
    Scope parameterScope = new Scope(
        getFields(),
        Scope.For.Parameter,
        methodDefinition.getBlock()
    );
    Scope localScope =
        new Scope(
            parameterScope,
            Scope.For.Field,
            methodDefinition.getBlock()
        );
    if (getMethods().containsKey(
        methodDefinition.getMethodName()
                        .getLabel()) ||
        getImports().contains(methodDefinition.getMethodName()
                                              .getLabel()) ||
        getFields().containsKey(
            methodDefinition.getMethodName()
                            .getLabel())) {
      // method already defined. add an exception
      errors.add(new SemanticError(
          methodDefinition.getTokenPosition(),
          SemanticError.SemanticErrorType.METHOD_ALREADY_DEFINED,
          String.format(
              "Method ``%s`` already defined",
              methodDefinition.getMethodName()
                              .getLabel()
          )
      ));
      methodDefinition.getBlock().blockScope = localScope;
    } else {
      for (var parameter : methodDefinition.getParameterList()) {
        parameter.accept(
            this,
            parameterScope
        );
      }
      // visit the method definition and populate the local symbol table
      getMethods().put(
          methodDefinition.getMethodName()
                          .getLabel(),
          new MethodDescriptor(
              methodDefinition,
              parameterScope,
              localScope
          )
      );
      methodDefinition.getBlock()
                      .accept(
                          this,
                          localScope
                      );
    }
    return null;
  }

  public Void visit(
      ImportDeclaration importDeclaration,
      Scope scope
  ) {
    if (scope.isShadowingParameter(importDeclaration.RValueId.getLabel())) {
      errors.add(new SemanticError(
          importDeclaration.RValueId.tokenPosition,
          SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
          "Import identifier " + importDeclaration.RValueId.getLabel() +
              " shadows a parameter"
      ));
    } else if (getImports().contains(importDeclaration.RValueId.getLabel())) {
      errors.add(new SemanticError(
          new TokenPosition(
              0,
              0,
              0
          ),
          SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
          "Import identifier " + importDeclaration.RValueId.getLabel() +
              " already declared"
      ));
    } else {
      getImports().add(importDeclaration.RValueId.getLabel());
    }
    return null;
  }

  public Void visit(For forStatement, Scope scope) {
    // this is the name of our loop irAssignableValue that we initialize in the
    // creation of the for loop for ( index = 0 ...) <-- index is the example
    // here
    String initializedVariableName =
        forStatement.initialization.initLocation.getLabel();

    if (scope.lookup(initializedVariableName)
             .isPresent()) {
      ++depth;
      forStatement.block.accept(
          this,
          scope
      );
      --depth;
    } else {
      // the irAssignableValue referred to was not declared. Add an exception.
      errors.add(new SemanticError(
          forStatement.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Variable " + initializedVariableName + " was not declared"
      ));
    }
    return null;
  }

  public Void visit(Break breakStatement, Scope scope) {
    if (depth < 1) {
      errors.add(new SemanticError(
          breakStatement.tokenPosition,
          SemanticError.SemanticErrorType.BREAK_STATEMENT_NOT_ENCLOSED,
          String.format(
              "break statement not enclosed; it is at a perceived depth of %s",
              depth
          )
      ));
    }
    return null;
  }

  public Void visit(Continue continueStatement, Scope scope) {
    if (depth < 1) {
      errors.add(new SemanticError(
          continueStatement.tokenPosition,
          SemanticError.SemanticErrorType.CONTINUE_STATEMENT_NOT_ENCLOSED,
          String.format(
              "continue statement not enclosed; it is at a perceived depth of %s",
              depth
          )
      ));
    }
    return null;
  }

  public Void visit(While whileStatement, Scope scope) {
    whileStatement.test.accept(
        this,
        scope
    );
    ++depth;
    whileStatement.body.accept(
        this,
        scope
    );
    --depth;
    return null;
  }

  public Void visit(Program program, Scope scope) {
    checkMainMethodExists(program.getMethodDefinitionList());
    for (ImportDeclaration importDeclaration :
        program.getImportDeclarationList())
      getImports().add(importDeclaration.RValueId.getLabel());
    for (FieldDeclaration fieldDeclaration : program.getFieldDeclarationList())
      fieldDeclaration.accept(
          this,
          getFields()
      );
    for (MethodDefinition methodDefinition : program.getMethodDefinitionList())
      methodDefinition.accept(
          this,
          getMethods()
      );
    return null;
  }

  public Void visit(
      UnaryOpExpression unaryOpExpression,
      Scope scope
  ) {
    return null;
  }

  public Void visit(
      BinaryOpExpression binaryOpExpression,
      Scope scope
  ) {
    return null;
  }

  public Void visit(Block block, Scope scope) {
    Scope blockST =
        new Scope(
            scope,
            Scope.For.Field,
            block
        );
    block.blockScope = blockST;
    scope.children.add(blockST);
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
    return null;
  }

  public Void visit(
      ParenthesizedExpression parenthesizedExpression,
      Scope scope
  ) {
    return null;
  }

  public Void visit(LocationArray locationArray, Scope scope) {
    final Optional<Descriptor> optionalDescriptor =
        scope.lookup(locationArray.RValue.getLabel());
    if (optionalDescriptor.isEmpty())
      errors.add(new SemanticError(
          locationArray.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Array " + locationArray.RValue.getLabel() + " not declared"
      ));
    else if (!(optionalDescriptor.get() instanceof ArrayDescriptor))
      errors.add(new SemanticError(
          locationArray.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          locationArray.RValue.getLabel() + " is not an array"
      ));
    locationArray.expression.accept(
        this,
        scope
    );
    return null;
  }

  public Void visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      Scope scope
  ) {
    compoundAssignOpExpr.expression.accept(
        this,
        scope
    );
    return null;
  }

  @Override
  public Void visit(Initialization initialization, Scope scope) {
    return null;
  }

  @Override
  public Void visit(Assignment assignment, Scope scope) {
    return null;
  }

  @Override
  public Void visit(VoidExpression voidExpression, Scope scope) {
    return null;
  }

  public Void visit(
      ExpressionParameter expressionParameter,
      Scope scope
  ) {
    return null;
  }

  public Void visit(If ifStatement, Scope scope) {
    // irAssignableValue lookup happens in ifCondition or if/else body
    for (Pair<String, AST> child : ifStatement.getChildren())
      child.second()
           .accept(
               this,
               scope
           );
    return null;
  }

  public Void visit(Return returnStatement, Scope scope) {
    if (!returnStatement.isTerminal())
      returnStatement.retExpression.accept(
          this,
          scope
      );

    return null;
  }

  public Void visit(Array array, Scope scope) {
    if (Integer.parseInt(array.getSize().literal) <= 0) {
      errors.add(
          new SemanticError(
              array.getSize().tokenPosition,
              SemanticError.SemanticErrorType.INVALID_ARRAY_SIZE,
              "array declaration size " + array.getLabel() + "[" +
                  array.getSize().literal + "]"
                  + " must be greater than 0"
          ));
    }
    return null;
  }

  public Void visit(MethodCall methodCall, Scope scope) {
    RValue methodRValue = methodCall.RValueId;

    if (getMethods()
        .lookup(methodRValue.getLabel())
        .isEmpty() &&
        !getImports().contains(methodRValue.getLabel()))
      errors.add(new SemanticError(
          methodRValue.tokenPosition,
          SemanticError.SemanticErrorType.METHOD_DEFINITION_NOT_FOUND,
          "Method name " + methodRValue.getLabel() + " hasn't been defined yet"
      ));

    for (MethodCallParameter parameter : methodCall.methodCallParameterList) {
      // TODO: partial rule 7 - array checking will be done in second pass
      if (!getImports().contains(methodRValue.getLabel()) && parameter instanceof
          StringLiteral)
        errors.add(new SemanticError(
            methodRValue.tokenPosition,
            SemanticError.SemanticErrorType.INVALID_ARGUMENT_TYPE,
            "String " + parameter +
                " cannot be arguments to non-import methods"
        ));
      parameter.accept(
          this,
          scope
      );
    }
    return null;
  }

  public Void visit(
      MethodCallStatement methodCallStatement,
      Scope scope
  ) {
    methodCallStatement.methodCall.accept(
        this,
        scope
    );
    return null;
  }

  public Void visit(
      LocationAssignExpr locationAssignExpr,
      Scope scope
  ) {
    locationAssignExpr.location.accept(
        this,
        scope
    );
    return locationAssignExpr.assignExpr.accept(
        this,
        scope
    );
  }

  public Void visit(AssignOpExpr assignOpExpr, Scope scope) {
    // no node for AssignOperator?
    assignOpExpr.expression.accept(
        this,
        scope
    );
    return null;
  }

  public Void visit(
      MethodDefinitionParameter methodDefinitionParameter,
      Scope scope
  ) {
    var paramName = methodDefinitionParameter.getName();
    var paramType = methodDefinitionParameter.getType();
    if (scope.isShadowingParameter(paramName))
      errors.add(
          new SemanticError(
              methodDefinitionParameter.tokenPosition,
              SemanticError.SemanticErrorType.SHADOWING_FORMAL_ARGUMENT,
              "MethodDefinitionParameter " + paramName +
                  " is shadowing a parameter"
          ));
    else
      scope.put(
          paramName,
          new ParameterDescriptor(
              paramName,
              paramType
          )
      );
    return null;
  }

  public Void visit(RValue RValue, Scope scope) {
    return null;
  }

  public Void visit(
      LocationVariable locationVariable,
      Scope scope
  ) {
    if (scope
        .lookup(locationVariable.RValue.getLabel())
        .isEmpty()) {
      errors.add(new SemanticError(
          locationVariable.RValue.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Location " + locationVariable.RValue.getLabel() + " must be defined"
      ));
    }
    return null;
  }

  public Void visit(Len len, Scope scope) {
    String arrayName = len.RValueId.getLabel();
    Optional<Descriptor> descriptor =
        scope.lookup(arrayName);
    if (descriptor.isEmpty()) {
      errors.add(new SemanticError(
          len.RValueId.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          arrayName + " not in scope"
      ));
    } else if (!(descriptor.get() instanceof ArrayDescriptor)) {
      errors.add(new SemanticError(
          len.RValueId.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "the argument of the len operator must be an array"
      ));
    }
    return null;
  }

  public Void visit(Increment increment, Scope scope) {
    return null;
  }

  public Void visit(Decrement decrement, Scope scope) {
    return null;
  }

  public Void visit(CharLiteral charLiteral, Scope scope) {
    return null;
  }

  public Void visit(StringLiteral stringLiteral, Scope scope) {
    return null;
  }

  public TreeSet<String> getImports() {
    return imports;
  }

  public Scope getFields() {
    return fields;
  }

  public Scope getMethods() {
    return methods;
  }
}

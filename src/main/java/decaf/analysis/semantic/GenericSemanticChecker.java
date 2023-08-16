package decaf.analysis.semantic;

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
import decaf.analysis.syntax.ast.Name;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.Program;
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
import decaf.shared.errors.SemanticError;
import decaf.shared.symboltable.SymbolTable;
import decaf.shared.symboltable.SymbolTableType;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class GenericSemanticChecker implements AstVisitor<Void> {
  int depth = 0; // the number of nested while/for loops we are in
  private TreeSet<String> imports = new TreeSet<>();
  private final List<SemanticError> errors;
  private SymbolTable fields =
      new SymbolTable(null, SymbolTableType.Field, null);
  private SymbolTable methods =
      new SymbolTable(null, SymbolTableType.Method, null);

  public GenericSemanticChecker(List<SemanticError> errors) {
    this.errors = errors;
  }

  public Void visit(IntLiteral intLiteral, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(FieldDeclaration fieldDeclaration,
                    SymbolTable symbolTable) {
    Type type = fieldDeclaration.getType();
    for (Name name : fieldDeclaration.names) {
      if (symbolTable.isShadowingParameter(name.getLabel())) {
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_PARAMETER,
            "Field " + name.getLabel() + " shadows a parameter"));
      } else if (symbolTable.parent == null &&
                 (getMethods().containsEntry(name.getLabel()) ||
                  getImports().contains(name.getLabel()) ||
                  getFields().containsEntry(name.getLabel()))) {
        // global field already declared in global scope
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
            "(global) Field " + name.getLabel() + " already declared"));
      } else if (symbolTable.containsEntry(name.getLabel())) {
        // field already declared in scope
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
            "Field " + name.getLabel() + " already declared"));
      } else {
        // fields just declared do not have a value.
        symbolTable.entries.put(
            name.getLabel(),
            new VariableDescriptor(name.getLabel(), type, name));
      }
    }

    for (Array array : fieldDeclaration.arrays) {
      var arrayIdLabel = array.getId().getLabel();
      if (symbolTable.isShadowingParameter(array.getId().getLabel())) {
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.SHADOWING_PARAMETER,
            "Field " + arrayIdLabel + " shadows a parameter"));
      } else if (symbolTable
                     .getDescriptorFromValidScopes(array.getId().getLabel())
                     .isPresent()) {
        errors.add(new SemanticError(
            fieldDeclaration.tokenPosition,
            SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
            "Field " + arrayIdLabel + " already declared"));
      } else {
        // TODO: Check hex parse long
        symbolTable.entries.put(
            arrayIdLabel,
            new ArrayDescriptor(arrayIdLabel, array.getSize().convertToLong(),
                                type == Type.Int      ? Type.IntArray
                                : (type == Type.Bool) ? Type.BoolArray
                                                      : Type.Undefined,
                                array));
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
          Utils.coloredPrint(methodDefinitionParameter.getType().toString(),
                             Utils.ANSIColorConstants.ANSI_PURPLE));
      stringBuilder.append(" ");
      stringBuilder.append(
          Utils.coloredPrint(methodDefinitionParameter.getName(),
                             Utils.ANSIColorConstants.ANSI_WHITE));
      stringBuilder.append(", ");
    }
    stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
    return stringBuilder.toString();
  }

  private void
  checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
    for (MethodDefinition methodDefinition : methodDefinitionList) {
      if (methodDefinition.getMethodName().getLabel().equals("main")) {
        if (methodDefinition.getReturnType() == Type.Void) {
          if (!(methodDefinition.getParameterList().isEmpty())) {
            errors.add(new SemanticError(
                methodDefinition.getTokenPosition(),
                SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
                "main method must have no parameters, yours has: " +
                    simplify(methodDefinition.getParameterList())));
          }
        } else {
          errors.add(new SemanticError(
              methodDefinition.getTokenPosition(),
              SemanticError.SemanticErrorType.INVALID_MAIN_METHOD,
              "main method return type must be void, not `" +
                  methodDefinition.getReturnType() + "`"));
        }
        return;
      }
    }
    errors.add(
        new SemanticError(new TokenPosition(0, 0, 0),
                          SemanticError.SemanticErrorType.MISSING_MAIN_METHOD,
                          "main method not found"));
  }

  public Void visit(MethodDefinition methodDefinition,
                    SymbolTable symbolTable) {
    SymbolTable parameterSymbolTable = new SymbolTable(
        getFields(), SymbolTableType.Parameter, methodDefinition.getBlock());
    SymbolTable localSymbolTable =
        new SymbolTable(parameterSymbolTable, SymbolTableType.Field,
                        methodDefinition.getBlock());
    if (getMethods().containsEntry(
            methodDefinition.getMethodName().getLabel()) ||
        getImports().contains(methodDefinition.getMethodName().getLabel()) ||
        getFields().containsEntry(
            methodDefinition.getMethodName().getLabel())) {
      // method already defined. add an exception
      errors.add(new SemanticError(
          methodDefinition.getTokenPosition(),
          SemanticError.SemanticErrorType.METHOD_ALREADY_DEFINED,
          String.format("Method ``%s`` already defined",
                        methodDefinition.getMethodName().getLabel())));
      methodDefinition.getBlock().blockSymbolTable = localSymbolTable;
    } else {
      for (var parameter : methodDefinition.getParameterList()) {
        parameter.accept(this, parameterSymbolTable);
      }
      // visit the method definition and populate the local symbol table
      getMethods().entries.put(methodDefinition.getMethodName().getLabel(),
                               new MethodDescriptor(methodDefinition,
                                                    parameterSymbolTable,
                                                    localSymbolTable));
      methodDefinition.getBlock().accept(this, localSymbolTable);
    }
    return null;
  }

  public Void visit(ImportDeclaration importDeclaration,
                    SymbolTable symbolTable) {
    if (symbolTable.isShadowingParameter(importDeclaration.nameId.getLabel())) {
      errors.add(new SemanticError(
          importDeclaration.nameId.tokenPosition,
          SemanticError.SemanticErrorType.SHADOWING_PARAMETER,
          "Import identifier " + importDeclaration.nameId.getLabel() +
              " shadows a parameter"));
    } else if (getImports().contains(importDeclaration.nameId.getLabel())) {
      errors.add(new SemanticError(
          new TokenPosition(0, 0, 0),
          SemanticError.SemanticErrorType.IDENTIFIER_ALREADY_DECLARED,
          "Import identifier " + importDeclaration.nameId.getLabel() +
              " already declared"));
    } else {
      getImports().add(importDeclaration.nameId.getLabel());
    }
    return null;
  }

  public Void visit(For forStatement, SymbolTable symbolTable) {
    // this is the name of our loop irAssignableValue that we initialize in the
    // creation of the for loop for ( index = 0 ...) <-- index is the example
    // here
    String initializedVariableName =
        forStatement.initialization.initLocation.getLabel();

    if (symbolTable.getDescriptorFromValidScopes(initializedVariableName)
            .isPresent()) {
      ++depth;
      forStatement.block.accept(this, symbolTable);
      --depth;
    } else {
      // the irAssignableValue referred to was not declared. Add an exception.
      errors.add(new SemanticError(
          forStatement.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Variable " + initializedVariableName + " was not declared"));
    }
    return null;
  }

  public Void visit(Break breakStatement, SymbolTable symbolTable) {
    if (depth < 1) {
      errors.add(new SemanticError(
          breakStatement.tokenPosition,
          SemanticError.SemanticErrorType.BREAK_STATEMENT_NOT_ENCLOSED,
          String.format(
              "break statement not enclosed; it is at a perceived depth of %s",
              depth)));
    }
    return null;
  }

  public Void visit(Continue continueStatement, SymbolTable symbolTable) {
    if (depth < 1) {
      errors.add(new SemanticError(
          continueStatement.tokenPosition,
          SemanticError.SemanticErrorType.CONTINUE_STATEMENT_NOT_ENCLOSED,
          String.format(
              "continue statement not enclosed; it is at a perceived depth of %s",
              depth)));
    }
    return null;
  }

  public Void visit(While whileStatement, SymbolTable symbolTable) {
    whileStatement.test.accept(this, symbolTable);
    ++depth;
    whileStatement.body.accept(this, symbolTable);
    --depth;
    return null;
  }

  public Void visit(Program program, SymbolTable symbolTable) {
    checkMainMethodExists(program.getMethodDefinitionList());
    for (ImportDeclaration importDeclaration :
         program.getImportDeclarationList())
      getImports().add(importDeclaration.nameId.getLabel());
    for (FieldDeclaration fieldDeclaration : program.getFieldDeclarationList())
      fieldDeclaration.accept(this, getFields());
    for (MethodDefinition methodDefinition : program.getMethodDefinitionList())
      methodDefinition.accept(this, getMethods());
    return null;
  }

  public Void visit(UnaryOpExpression unaryOpExpression,
                    SymbolTable symbolTable) {
    return null;
  }

  public Void visit(BinaryOpExpression binaryOpExpression,
                    SymbolTable symbolTable) {
    return null;
  }

  public Void visit(Block block, SymbolTable symbolTable) {
    SymbolTable blockST =
        new SymbolTable(symbolTable, SymbolTableType.Field, block);
    block.blockSymbolTable = blockST;
    symbolTable.children.add(blockST);
    for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
      fieldDeclaration.accept(this, block.blockSymbolTable);
    for (Statement statement : block.statementList)
      statement.accept(this, block.blockSymbolTable);
    return null;
  }

  public Void visit(ParenthesizedExpression parenthesizedExpression,
                    SymbolTable symbolTable) {
    return null;
  }

  public Void visit(LocationArray locationArray, SymbolTable symbolTable) {
    final Optional<Descriptor> optionalDescriptor =
        symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
    if (optionalDescriptor.isEmpty())
      errors.add(new SemanticError(
          locationArray.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Array " + locationArray.name.getLabel() + " not declared"));
    else if (!(optionalDescriptor.get() instanceof ArrayDescriptor))
      errors.add(new SemanticError(
          locationArray.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          locationArray.name.getLabel() + " is not an array"));
    locationArray.expression.accept(this, symbolTable);
    return null;
  }

  public Void visit(CompoundAssignOpExpr compoundAssignOpExpr,
                    SymbolTable symbolTable) {
    compoundAssignOpExpr.expression.accept(this, symbolTable);
    return null;
  }

  @Override
  public Void visit(Initialization initialization, SymbolTable symbolTable) {
    return null;
  }

  @Override
  public Void visit(Assignment assignment, SymbolTable symbolTable) {
    return null;
  }

  @Override
  public Void visit(VoidExpression voidExpression, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(ExpressionParameter expressionParameter,
                    SymbolTable symbolTable) {
    return null;
  }

  public Void visit(If ifStatement, SymbolTable symbolTable) {
    // irAssignableValue lookup happens in ifCondition or if/else body
    for (Pair<String, AST> child : ifStatement.getChildren())
      child.second().accept(this, symbolTable);
    return null;
  }

  public Void visit(Return returnStatement, SymbolTable symbolTable) {
    if (!returnStatement.isTerminal())
      returnStatement.retExpression.accept(this, symbolTable);

    return null;
  }

  public Void visit(Array array, SymbolTable symbolTable) {
    if (Integer.parseInt(array.getSize().literal) <= 0) {
      errors.add(
          new SemanticError(array.getSize().tokenPosition,
                            SemanticError.SemanticErrorType.INVALID_ARRAY_SIZE,
                            "array declaration size " + array.getId() + "[" +
                                array.getSize().literal + "]"
                                + " must be greater than 0"));
    }
    return null;
  }

  public Void visit(MethodCall methodCall, SymbolTable symbolTable) {
    Name methodName = methodCall.nameId;

    if (getMethods()
            .getDescriptorFromValidScopes(methodName.getLabel())
            .isEmpty() &&
        !getImports().contains(methodName.getLabel()))
      errors.add(new SemanticError(
          methodName.tokenPosition,
          SemanticError.SemanticErrorType.METHOD_DEFINITION_NOT_FOUND,
          "Method name " + methodName.getLabel() + " hasn't been defined yet"));

    for (MethodCallParameter parameter : methodCall.methodCallParameterList) {
      // TODO: partial rule 7 - array checking will be done in second pass
      if (!getImports().contains(methodName.getLabel()) && parameter instanceof
                                                               StringLiteral)
        errors.add(new SemanticError(
            methodName.tokenPosition,
            SemanticError.SemanticErrorType.INVALID_ARGUMENT_TYPE,
            "String " + parameter +
                " cannot be arguments to non-import methods"));
      parameter.accept(this, symbolTable);
    }
    return null;
  }

  public Void visit(MethodCallStatement methodCallStatement,
                    SymbolTable symbolTable) {
    methodCallStatement.methodCall.accept(this, symbolTable);
    return null;
  }

  public Void visit(LocationAssignExpr locationAssignExpr,
                    SymbolTable symbolTable) {
    locationAssignExpr.location.accept(this, symbolTable);
    return locationAssignExpr.assignExpr.accept(this, symbolTable);
  }

  public Void visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
    // no node for AssignOperator?
    assignOpExpr.expression.accept(this, symbolTable);
    return null;
  }

  public Void visit(MethodDefinitionParameter methodDefinitionParameter,
                    SymbolTable symbolTable) {
    String paramName = methodDefinitionParameter.getName();
    Type paramType = methodDefinitionParameter.getType();
    if (symbolTable.isShadowingParameter(paramName))
      errors.add(
          new SemanticError(methodDefinitionParameter.tokenPosition,
                            SemanticError.SemanticErrorType.SHADOWING_PARAMETER,
                            "MethodDefinitionParameter " + paramName +
                                " is shadowing a parameter"));
    else
      symbolTable.entries.put(paramName,
                              new ParameterDescriptor(paramName, paramType));
    return null;
  }

  public Void visit(Name name, SymbolTable symbolTable) { return null; }

  public Void visit(LocationVariable locationVariable,
                    SymbolTable symbolTable) {
    if (symbolTable
            .getDescriptorFromValidScopes(locationVariable.name.getLabel())
            .isEmpty()) {
      errors.add(new SemanticError(
          locationVariable.name.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          "Location " + locationVariable.name.getLabel() + " must be defined"));
    }
    return null;
  }

  public Void visit(Len len, SymbolTable symbolTable) {
    String arrayName = len.nameId.getLabel();
    Optional<Descriptor> descriptor =
        symbolTable.getDescriptorFromValidScopes(arrayName);
    if (descriptor.isEmpty()) {
      errors.add(new SemanticError(
          len.nameId.tokenPosition,
          SemanticError.SemanticErrorType.IDENTIFIER_NOT_IN_SCOPE,
          arrayName + " not in scope"));
    } else if (!(descriptor.get() instanceof ArrayDescriptor)) {
      errors.add(new SemanticError(
          len.nameId.tokenPosition,
          SemanticError.SemanticErrorType.UNSUPPORTED_TYPE,
          "the argument of the len operator must be an array"));
    }
    return null;
  }

  public Void visit(Increment increment, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(Decrement decrement, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(CharLiteral charLiteral, SymbolTable symbolTable) {
    return null;
  }

  public Void visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
    return null;
  }

  public TreeSet<String> getImports() { return imports; }

  public void setImports(TreeSet<String> imports) { this.imports = imports; }

  public SymbolTable getFields() { return fields; }

  public void setFields(SymbolTable fields) { this.fields = fields; }

  public SymbolTable getMethods() { return methods; }

  public void setMethods(SymbolTable methods) { this.methods = methods; }
}

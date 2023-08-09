package decaf.ir;


import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import decaf.ast.AST;
import decaf.ast.Array;
import decaf.ast.AssignOpExpr;
import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.Break;
import decaf.ast.CharLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.Continue;
import decaf.ast.DecimalLiteral;
import decaf.ast.Decrement;
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
import decaf.ast.Return;
import decaf.ast.Statement;
import decaf.ast.StringLiteral;
import decaf.ast.Type;
import decaf.ast.UnaryOpExpression;
import decaf.ast.While;
import decaf.common.Pair;
import decaf.common.Utils;
import decaf.descriptors.ArrayDescriptor;
import decaf.descriptors.Descriptor;
import decaf.descriptors.MethodDescriptor;
import decaf.descriptors.ParameterDescriptor;
import decaf.descriptors.VariableDescriptor;
import decaf.exceptions.DecafSemanticException;
import decaf.grammar.TokenPosition;
import decaf.symboltable.SymbolTable;
import decaf.symboltable.SymbolTableType;

public class GenericSemanticChecker implements AstVisitor<Void> {
  int depth = 0; // the number of nested while/for loops we are in
  private TreeSet<String> imports = new TreeSet<>();
  private SymbolTable fields = new SymbolTable(
      null,
      SymbolTableType.Field,
      null
  );
  private SymbolTable methods = new SymbolTable(
      null,
      SymbolTableType.Method,
      null
  );

  public Void visit(
      IntLiteral intLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      BooleanLiteral booleanLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      DecimalLiteral decimalLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      HexLiteral hexLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      FieldDeclaration fieldDeclaration,
      SymbolTable symbolTable
  ) {
    Type type = fieldDeclaration.getType();
    for (Name name : fieldDeclaration.names) {
      if (symbolTable.isShadowingParameter(name.getLabel())) {
        exceptions.add(new DecafSemanticException(
            fieldDeclaration.tokenPosition,
            "Field " + name.getLabel() + " shadows a parameter"
        ));
      } else if (symbolTable.parent == null && (
          getMethods().containsEntry(name.getLabel()) || getImports().contains(name.getLabel()) ||
              getFields().containsEntry(name.getLabel()))) {
        // global field already declared in global scope
        exceptions.add(new DecafSemanticException(
            fieldDeclaration.tokenPosition,
            "(global) Field " + name.getLabel() + " already declared"
        ));
      } else if (symbolTable.containsEntry(name.getLabel())) {
        // field already declared in scope
        exceptions.add(new DecafSemanticException(
            fieldDeclaration.tokenPosition,
            "Field " + name.getLabel() + " already declared"
        ));
      } else {
        // fields just declared do not have a value.
        symbolTable.entries.put(
            name.getLabel(),
            new VariableDescriptor(name.getLabel(),
                                   type,
                                   name
            )
        );
      }
    }

    for (Array array : fieldDeclaration.arrays) {
      var arrayIdLabel = array.getId()
                              .getLabel();
      if (symbolTable.isShadowingParameter(array.getId()
                                                .getLabel())) {
        exceptions.add(new DecafSemanticException(
            fieldDeclaration.tokenPosition,
            "Field " + arrayIdLabel + " shadows a parameter"
        ));
      } else if (symbolTable.getDescriptorFromValidScopes(array.getId()
                                                               .getLabel())
                            .isPresent()) {
        exceptions.add(new DecafSemanticException(
            fieldDeclaration.tokenPosition,
            "Field " + arrayIdLabel + " already declared"
        ));
      } else {
        // TODO: Check hex parse long
        symbolTable.entries.put(
            arrayIdLabel,
            new ArrayDescriptor(arrayIdLabel,
                                array.getSize()
                                     .convertToLong(),
                                type == Type.Int ? Type.IntArray: (type == Type.Bool) ? Type.BoolArray: Type.Undefined,
                                array
            )
        );
      }
    }
    return null;
  }

  private String simplify(List<MethodDefinitionParameter> methodDefinitionParameterList) {
    if (methodDefinitionParameterList.isEmpty()) {
      return "";
    }
    StringBuilder stringBuilder = new StringBuilder();
    for (MethodDefinitionParameter methodDefinitionParameter : methodDefinitionParameterList) {
      stringBuilder.append(Utils.coloredPrint(
          methodDefinitionParameter.getType()
                                   .toString(),
          Utils.ANSIColorConstants.ANSI_PURPLE
      ));
      stringBuilder.append(" ");
      stringBuilder.append(Utils.coloredPrint(
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

  private void checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
    for (MethodDefinition methodDefinition : methodDefinitionList) {
      if (methodDefinition.getMethodName()
                          .getLabel()
                          .equals("main")) {
        if (methodDefinition.getReturnType() == Type.Void) {
          if (!(methodDefinition.getParameterList()
                                .isEmpty())) {
            exceptions.add(new DecafSemanticException(
                methodDefinition.getTokenPosition(),
                "main method must have no parameters, yours has: " + simplify(methodDefinition.getParameterList())
            ));
          }
        } else {
          exceptions.add(new DecafSemanticException(
              methodDefinition.getTokenPosition(),
              "main method return type must be void, not `" +
                  methodDefinition.getReturnType() + "`"
          ));
        }
        return;
      }
    }
    exceptions.add(new DecafSemanticException(
        new TokenPosition(
            0,
            0,
            0
        ),
        "main method not found"
    ));
  }

  public Void visit(
      MethodDefinition methodDefinition,
      SymbolTable symbolTable
  ) {
    SymbolTable parameterSymbolTable = new SymbolTable(getFields(),
                                                       SymbolTableType.Parameter,
                                                       methodDefinition.getBlock()
    );
    SymbolTable localSymbolTable = new SymbolTable(parameterSymbolTable,
                                                   SymbolTableType.Field,
                                                   methodDefinition.getBlock()
    );
    if (getMethods().containsEntry(methodDefinition.getMethodName()
                                                   .getLabel()) ||
        getImports().contains(methodDefinition.getMethodName()
                                              .getLabel()) || getFields().containsEntry(methodDefinition.getMethodName()
                                                                                                        .getLabel())) {
      // method already defined. add an exception
      exceptions.add(new DecafSemanticException(
          methodDefinition.getTokenPosition(),
          "Method name " + methodDefinition.getMethodName()
                                           .getLabel() + " already defined"
      ));
      methodDefinition.getBlock().blockSymbolTable = localSymbolTable;
    } else {
      for (MethodDefinitionParameter parameter : methodDefinition.getParameterList()) {
        parameter.accept(
            this,
            parameterSymbolTable
        );
      }
      // visit the method definition and populate the local symbol table
      getMethods().entries.put(
          methodDefinition.getMethodName()
                          .getLabel(),
          new MethodDescriptor(methodDefinition,
                               parameterSymbolTable,
                               localSymbolTable
          )
      );
      methodDefinition.getBlock()
                      .accept(
                          this,
                          localSymbolTable
                      );
    }
    return null;
  }

  public Void visit(
      ImportDeclaration importDeclaration,
      SymbolTable symbolTable
  ) {
    if (symbolTable.isShadowingParameter(importDeclaration.nameId.getLabel())) {
      exceptions.add(new DecafSemanticException(
          importDeclaration.nameId.tokenPosition,
          "Import identifier " + importDeclaration.nameId.getLabel() + " shadows a parameter"
      ));
    } else if (getImports().contains(importDeclaration.nameId.getLabel())) {
      exceptions.add(new DecafSemanticException(
          new TokenPosition(
              0,
              0,
              0
          ),
          "Import identifier " + importDeclaration.nameId.getLabel() + " already declared"
      ));
    } else {
      getImports().add(importDeclaration.nameId.getLabel());
    }
    return null;
  }

  public Void visit(
      For forStatement,
      SymbolTable symbolTable
  ) {
    // this is the name of our loop irAssignableValue that we initialize in the creation of the for loop
    // for ( index = 0 ...) <-- index is the example here
    String initializedVariableName = forStatement.initialization.initLocation.getLabel();

    if (symbolTable.getDescriptorFromValidScopes(initializedVariableName)
                   .isPresent()) {
      ++depth;
      forStatement.block.accept(
          this,
          symbolTable
      );
      --depth;
    } else {
      // the irAssignableValue referred to was not declared. Add an exception.
      exceptions.add(new DecafSemanticException(
          forStatement.tokenPosition,
          "Variable " + initializedVariableName + " was not declared"
      ));
    }
    return null;
  }

  public Void visit(
      Break breakStatement,
      SymbolTable symbolTable
  ) {
    if (depth < 1) {
      exceptions.add(new DecafSemanticException(
          breakStatement.tokenPosition,
          "break statement not enclosed"
      ));
    }
    return null;
  }

  public Void visit(
      Continue continueStatement,
      SymbolTable symbolTable
  ) {
    if (depth < 1) {
      exceptions.add(new DecafSemanticException(
          continueStatement.tokenPosition,
          "continue statement not enclosed"
      ));
    }
    return null;
  }

  public Void visit(
      While whileStatement,
      SymbolTable symbolTable
  ) {
    whileStatement.test.accept(
        this,
        symbolTable
    );
    ++depth;
    whileStatement.body.accept(
        this,
        symbolTable
    );
    --depth;
    return null;
  }

  public Void visit(
      Program program,
      SymbolTable symbolTable
  ) {
    checkMainMethodExists(program.getMethodDefinitionList());
    for (ImportDeclaration importDeclaration : program.getImportDeclarationList())
      getImports().add(importDeclaration.nameId.getLabel());
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
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      BinaryOpExpression binaryOpExpression,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      Block block,
      SymbolTable symbolTable
  ) {
    SymbolTable blockST = new SymbolTable(
        symbolTable,
        SymbolTableType.Field,
        block
    );
    block.blockSymbolTable = blockST;
    symbolTable.children.add(blockST);
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
    return null;
  }

  public Void visit(
      ParenthesizedExpression parenthesizedExpression,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      LocationArray locationArray,
      SymbolTable symbolTable
  ) {
    final Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
    if (optionalDescriptor.isEmpty())
      exceptions.add(new DecafSemanticException(
          locationArray.tokenPosition,
          "Array " + locationArray.name.getLabel() + " not declared"
      ));
    else if (!(optionalDescriptor.get() instanceof ArrayDescriptor))
      exceptions.add(new DecafSemanticException(
          locationArray.tokenPosition,
          locationArray.name.getLabel() + " is not an array"
      ));
    locationArray.expression.accept(
        this,
        symbolTable
    );
    return null;
  }

  public Void visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      SymbolTable symbolTable
  ) {
    compoundAssignOpExpr.expression.accept(
        this,
        symbolTable
    );
    return null;
  }

  @Override
  public Void visit(
      Initialization initialization,
      SymbolTable symbolTable
  ) {
    return null;
  }

  @Override
  public Void visit(
      Assignment assignment,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      ExpressionParameter expressionParameter,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      If ifStatement,
      SymbolTable symbolTable
  ) {
    // irAssignableValue lookup happens in ifCondition or if/else body
    for (Pair<String, AST> child : ifStatement.getChildren())
      child.second()
           .accept(
               this,
               symbolTable
           );
    return null;
  }

  public Void visit(
      Return returnStatement,
      SymbolTable symbolTable
  ) {
    if (!returnStatement.isTerminal())
      returnStatement.retExpression.accept(
          this,
          symbolTable
      );

    return null;
  }

  public Void visit(
      Array array,
      SymbolTable symbolTable
  ) {
    if (Integer.parseInt(array.getSize().literal) <= 0) {
      exceptions.add(new DecafSemanticException(
          array.getSize().tokenPosition,
          "array declaration " + array.getId() + "[" + array.getSize().literal + "]" + " must be greater than 0"
      ));
    }
    return null;
  }

  public Void visit(
      MethodCall methodCall,
      SymbolTable symbolTable
  ) {
    Name methodName = methodCall.nameId;

    if (getMethods().getDescriptorFromValidScopes(methodName.getLabel())
                    .isEmpty() && !getImports().contains(methodName.getLabel()))
      exceptions.add(new DecafSemanticException(
          methodName.tokenPosition,
          "Method name " + methodName.getLabel() + " hasn't been defined yet"
      ));

    for (MethodCallParameter parameter : methodCall.methodCallParameterList) {
      // TODO: partial rule 7 - array checking will be done in second pass
      if (!getImports().contains(methodName.getLabel()) && parameter instanceof StringLiteral)
        exceptions.add(new DecafSemanticException(
            methodName.tokenPosition,
            "String " + parameter + " cannot be arguments to non-import methods"
        ));
      parameter.accept(
          this,
          symbolTable
      );
    }
    return null;
  }

  public Void visit(
      MethodCallStatement methodCallStatement,
      SymbolTable symbolTable
  ) {
    methodCallStatement.methodCall.accept(
        this,
        symbolTable
    );
    return null;
  }

  public Void visit(
      LocationAssignExpr locationAssignExpr,
      SymbolTable symbolTable
  ) {
    locationAssignExpr.location.accept(
        this,
        symbolTable
    );
    return locationAssignExpr.assignExpr.accept(
        this,
        symbolTable
    );
  }

  public Void visit(
      AssignOpExpr assignOpExpr,
      SymbolTable symbolTable
  ) {
    // no node for AssignOperator?
    assignOpExpr.expression.accept(
        this,
        symbolTable
    );
    return null;
  }

  public Void visit(
      MethodDefinitionParameter methodDefinitionParameter,
      SymbolTable symbolTable
  ) {
    String paramName = methodDefinitionParameter.getName();
    Type paramType = methodDefinitionParameter.getType();
    if (symbolTable.isShadowingParameter(paramName))
      exceptions.add(new DecafSemanticException(
          methodDefinitionParameter.tokenPosition,
          "MethodDefinitionParameter " + paramName + " is shadowing a parameter"
      ));
    else
      symbolTable.entries.put(
          paramName,
          new ParameterDescriptor(
              paramName,
              paramType
          )
      );
    return null;
  }

  public Void visit(
      Name name,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      LocationVariable locationVariable,
      SymbolTable symbolTable
  ) {
    if (symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel())
                   .isEmpty()) {
      exceptions.add(new DecafSemanticException(
          locationVariable.name.tokenPosition,
          "Location " + locationVariable.name.getLabel() + " must be defined"
      ));
    }
    return null;
  }

  public Void visit(
      Len len,
      SymbolTable symbolTable
  ) {
    String arrayName = len.nameId.getLabel();
    Optional<Descriptor> descriptor = symbolTable.getDescriptorFromValidScopes(arrayName);
    if (descriptor.isEmpty()) {
      exceptions.add(new DecafSemanticException(
          len.nameId.tokenPosition,
          arrayName + " not in scope"
      ));
    } else if (!(descriptor.get() instanceof ArrayDescriptor)) {
      exceptions.add(new DecafSemanticException(
          len.nameId.tokenPosition,
          "the argument of the len operator must be an array"
      ));
    }
    return null;
  }

  public Void visit(
      Increment increment,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      Decrement decrement,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      CharLiteral charLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public Void visit(
      StringLiteral stringLiteral,
      SymbolTable symbolTable
  ) {
    return null;
  }

  public TreeSet<String> getImports() {
    return imports;
  }

  public void setImports(TreeSet<String> imports) {
    this.imports = imports;
  }

  public SymbolTable getFields() {
    return fields;
  }

  public void setFields(SymbolTable fields) {
    this.fields = fields;
  }

  public SymbolTable getMethods() {
    return methods;
  }

  public void setMethods(SymbolTable methods) {
    this.methods = methods;
  }
}

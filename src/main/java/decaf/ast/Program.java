package decaf.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class Program extends AST {
  private final List<ImportDeclaration> importDeclarationList;
  private final List<FieldDeclaration> fieldDeclarationList;
  private final List<MethodDefinition> methodDefinitionList;

  public Program() {
    this.importDeclarationList = new ArrayList<>();
    this.fieldDeclarationList = new ArrayList<>();
    this.methodDefinitionList = new ArrayList<>();
  }

  @Override
  public Type getType() {
    return Type.Undefined;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeList = new ArrayList<>();
    for (ImportDeclaration importDeclaration : getImportDeclarationList())
      nodeList.add(new Pair<>(
          "import",
          importDeclaration
      ));
    for (FieldDeclaration fieldDeclaration : getFieldDeclarationList())
      nodeList.add(new Pair<>(
          "field",
          fieldDeclaration
      ));
    for (MethodDefinition methodDefinition : getMethodDefinitionList())
      nodeList.add(new Pair<>(
          "method",
          methodDefinition
      ));
    return nodeList;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "Program{"
        + "importDeclarationList="
        + getImportDeclarationList()
        + ", fieldDeclarationList="
        + getFieldDeclarationList()
        + ", methodDefinitionList="
        + getMethodDefinitionList()
        + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> stringList = new ArrayList<>();
    for (ImportDeclaration importDeclaration : getImportDeclarationList()) {
      String sourceCode = importDeclaration.getSourceCode();
      stringList.add(sourceCode);
    }
    String imports = String.join(
        ";\n",
        stringList
    );

    List<String> list = new ArrayList<>();
    for (FieldDeclaration fieldDeclaration : getFieldDeclarationList()) {
      String sourceCode = fieldDeclaration.getSourceCode();
      list.add(sourceCode);
    }
    String fieldDeclarations = String.join(
        ";\n",
        list
    );
    List<String> result = new ArrayList<>();
    for (MethodDefinition methodDefinition : getMethodDefinitionList()) {
      String sourceCode = methodDefinition.getSourceCode();
      result.add(sourceCode);
    }
    String methodDefinitions = String.join(
        ";\n\n",
        result
    );
    if (imports.isBlank() && fieldDeclarations.isBlank()) {
      return methodDefinitions;
    } else if (imports.isBlank()) {
      return String.join(
          ";\n\n\n",
          List.of(
              fieldDeclarations,
              methodDefinitions
          )
      );
    } else if (fieldDeclarations.isBlank()) {
      return String.join(
          ";\n\n\n",
          List.of(
              imports,
              methodDefinitions
          )
      );
    }
    return String.join(
        ";\n\n\n",
        List.of(imports,
                fieldDeclarations,
                methodDefinitions
        )
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable curSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        curSymbolTable
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }

  public List<ImportDeclaration> getImportDeclarationList() {
    return importDeclarationList;
  }

  public List<FieldDeclaration> getFieldDeclarationList() {
    return fieldDeclarationList;
  }

  public List<MethodDefinition> getMethodDefinitionList() {
    return methodDefinitionList;
  }
}

package decaf.analysis.syntax.ast;


import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import decaf.shared.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Program extends AST {
  @NotNull
  private final List<ImportDeclaration> importDeclarationList;
  @NotNull
  private final List<FieldDeclaration> fieldDeclarationList;
  @NotNull
  private final List<MethodDefinition> methodDefinitionList;

  public Program(
      @NotNull List<ImportDeclaration> importDeclarationList,
      @NotNull List<FieldDeclaration> fieldDeclarationList,
      @NotNull List<MethodDefinition> methodDefinitionList
  ) {
    this.importDeclarationList = importDeclarationList;
    this.fieldDeclarationList = fieldDeclarationList;
    this.methodDefinitionList = methodDefinitionList;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeList = new ArrayList<>();
    for (ImportDeclaration importDeclaration : getImportDeclaration())
      nodeList.add(new Pair<>(
          "import",
          importDeclaration
      ));
    for (FieldDeclaration fieldDeclaration : getFieldDeclaration())
      nodeList.add(new Pair<>(
          "field",
          fieldDeclaration
      ));
    for (MethodDefinition methodDefinition : getMethodDefinitions())
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
        + getImportDeclaration()
        + ", fieldDeclarationList="
        + getFieldDeclaration()
        + ", methodDefinitionList="
        + getMethodDefinitions()
        + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> stringList = new ArrayList<>();
    for (ImportDeclaration importDeclaration : getImportDeclaration()) {
      String sourceCode = importDeclaration.getSourceCode();
      stringList.add(sourceCode);
    }
    String imports = String.join(
        ";\n",
        stringList
    );

    List<String> list = new ArrayList<>();
    for (FieldDeclaration fieldDeclaration : getFieldDeclaration()) {
      String sourceCode = fieldDeclaration.getSourceCode();
      list.add(sourceCode);
    }
    String fieldDeclarations = String.join(
        ";\n",
        list
    );
    List<String> result = new ArrayList<>();
    for (MethodDefinition methodDefinition : getMethodDefinitions()) {
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
        List.of(
            imports,
            fieldDeclarations,
            methodDefinitions
        )
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }

  public List<ImportDeclaration> getImportDeclaration() {
    return importDeclarationList;
  }

  public List<FieldDeclaration> getFieldDeclaration() {
    return fieldDeclarationList;
  }

  public List<MethodDefinition> getMethodDefinitions() {
    return methodDefinitionList;
  }
}

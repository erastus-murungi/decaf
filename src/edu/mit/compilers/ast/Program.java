package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class Program extends AST {
  public final List<ImportDeclaration> importDeclarationList;
  public final List<FieldDeclaration> fieldDeclarationList;
  public final List<MethodDefinition> methodDefinitionList;

  public Program() {
    this.importDeclarationList = new ArrayList<>();
    this.fieldDeclarationList = new ArrayList<>();
    this.methodDefinitionList = new ArrayList<>();
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeList = new ArrayList<>();
    for (ImportDeclaration importDeclaration : importDeclarationList)
      nodeList.add(new Pair<>("import", importDeclaration));
    for (FieldDeclaration fieldDeclaration : fieldDeclarationList)
      nodeList.add(new Pair<>("field", fieldDeclaration));
    for (MethodDefinition methodDefinition : methodDefinitionList)
      nodeList.add(new Pair<>("method", methodDefinition));
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
        + importDeclarationList
        + ", fieldDeclarationList="
        + fieldDeclarationList
        + ", methodDefinitionList="
        + methodDefinitionList
        + '}';
  }

  @Override
  public String getSourceCode() {
    String imports = String.join(";\n", importDeclarationList.stream().map(ImportDeclaration::getSourceCode).toList());
    String fieldDeclarations = String.join(";\n", fieldDeclarationList.stream().map(FieldDeclaration::getSourceCode).toList());
    String methodDefinitions = String.join(";\n\n", methodDefinitionList.stream().map(MethodDefinition::getSourceCode).toList());
    if (imports.isBlank() && fieldDeclarations.isBlank()) {
      return methodDefinitions;
    } else if (imports.isBlank()){
      return String.join(";\n\n\n", List.of(fieldDeclarations, methodDefinitions));
    } else if (fieldDeclarations.isBlank()) {
      return String.join(";\n\n\n", List.of(imports, methodDefinitions));
    }
    return String.join(";\n\n\n", List.of(imports, fieldDeclarations, methodDefinitions));
  }

  @Override
  public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
    return visitor.visit(this, curSymbolTable);
  }
}

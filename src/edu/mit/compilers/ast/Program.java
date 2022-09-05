package edu.mit.compilers.ast;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

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
    public Type getType() {
        return Type.Undefined;
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
        List<String> stringList = new ArrayList<>();
        for (ImportDeclaration importDeclaration : importDeclarationList) {
            String sourceCode = importDeclaration.getSourceCode();
            stringList.add(sourceCode);
        }
        String imports = String.join(";\n", stringList);

        List<String> list = new ArrayList<>();
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            String sourceCode = fieldDeclaration.getSourceCode();
            list.add(sourceCode);
        }
        String fieldDeclarations = String.join(";\n", list);
        List<String> result = new ArrayList<>();
        for (MethodDefinition methodDefinition : methodDefinitionList) {
            String sourceCode = methodDefinition.getSourceCode();
            result.add(sourceCode);
        }
        String methodDefinitions = String.join(";\n\n", result);
        if (imports.isBlank() && fieldDeclarations.isBlank()) {
            return methodDefinitions;
        } else if (imports.isBlank()) {
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

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}

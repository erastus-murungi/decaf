package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodDefinition extends AST {
    final public TokenPosition tokenPosition;
    final public BuiltinType returnType;
    final public List<MethodDefinitionParameter> methodDefinitionParameterList;
    final public Name methodName;
    final public Block block;

    public MethodDefinition(TokenPosition tokenPosition, BuiltinType returnType, List<MethodDefinitionParameter> methodDefinitionParameterList, Name methodName, Block block) {
        this.tokenPosition = tokenPosition;
        this.returnType = returnType;
        this.methodDefinitionParameterList = methodDefinitionParameterList;
        this.methodName = methodName;
        this.block = block;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        List<Pair<String, AST>> nodes = new ArrayList<>();
        nodes.add(new Pair<>("returnType", new Name(returnType.toString(), tokenPosition, ExprContext.DECLARE)));
        nodes.add(new Pair<>("methodName", methodName));
        for (MethodDefinitionParameter methodDefinitionParameter : methodDefinitionParameterList) {
            nodes.add(new Pair<>("arg", methodDefinitionParameter));
        }
        nodes.add(new Pair<>("block", block));

        return nodes;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "MethodDefinition{"
                + "returnType="
                + returnType
                + ", methodDeclarationArgList="
                + methodDefinitionParameterList
                + ", methodName="
                + methodName
                + ", block="
                + block
                + '}';
    }

    @Override
  public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
  }
}

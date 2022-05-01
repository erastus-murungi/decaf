package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodCall extends Expression {
    final public Name nameId;
    final public List<MethodCallParameter> methodCallParameterList;

    public boolean isImported = false;

    public MethodCall(Name nameId, List<MethodCallParameter> methodCallParameterList) {
        super(nameId.tokenPosition);
        this.nameId = nameId;
        this.methodCallParameterList = methodCallParameterList;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
        nodeArrayList.add(new Pair<>("methodName", nameId));
        for (MethodCallParameter methodCallParameter : methodCallParameterList)
            nodeArrayList.add(new Pair<>("arg", methodCallParameter));
        return nodeArrayList;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "MethodCall{" + "nameId=" + nameId + ", methodCallParameterList=" + methodCallParameterList + '}';
    }

    @Override
    public String getSourceCode() {
        List<String> stringList = new ArrayList<>();
        for (MethodCallParameter methodCallParameter : methodCallParameterList) {
            String sourceCode = methodCallParameter.getSourceCode();
            stringList.add(sourceCode);
        }
        return String.format("%s(%s)",
                nameId.getSourceCode(),
                String.join(", ", stringList));
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}

package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodCall extends Expression {
    final Name nameId;
    final List<MethodCallParameter> methodCallParameterList;

    public MethodCall(Name nameId, List<MethodCallParameter> methodCallParameterList) {
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
}

package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodCall extends Expr {
    final Name nameId;
    final List<MethodCallArg> methodCallArgList;

    public MethodCall(Name nameId, List<MethodCallArg> methodCallArgList) {
        this.nameId = nameId;
        this.methodCallArgList = methodCallArgList;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        ArrayList<Pair<String, Node>> nodeArrayList = new ArrayList<>();
        nodeArrayList.add(new Pair<>("methodName", nameId));
        for (MethodCallArg methodCallArg: methodCallArgList)
            nodeArrayList.add(new Pair<>("arg", methodCallArg));
        return nodeArrayList;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "MethodCall{" + "nameId=" + nameId + ", methodCallArgList=" + methodCallArgList + '}';
    }
}

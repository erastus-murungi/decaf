package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodDefinition extends Node {
  final BuiltinType returnType;
  final List<MethodDeclarationArg> methodDeclarationArgList;
  final Name methodName;
  final Block block;

  public MethodDefinition(
      BuiltinType returnType,
      List<MethodDeclarationArg> methodDeclarationArgList,
      Name methodName,
      Block block) {
    this.returnType = returnType;
    this.methodDeclarationArgList = methodDeclarationArgList;
    this.methodName = methodName;
    this.block = block;
  }

  @Override
  public List<Pair<String, Node>> getChildren() {
    List<Pair<String, Node>> nodes = new ArrayList<>();
    nodes.add(new Pair<>("returnType", returnType));
    nodes.add(new Pair<>("methodName", methodName));
      for (MethodDeclarationArg methodDeclarationArg : methodDeclarationArgList) {
          nodes.add(new Pair<>("arg", methodDeclarationArg));
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
        + methodDeclarationArgList
        + ", methodName="
        + methodName
        + ", block="
        + block
        + '}';
  }
}

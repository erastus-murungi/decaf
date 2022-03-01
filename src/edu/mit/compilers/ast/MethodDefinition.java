package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodDefinition extends AST {
  final BuiltinType returnType;
  final List<MethodDefinitionParameter> methodDefinitionParameterList;
  final Name methodName;
  final Block block;

  public MethodDefinition(
      BuiltinType returnType,
      List<MethodDefinitionParameter> methodDefinitionParameterList,
      Name methodName,
      Block block) {
    this.returnType = returnType;
    this.methodDefinitionParameterList = methodDefinitionParameterList;
    this.methodName = methodName;
    this.block = block;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    List<Pair<String, AST>> nodes = new ArrayList<>();
    nodes.add(new Pair<>("returnType", returnType));
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
}

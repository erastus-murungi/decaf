package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodCall extends Expression {
  final public RValue methodId;
  final public List<ActualArgument> actualArgumentList;

  public boolean isImported = false;

  public MethodCall(
      RValue methodId,
      List<ActualArgument> actualArgumentList
  ) {
    super(methodId.tokenPosition);
    this.methodId = methodId;
    this.actualArgumentList = actualArgumentList;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
    nodeArrayList.add(new Pair<>(
        "methodName",
        methodId
    ));
    for (ActualArgument actualArgument : actualArgumentList)
      nodeArrayList.add(new Pair<>(
          "arg",
          actualArgument
      ));
    return nodeArrayList;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "MethodCall{" + "nameId=" + methodId + ", methodCallParameterList=" + actualArgumentList + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> stringList = new ArrayList<>();
    for (ActualArgument actualArgument : actualArgumentList) {
      String sourceCode = actualArgument.getSourceCode();
      stringList.add(sourceCode);
    }
    return String.format(
            "%s(%s)",
            methodId.getSourceCode(),
            String.join(
            ", ",
            stringList
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
}

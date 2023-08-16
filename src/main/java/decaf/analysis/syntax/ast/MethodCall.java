package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodCall extends Expression {
  final public RValue RValueId;
  final public List<MethodCallParameter> methodCallParameterList;

  public boolean isImported = false;

  public MethodCall(
      RValue RValueId,
      List<MethodCallParameter> methodCallParameterList
  ) {
    super(RValueId.tokenPosition);
    this.RValueId = RValueId;
    this.methodCallParameterList = methodCallParameterList;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
    nodeArrayList.add(new Pair<>(
        "methodName",
        RValueId
    ));
    for (MethodCallParameter methodCallParameter : methodCallParameterList)
      nodeArrayList.add(new Pair<>(
          "arg",
          methodCallParameter
      ));
    return nodeArrayList;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "MethodCall{" + "nameId=" + RValueId + ", methodCallParameterList=" + methodCallParameterList + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> stringList = new ArrayList<>();
    for (MethodCallParameter methodCallParameter : methodCallParameterList) {
      String sourceCode = methodCallParameter.getSourceCode();
      stringList.add(sourceCode);
    }
    return String.format(
        "%s(%s)",
        RValueId.getSourceCode(),
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

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
  }
}

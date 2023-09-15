package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodCallStatement extends Statement {
  public final MethodCall methodCall;

  public MethodCallStatement(
      TokenPosition tokenPosition,
      MethodCall methodCall
  ) {
    super(tokenPosition);
    this.methodCall = methodCall;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return methodCall.getChildren();
  }

  @Override
  public String toString() {
    return methodCall.toString();
  }

  @Override
  public String getSourceCode() {
    return methodCall.getSourceCode();
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }
}

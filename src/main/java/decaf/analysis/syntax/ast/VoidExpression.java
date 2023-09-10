package decaf.analysis.syntax.ast;

import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class VoidExpression extends Expression {
  public VoidExpression(TokenPosition tokenPosition) {
    super(tokenPosition);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope currentScope
  ) {
    return astVisitor.visit(
        this,
        currentScope
    );
  }

  @Override
  public String getSourceCode() {
    return "";
  }
}

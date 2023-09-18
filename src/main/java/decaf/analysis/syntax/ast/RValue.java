package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;


public class RValue extends AST {
  public final TokenPosition tokenPosition;
  private String label;

  public RValue(
          TokenPosition tokenPosition,
      String label
  ) {
      super(tokenPosition);
      this.label = label;
    this.tokenPosition = tokenPosition;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return "Name{" + "label='" + label + '\'' + '}';
  }

  @Override
  public String getSourceCode() {
    return label;
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
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }
}

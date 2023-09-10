package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Array extends AST {
  public final TokenPosition tokenPosition;
  private final IntLiteral size;
  private final String label;

  public Array(
      TokenPosition tokenPosition,
      IntLiteral size,
      String label
  ) {
    this.tokenPosition = tokenPosition;
    this.size = size;
    this.label = label;
  }

  public IntLiteral getSize() {
    return size;
  }

  public String getLabel() {
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
  public String toString() {
    return "Array{" + "size=" + size + ", id=" + label + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s[%s]",
        label,
        size.literal
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

package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;

public class StringLiteral extends ActualArgument {
  final public String literal;
  public StringLiteral(
      TokenPosition tokenPosition,
      String literal
  ) {
    super(tokenPosition);
    this.literal = literal;
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
    return "StringLiteral{" + literal + "}";
  }

  @Override
  public String getSourceCode() {
    return literal;
  }

  public @NotNull Type getType() {
    return Type.getStringType();
  }

  @Override
  public void setType(@NotNull Type type) {
    throw new UnsupportedOperationException("no need to set the type of a literal");
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

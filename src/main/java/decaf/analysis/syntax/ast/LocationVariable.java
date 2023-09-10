package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class LocationVariable extends Location {
  public LocationVariable(RValue RValue) {
    super(RValue);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.singletonList(new Pair<>(
        "name",
        rValue
    ));
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

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return "LocationVariable{" + "name=" + rValue + '}';
  }

  @Override
  public String getSourceCode() {
    return rValue.getSourceCode();
  }
}

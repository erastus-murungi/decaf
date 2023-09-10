package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.Pair;


public abstract class Operator extends AST {
  final public TokenPosition tokenPosition;
  public final String label;

  public Operator(
      TokenPosition tokenPosition,
      String label
  ) {
    this.tokenPosition = tokenPosition;
    this.label = label;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  public abstract String opRep();

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return opRep() + "{}";
  }
}
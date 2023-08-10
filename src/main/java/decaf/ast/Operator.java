package decaf.ast;


import java.util.Collections;
import java.util.List;

import decaf.common.Pair;
import decaf.grammar.TokenPosition;


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

  @Override
  public Type getType() {
    return Type.Undefined;
  }
}
package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Return extends Statement {
  final Expr retExpr;

  public Return(TokenPosition tokenPosition, Expr expr) {
    super(tokenPosition);
    this.retExpr = expr;
  }

  @Override
  public List<Pair<String, Node>> getChildren() {
    return (retExpr == null) ? Collections.emptyList() : List.of(new Pair<>("return", retExpr));
  }

  @Override
  public boolean isTerminal() {
    return retExpr == null;
  }

  @Override
  public String toString() {
    return (retExpr == null) ? "Return{}" : "Return{" + "retExpr=" + retExpr + '}';
  }
}

package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class If extends Statement {
  final Expr test;
  final Block ifBlock;
  final Block elseBlock; // maybe null

  public If(TokenPosition tokenPosition, Expr test, Block ifBlock, Block elseBlock) {
    super(tokenPosition);
    this.test = test;
    this.ifBlock = ifBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public List<Pair<String, Node>> getChildren() {
    return (elseBlock != null)
        ? List.of(
            new Pair<>("test", test),
            new Pair<>("ifBody", ifBlock),
            new Pair<>("elseBody", elseBlock))
        : List.of(new Pair<>("test", test), new Pair<>("ifBody", ifBlock));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    if (elseBlock != null)
      return "If{" + "test=" + test + ", ifBlock=" + ifBlock + ", elseBlock=" + elseBlock + '}';
    else return "If{" + "test=" + test + ", ifBlock=" + ifBlock + '}';
  }
}

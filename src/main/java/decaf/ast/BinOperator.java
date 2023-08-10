package decaf.ast;


import decaf.grammar.Scanner;
import decaf.grammar.TokenPosition;

public abstract class BinOperator extends Operator {

  public BinOperator(
      TokenPosition tokenPosition,
      String op
  ) {
    super(
        tokenPosition,
        op
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    BinOperator that = (BinOperator) o;
    return toString().equals(that.toString());
  }
}

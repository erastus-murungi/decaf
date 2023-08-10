package decaf.ast;

import java.util.List;

public interface HasExpression {
  List<Expression> getExpression();

  void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  );
}

package decaf.analysis.syntax.ast;

import java.util.List;

public interface HasExpression {
  List<Expression> getExpressions();

  void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  );
}

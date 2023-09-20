package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Initialization extends Statement implements HasExpression {
  public final RValue initLocation;
  public Expression initExpression;

  public Initialization(
      RValue initLocation,
      Expression initExpression
  ) {
    super(initLocation.tokenPosition);
    this.initLocation = initLocation;
    this.initExpression = initExpression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "initLocation",
            initLocation
        ),
        new Pair<>(
            "initExpression",
            initExpression
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
    return astVisitor.visit(this, input);
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s = %s",
        initLocation.getSourceCode(),
        initExpression.getSourceCode()
    );
  }

  @Override
  public String toString() {
    return "Initialization{" +
        "initLocation=" + initLocation +
        ", initExpression=" + initExpression +
        '}';
  }

  @Override
  public List<Expression> getExpression() {
    return List.of(initExpression);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (initExpression == oldExpr)
      initExpression = newExpr;
  }
}

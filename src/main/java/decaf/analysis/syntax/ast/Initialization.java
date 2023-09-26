package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;

public class Initialization extends Statement implements HasExpression {
  private final RValue initLocation;
  private Expression initExpression;

  public Initialization(
      RValue initLocation,
      Expression initExpression
  ) {
    super(initLocation.getTokenPosition());
    this.initLocation = initLocation;
    this.setInitExpression(initExpression);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "initLocation",
            getInitLocation()
        ),
        new Pair<>(
            "initExpression",
            getInitExpression()
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
            getInitLocation().getSourceCode(),
            getInitExpression().getSourceCode()
    );
  }

  @Override
  public String toString() {
    return "Initialization{" +
           "initLocation=" + getInitLocation() +
           ", initExpression=" + getInitExpression() +
           '}';
  }

  @Override
  public List<Expression> getExpressions() {
    return List.of(getInitExpression());
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getInitExpression() == oldExpr)
      setInitExpression(newExpr);
  }

  public RValue getInitLocation() {
    return initLocation;
  }

  public Expression getInitExpression() {
    return initExpression;
  }

  public void setInitExpression(Expression initExpression) {
    this.initExpression = initExpression;
  }
}

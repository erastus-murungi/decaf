package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
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
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope currentScope
  ) {
    return ASTVisitor.visit(
        this,
        currentScope
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
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

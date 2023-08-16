package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class LocationArray extends Location implements HasExpression {
  public Expression expression;

  public LocationArray(
      Name name,
      Expression expression
  ) {
    super(name);
    this.expression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "id",
            name
        ),
        new Pair<>(
            "expression",
            expression
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
      SymbolTable currentSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        currentSymbolTable
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
  public String toString() {
    return "LocationArray{" + "name=" + name + ", expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s[%s]",
        name.getSourceCode(),
        expression.getSourceCode()
    );
  }

  @Override
  public List<Expression> getExpression() {
    return List.of(expression);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (expression == oldExpr)
      expression = newExpr;
  }
}

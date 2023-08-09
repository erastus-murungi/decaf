package decaf.ast;


import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class CompoundAssignOpExpr extends AssignExpr implements HasExpression {
  public final CompoundAssignOperator compoundAssignOp;

  public CompoundAssignOpExpr(
      TokenPosition tokenPosition,
      CompoundAssignOperator compoundAssignOp,
      Expression expression
  ) {
    super(
        tokenPosition,
        expression
    );
    this.compoundAssignOp = compoundAssignOp;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "compoundAssign",
            compoundAssignOp
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
  public String toString() {
    return "CompoundAssignOpExpr{" + "compoundAssignOp=" + compoundAssignOp + ", expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        compoundAssignOp.getSourceCode(),
        expression.getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable curSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        curSymbolTable
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
  public String getOperator() {
    return compoundAssignOp.label;
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

package decaf.analysis.syntax.ast;


import java.util.HashMap;
import java.util.List;

import decaf.shared.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class BinaryOpExpression extends Expression implements HasExpression {
  public static HashMap<String, Integer> operatorPrecedence = new HashMap<>();

  static {
    operatorPrecedence.put(
        "*",
        13
    );
    operatorPrecedence.put(
        "/",
        13
    );
    operatorPrecedence.put(
        "%",
        13
    );
    operatorPrecedence.put(
        "+",
        12
    );
    operatorPrecedence.put(
        "-",
        12
    );
    operatorPrecedence.put(
        "<",
        10
    );
    operatorPrecedence.put(
        "<=",
        10
    );
    operatorPrecedence.put(
        ">",
        10
    );
    operatorPrecedence.put(
        ">=",
        10
    );
    operatorPrecedence.put(
        "==",
        9
    );
    operatorPrecedence.put(
        "!=",
        9
    );
    operatorPrecedence.put(
        "&&",
        5
    );
    operatorPrecedence.put(
        "||",
        4
    );
  }

  public Expression lhs;
  public BinOperator op;
  public Expression rhs;

  private BinaryOpExpression(
      Expression lhs,
      BinOperator binaryOp,
      Expression rhs
  ) {
    super(lhs.tokenPosition);
    this.lhs = lhs;
    this.rhs = rhs;
    this.op = binaryOp;
  }

  public static BinaryOpExpression of(
      Expression lhs,
      BinOperator binaryOp,
      Expression rhs
  ) {
    BinaryOpExpression binaryOpExpression = new BinaryOpExpression(
        lhs,
        binaryOp,
        rhs
    );
    binaryOpExpression.lhs = lhs;
    binaryOpExpression.rhs = rhs;
    binaryOpExpression.op = binaryOp;
    return maybeRotate(binaryOpExpression);
  }

  private static BinaryOpExpression maybeRotate(BinaryOpExpression parent) {
    if ((parent.rhs instanceof BinaryOpExpression child)) {
      if (operatorPrecedence.get(parent.op.label)
                            .equals(operatorPrecedence.get(child.op.label))) {
        return new BinaryOpExpression(
            new BinaryOpExpression(
                parent.lhs,
                parent.op,
                child.lhs
            ),
            child.op,
            child.rhs
        );
      }
    }
    return parent;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "op",
            op
        ),
        new Pair<>(
            "lhs",
            lhs
        ),
        new Pair<>(
            "rhs",
            rhs
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "BinaryOpExpression{" + "lhs=" + lhs + ", op=" + op + ", rhs=" + rhs + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s %s",
        lhs.getSourceCode(),
        op.getSourceCode(),
        rhs.getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
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
  public List<Expression> getExpression() {
    return List.of(
        rhs,
        lhs
    );
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (oldExpr == rhs)
      rhs = newExpr;
    if (oldExpr == lhs)
      lhs = newExpr;
  }
}

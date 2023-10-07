package decaf.analysis.syntax.ast;


import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;

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

  @NotNull private Expression lhs;
  @NotNull private BinOperator op;
  @NotNull private Expression rhs;

  private BinaryOpExpression(
      @NotNull Expression lhs,
      @NotNull BinOperator binaryOp,
      @NotNull Expression rhs
  ) {
    super(lhs.getTokenPosition());
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
    binaryOpExpression.setLhs(lhs);
    binaryOpExpression.setRhs(rhs);
    binaryOpExpression.setOp(binaryOp);
    return maybeRotate(binaryOpExpression);
  }

  private static BinaryOpExpression maybeRotate(BinaryOpExpression parent) {
    if ((parent.getRhs() instanceof BinaryOpExpression child)) {
      if (operatorPrecedence.get(parent.getOp().getLabel())
                            .equals(operatorPrecedence.get(child.getOp().getLabel()))) {
        return new BinaryOpExpression(
            new BinaryOpExpression(
                    parent.getLhs(),
                    parent.getOp(),
                    child.getLhs()
            ),
            child.getOp(),
            child.getRhs()
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
            getOp()
        ),
        new Pair<>(
            "lhs",
            getLhs()
        ),
        new Pair<>(
            "rhs",
            getRhs()
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "BinaryOpExpression{" + "lhs=" + getLhs() + ", op=" + getOp() + ", rhs=" + getRhs() + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "%s %s %s",
            getLhs().getSourceCode(),
            getOp().getSourceCode(),
            getRhs().getSourceCode()
    );
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }


  @Override
  public List<Expression> getExpressions() {
    return List.of(
            getRhs(),
        getLhs()
    );
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (oldExpr == getRhs())
      setRhs(newExpr);
    if (oldExpr == getLhs())
      setLhs(newExpr);
  }

  public @NotNull Expression getLhs() {
    return lhs;
  }

  public void setLhs(@NotNull Expression lhs) {
    this.lhs = lhs;
  }

  public @NotNull BinOperator getOp() {
    return op;
  }

  public void setOp(@NotNull BinOperator op) {
    this.op = op;
  }

  public @NotNull Expression getRhs() {
    return rhs;
  }

  public void setRhs(@NotNull Expression rhs) {
    this.rhs = rhs;
  }
}

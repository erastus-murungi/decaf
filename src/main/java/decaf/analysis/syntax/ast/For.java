package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.Utils;

public class For extends Statement implements HasExpression {
  private final Initialization initialization;
  private final Assignment update;
  private final Block body;
  private Expression terminatingCondition;

  public For(
      TokenPosition tokenPosition,
      Initialization initialization,
      Expression terminatingCondition,
      Assignment update,
      Block body
  ) {
    super(tokenPosition);
    this.initialization = initialization;
    this.terminatingCondition = terminatingCondition;
    this.update = update;
    this.body = body;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "initialization",
            getInitialization()
        ),
        new Pair<>(
            "terminatingCondition",
            getTerminatingCondition()
        ),
        new Pair<>(
            "update",
            getUpdate()
        ),
        new Pair<>(
                "block",
                getBody()
        )
    );
  }

  @Override
  public String toString() {
    return "For{" +
           "initialization=" + getInitialization() +
           ", terminatingCondition=" + getTerminatingCondition() +
           ", update=" + getUpdate() +
           ", block=" + getBody() +
           '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "%s (%s; %s; %s) {\n    %s\n    }",
            Scanner.RESERVED_FOR,
            getInitialization().getSourceCode(),
            getTerminatingCondition().getSourceCode(),
            getUpdate().getSourceCode(),
            Utils.indentBlock(getBody())
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
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
    return List.of(getTerminatingCondition());
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getTerminatingCondition() == oldExpr)
      setTerminatingCondition(newExpr);
  }

  public Initialization getInitialization() {
    return initialization;
  }

  public Assignment getUpdate() {
    return update;
  }

  public Block getBody() {
    return body;
  }

  public Expression getTerminatingCondition() {
    return terminatingCondition;
  }

  public void setTerminatingCondition(Expression terminatingCondition) {
    this.terminatingCondition = terminatingCondition;
  }
}

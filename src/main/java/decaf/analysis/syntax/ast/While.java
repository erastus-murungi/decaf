package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;

public class While extends Statement implements HasExpression {
  @NotNull private final Block body;
  @NotNull private Expression test;

  public While(
          TokenPosition tokenPosition,
          @NotNull Expression test,
          @NotNull Block body
  ) {
    super(tokenPosition);
    this.test = test;
    this.body = body;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "test",
            getTest()
        ),
        new Pair<>(
            "body",
            getBody()
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "While{" + "test=" + getTest() + ", body=" + getBody() + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "%s (%s) {\n    %s\n    }",
            Scanner.RESERVED_WHILE,
            getTest().getSourceCode(),
            Utils.indentBlock(getBody())
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
    return List.of(getTest());
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getTest() == oldExpr)
      setTest(newExpr);
  }

  public @NotNull Block getBody() {
    return body;
  }

  public @NotNull Expression getTest() {
    return test;
  }

  public void setTest(@NotNull Expression test) {
    this.test = test;
  }
}

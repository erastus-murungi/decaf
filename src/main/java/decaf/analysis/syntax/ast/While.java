package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.env.Scope;

public class While extends Statement implements HasExpression {
  public final Block body;
  public Expression test;

  public While(
      TokenPosition tokenPosition,
      Expression test,
      Block body
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
            test
        ),
        new Pair<>(
            "body",
            body
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "While{" + "test=" + test + ", body=" + body + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s (%s) {\n    %s\n    }",
        Scanner.RESERVED_WHILE,
        test.getSourceCode(),
        Utils.indentBlock(body)
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }


  @Override
  public List<Expression> getExpression() {
    return List.of(test);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (test == oldExpr)
      test = newExpr;
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}

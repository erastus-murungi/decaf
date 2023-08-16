package decaf.analysis.syntax.ast;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.env.Scope;

public class If extends Statement implements HasExpression {
  @NotNull
  public final Block ifBlock;
  @Nullable
  public final Block elseBlock; // maybe null
  @NotNull
  public Expression test;

  public If(
      TokenPosition tokenPosition,
      @NotNull Expression test,
      @NotNull Block ifBlock,
      @Nullable Block elseBlock
  ) {
    super(tokenPosition);
    this.test = test;
    this.ifBlock = ifBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return (elseBlock != null)
        ? List.of(
        new Pair<>(
            "test",
            test
        ),
        new Pair<>(
            "ifBody",
            ifBlock
        ),
        new Pair<>(
            "elseBody",
            elseBlock
        )
    )
        : List.of(
        new Pair<>(
            "test",
            test
        ),
        new Pair<>(
            "ifBody",
            ifBlock
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    if (elseBlock != null)
      return "If{" + "test=" + test + ", ifBlock=" + ifBlock + ", elseBlock=" + elseBlock + '}';
    else return "If{" + "test=" + test + ", ifBlock=" + ifBlock + '}';
  }

  @Override
  public String getSourceCode() {
    String indentedBlockString = Utils.indentBlock(ifBlock);
    if ((elseBlock == null)) {
      return String.format(
          "%s (%s) {\n    %s\n    }",
          Scanner.RESERVED_IF,
          test.getSourceCode(),
          indentedBlockString
      );
    } else {
      return String.format(
          "%s (%s) {\n    %s\n    } %s {\n    %s        \n    }",
          Scanner.RESERVED_IF,
          test.getSourceCode(),
          indentedBlockString,
          Scanner.RESERVED_ELSE,
          Utils.indentBlock(elseBlock)
      );
    }
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

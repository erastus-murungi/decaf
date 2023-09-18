package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.Utils;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class If extends Statement implements HasExpression {
  @NotNull
  private final Block thenBlock;
  @Nullable
  private final Block elseBlock; // maybe null
  @NotNull
  private Expression condition;

  public If(
      TokenPosition tokenPosition,
      @NotNull Expression condition,
      @NotNull Block thenBlock,
      @Nullable Block elseBlock
  ) {
    super(tokenPosition);
    this.condition = condition;
    this.thenBlock = thenBlock;
    this.elseBlock = elseBlock;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    List<Pair<String, AST>> commonChildren = new ArrayList<>();
    commonChildren.add(new Pair<>("condition", getCondition()));
    commonChildren.add(new Pair<>("thenBlock", getThenBlock()));
    if (elseBlock != null) {
      commonChildren.add(new Pair<>("elseBlock", elseBlock));
    }
      return List.copyOf(commonChildren);
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    if (elseBlock != null)
      return "If{" + "condition=" + getCondition() + ", ifBlock=" +
             getThenBlock() + ", elseBlock=" + getElseBlock() + '}';
    else return "If{" + "condition=" + getCondition() + ", ifBlock=" + getThenBlock() + '}';
  }

  @Override
  public String getSourceCode() {
    var indentedBlockString = Utils.indentBlock(getThenBlock());
    if ((elseBlock == null)) {
      return String.format(
              "%s (%s) {\n    %s\n    }",
              Scanner.RESERVED_IF,
              getCondition().getSourceCode(),
              indentedBlockString
      );
    } else {
      return String.format(
              "%s (%s) {\n    %s\n    } %s {\n    %s        \n    }",
              Scanner.RESERVED_IF,
              getCondition().getSourceCode(),
              indentedBlockString,
              Scanner.RESERVED_ELSE,
              Utils.indentBlock(elseBlock)
      );
    }
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
    return List.of(getCondition());
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getCondition() == oldExpr)
      setCondition(newExpr);
  }

  public @NotNull Optional<Block> getElseBlock() {
    return Optional.ofNullable(elseBlock);
  }

  public @NotNull Block getThenBlock() {
    return thenBlock;
  }

  public @NotNull Expression getCondition() {
    return condition;
  }

  public void setCondition(@NotNull Expression condition) {
    this.condition = condition;
  }
}

package decaf.ast;


import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.common.Utils;
import decaf.grammar.Scanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class If extends Statement implements HasExpression {
  public final Block ifBlock;
  public final Block elseBlock; // maybe null
  public Expression test;

  public If(
      TokenPosition tokenPosition,
      Expression test,
      Block ifBlock,
      Block elseBlock
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
      SymbolTable curSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        curSymbolTable
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

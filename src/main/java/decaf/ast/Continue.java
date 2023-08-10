package decaf.ast;


import java.util.Collections;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.Scanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class Continue extends Statement {
  public Continue(TokenPosition tokenPosition) {
    super(tokenPosition);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return "Continue{}";
  }

  @Override
  public String getSourceCode() {
    return Scanner.RESERVED_CONTINUE;
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
    return null;
  }
}

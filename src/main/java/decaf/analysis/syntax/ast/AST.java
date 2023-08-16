package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public abstract class AST {
  public abstract Type getType();

  public abstract List<Pair<String, AST>> getChildren();

  public abstract boolean isTerminal();

  public abstract <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable currentSymbolTable
  );

  public abstract <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  );

  @Override
  public String toString() {
    return this.getClass()
               .getSimpleName();
  }

  public abstract String getSourceCode();
}

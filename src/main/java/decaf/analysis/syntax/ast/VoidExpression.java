package decaf.analysis.syntax.ast;

import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class VoidExpression  extends Expression {
  public VoidExpression(TokenPosition tokenPosition) {
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
  public <T> T accept(
      AstVisitor<T> astVisitor,
      SymbolTable currentSymbolTable
  ) {
    return astVisitor.visit(
        this,
        currentSymbolTable
    );
  }

  @Override
  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSourceCode() {
    return "";
  }
}

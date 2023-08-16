package decaf.ast;

import java.util.Collections;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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

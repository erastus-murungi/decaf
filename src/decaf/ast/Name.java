package decaf.ast;


import java.util.Collections;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;


/* A irAssignableValue name. id holds the name as a string, and ctx is one of the following types. (Load, Store) */
public class Name extends AST {
  public final TokenPosition tokenPosition;
  public final ExprContext context;
  private String label;

  public Name(
      String label,
      TokenPosition tokenPosition,
      ExprContext context
  ) {
    this.label = label;
    this.tokenPosition = tokenPosition;
    this.context = context;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return "Name{" + "label='" + label + '\'' + ", context=" + context + '}';
  }

  @Override
  public String getSourceCode() {
    return label;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
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
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
  }
}

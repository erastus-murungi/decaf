package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;


/* A irAssignableValue name. id holds the name as a string, and ctx is one of the following types. (Load, Store) */
public class Name extends AST {
  public final TokenPosition tokenPosition;
  private String label;

  public static final Name dummyName = new Name(
      "INVALID NAME", TokenPosition.dummyTokenPosition()
  );

  public Name(
      String label,
      TokenPosition tokenPosition
  ) {
    this.label = label;
    this.tokenPosition = tokenPosition;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return "Name{" + "label='" + label + '\'' +'}';
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

package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class Len extends Expression {
  final public Name nameId;
  final public Type type = Type.Int;

  public Len(
      TokenPosition tokenPosition,
      Name nameId
  ) {
    super(tokenPosition);
    this.tokenPosition = tokenPosition;
    this.nameId = nameId;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "id",
        nameId
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "Len{" + "nameId=" + nameId + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s (%s)",
        Scanner.RESERVED_LEN,
        nameId.getLabel()
    );
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
    return codegenAstVisitor.visit(this);
  }

}

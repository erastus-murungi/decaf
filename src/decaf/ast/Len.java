package decaf.ast;


import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
        DecafScanner.RESERVED_LEN,
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

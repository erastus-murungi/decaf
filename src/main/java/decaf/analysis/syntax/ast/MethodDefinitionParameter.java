package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class MethodDefinitionParameter extends Declaration {
  final public TokenPosition tokenPosition;
  final private String name;
  final private Type type;

  public MethodDefinitionParameter(
      TokenPosition tokenPosition,
      String name,
      Type type
  ) {
    this.tokenPosition = tokenPosition;
    this.name = name;
    this.type = type;
  }

  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "type",
        new Name(type.toString(),
                 tokenPosition
        )
    ));
  }

  @Override
  public String toString() {
    return this.getClass()
               .getSimpleName() + "name=" + name + ", type=" + type + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        type.getSourceCode(),
        name
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
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

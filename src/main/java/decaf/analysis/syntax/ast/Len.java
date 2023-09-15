package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;
import decaf.ir.types.Type;

public class Len extends Expression {
  final public RValue rValue;
  final public Type type = Type.getIntType();

  public Len(
      TokenPosition tokenPosition,
      RValue rValue
  ) {
    super(tokenPosition);
    this.tokenPosition = tokenPosition;
    this.rValue = rValue;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "id",
        rValue
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "Len{" + "nameId=" + rValue + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s (%s)",
        Scanner.RESERVED_LEN,
        rValue.getLabel()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
    );
  }
}

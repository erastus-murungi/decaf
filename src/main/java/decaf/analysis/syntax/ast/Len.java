package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Len extends Expression {
  final private RValue arrayName;
  final public Type type = Type.getIntType();

  public Len(
      TokenPosition tokenPosition,
      RValue arrayName
  ) {
    super(tokenPosition);
    this.tokenPosition = tokenPosition;
    this.arrayName = arrayName;
  }

  public String getArrayLabel() {
    return arrayName.getLabel();
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "id",
        arrayName
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "Len{" + "nameId=" + arrayName + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "%s (%s)",
            Scanner.RESERVED_LEN,
            arrayName.getLabel()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }
}

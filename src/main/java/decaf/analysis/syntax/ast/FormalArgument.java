package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class FormalArgument extends Declaration {
  final public TokenPosition tokenPosition;
  final private String name;
  final private Type type;

  public FormalArgument(
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
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "type",
        new RValue(
            type.toString(),
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
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }

  public Type getType() {
    return type;
  }
}

package decaf.analysis.syntax.ast;


import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class FieldDeclaration extends Declaration {
  @NotNull
  final public List<RValue> vars;
  @NotNull
  final public List<Array> arrays;
  @NotNull
  final private Type type;

  public FieldDeclaration(
      @NotNull TokenPosition tokenPosition,
      @NotNull Type type,
      @NotNull List<RValue> vars,
      @NotNull List<Array> arrays
  ) {
    super(tokenPosition);
    this.type = type;
    this.vars = vars;
    this.arrays = arrays;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
   var pairArrayList = new ArrayList<Pair<String, AST>>();
    pairArrayList.add(new Pair<>(
        "type",
        new RValue(
                getTokenPosition(),
            type.toString()
        )
    ));
    for (RValue RValue : vars)
      pairArrayList.add(new Pair<>(
          "var",
          RValue
      ));
    for (Array array : arrays)
      pairArrayList.add(new Pair<>(
          "array",
          array
      ));
    return pairArrayList;
  }

  public @NotNull Type getType() {
    return type;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "FieldDeclaration{" + "type=" + type + ", names=" + vars + ", arrays=" + arrays + '}';
  }

  @Override
  public String getSourceCode() {
    List<String> stringList = new ArrayList<>();
    for (RValue RValue : vars)
      stringList.add(RValue.getSourceCode());
    for (Array array : arrays)
      stringList.add(array.getSourceCode());
    String args = String.join(
        ", ",
        stringList
    );
    return String.format(
        "%s %s",
        type.getSourceCode(),
        args
    );
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }
}

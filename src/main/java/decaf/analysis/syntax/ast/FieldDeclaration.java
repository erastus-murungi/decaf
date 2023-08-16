package decaf.analysis.syntax.ast;


import java.util.ArrayList;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class FieldDeclaration extends Declaration {
  final public TokenPosition tokenPosition;
  final public List<RValue> vars;
  final public List<Array> arrays;
  final private Type type;

  public FieldDeclaration(
      TokenPosition tokenPosition,
      Type type,
      List<RValue> vars,
      List<Array> arrays
  ) {
    this.tokenPosition = tokenPosition;
    this.type = type;
    this.vars = vars;
    this.arrays = arrays;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    ArrayList<Pair<String, AST>> pairArrayList = new ArrayList<>();
    pairArrayList.add(new Pair<>(
        "type",
        new RValue(
            type.toString(),
            tokenPosition
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
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
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

  @Override
  public Type getType() {
    return type;
  }
}

package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class LocationVariable extends Location {
  public LocationVariable(RValue RValue) {
    super(RValue);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.singletonList(new Pair<>(
        "name",
        RValue
    ));
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
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return "LocationVariable{" + "name=" + RValue + '}';
  }

  @Override
  public String getSourceCode() {
    return RValue.getSourceCode();
  }
}

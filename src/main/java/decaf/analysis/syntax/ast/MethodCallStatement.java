package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class MethodCallStatement extends Statement {
  public final MethodCall methodCall;

  public MethodCallStatement(
      TokenPosition tokenPosition,
      MethodCall methodCall
  ) {
    super(tokenPosition);
    this.methodCall = methodCall;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return methodCall.getChildren();
  }

  @Override
  public String toString() {
    return methodCall.toString();
  }

  @Override
  public String getSourceCode() {
    return methodCall.getSourceCode();
  }

  @Override
  public boolean isTerminal() {
    return false;
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
}

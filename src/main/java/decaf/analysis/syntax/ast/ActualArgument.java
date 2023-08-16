package decaf.analysis.syntax.ast;


import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;

public abstract class ActualArgument extends AST {
  public Type type;

  public abstract <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  );
}

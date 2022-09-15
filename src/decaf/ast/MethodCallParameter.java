package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;

public abstract class MethodCallParameter extends AST {
    public Type type;

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation);
}

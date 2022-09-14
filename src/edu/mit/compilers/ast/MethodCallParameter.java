package edu.mit.compilers.ast;


import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;

public abstract class MethodCallParameter extends AST {
    public Type type;

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation);
}

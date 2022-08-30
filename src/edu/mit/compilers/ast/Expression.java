package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;

public abstract class Expression extends AST {
    public TokenPosition tokenPosition;

    protected Type type;

    public void setType(Type type) {
        this.type = type;
    }

    public Expression(TokenPosition tokenPosition) {
        this.tokenPosition = tokenPosition;
    }

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation);

    @Override
    public Type getType() {
        return type;
    }
}

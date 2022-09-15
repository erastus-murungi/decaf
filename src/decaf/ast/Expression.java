package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.TokenPosition;

public abstract class Expression extends AST {
    public TokenPosition tokenPosition;

    protected Type type;

    public Expression(TokenPosition tokenPosition) {
        this.tokenPosition = tokenPosition;
    }

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation);

    @Override
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}

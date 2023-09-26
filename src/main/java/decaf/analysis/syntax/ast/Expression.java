package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;

public abstract class Expression extends AST implements Typed {
    private Type type = Type.getUnsetType();

    public Expression(TokenPosition tokenPosition) {
        super(tokenPosition);
    }

    @NotNull
    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void setType(@NotNull Type type) {
        this.type = type;
    }

    public Branch toEvalCondition() {
        if (this.getType() != Type.getBoolType()) {
            throw new IllegalArgumentException("Condition must evaluate to type bool");
        }
        return new Branch(this);
    }
}

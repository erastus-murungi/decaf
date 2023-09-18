package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;
import org.jetbrains.annotations.NotNull;

public abstract class ActualArgument extends AST implements Typed {
    public ActualArgument(@NotNull TokenPosition tokenPosition) {
        super(tokenPosition);
    }
}

package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;
import org.jetbrains.annotations.NotNull;

public abstract class Declaration extends Statement {
    public Declaration(@NotNull TokenPosition tokenPosition) {
        super(tokenPosition);
    }
}

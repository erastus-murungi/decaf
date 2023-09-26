package decaf.analysis.syntax.ast;

import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Branch extends Statement {
    @NotNull private final Expression condition;

    public Branch(@NotNull Expression condition) {
        super(condition.getTokenPosition());
        if (condition.getType() != Type.getBoolType()) {
            throw new IllegalArgumentException("Condition must evaluate of type bool");
        }
        this.condition = condition;
    }
    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("condition", condition));
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
        throw new IllegalStateException();
    }

    @Override
    public String getSourceCode() {
        return String.format("branch on: %s", condition.getSourceCode());
    }

    public @NotNull Expression getCondition() {
        return condition;
    }
}

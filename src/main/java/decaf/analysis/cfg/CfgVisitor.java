package decaf.analysis.cfg;

import org.jetbrains.annotations.NotNull;

public interface CfgVisitor<InputType, ReturnType> {
    @NotNull ReturnType visit(@NotNull CfgBlock cfgBlock, @NotNull InputType input);
}

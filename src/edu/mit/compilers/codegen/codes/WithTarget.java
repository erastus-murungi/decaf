package edu.mit.compilers.codegen.codes;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.cfg.BasicBlock;

public interface WithTarget {
    @NotNull BasicBlock getTarget();

    void setTargetWithTributary(@NotNull BasicBlock newTarget);

    default void replaceTarget(BasicBlock newTarget) {
        var oldTarget = getTarget();
        setTargetWithTributary(newTarget);
        oldTarget.removeTributary(this);
    }
}

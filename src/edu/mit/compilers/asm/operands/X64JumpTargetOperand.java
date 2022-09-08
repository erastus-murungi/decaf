package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.cfg.BasicBlock;

public class X64JumpTargetOperand extends X64Operand {
    @NotNull private final String target;

    public X64JumpTargetOperand(@NotNull BasicBlock target) {
        this.target = target.getInstructionList().getLabelForAsm();
    }

    @Override
    public String toString() {
        return String.format(".%s", target);
    }
}

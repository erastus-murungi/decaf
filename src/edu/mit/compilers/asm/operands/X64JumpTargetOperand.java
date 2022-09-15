package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.asm.X86Register;
import edu.mit.compilers.cfg.BasicBlock;

public class X64JumpTargetOperand extends X86Value {
    @NotNull private final String target;

    public X64JumpTargetOperand(@NotNull BasicBlock target) {
        super(null);
        this.target = target.getInstructionList().getLabelForAsm();
    }

    @Override
    public String toString() {
        return String.format(".%s", target);
    }

    @Override
    public List<X86Register> registersInUse() {
        return Collections.emptyList();
    }
}

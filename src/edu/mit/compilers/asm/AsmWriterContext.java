package edu.mit.compilers.asm;

import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.codegen.codes.FunctionCall;

public class AsmWriterContext {
    /**
     * keeps track of whether the {@code .text} label has been added or not
     */
    private boolean textAdded = false;

    /**
     * Keeps track of the last comparison operator used
     * This is useful for evaluating conditionals
     * {@code lastComparisonOperator} will always have a value if a conditional jump evaluates
     * a variable;
     */
    @Nullable
    private String lastComparisonOperator = null;

    private int locationOfSubqInst = 0;

    private int maxStackSpaceForArgs = 0;

    public void setMaxStackSpaceForArgs(FunctionCall functionCall) {
        this.maxStackSpaceForArgs = Math.max(maxStackSpaceForArgs, (functionCall.getNumArguments() - X64RegisterType.N_ARG_REGISTERS) * 8);
    }

    public boolean isTextLabelAdded() {
        return textAdded;
    }

    public void setTextLabelAdded() {
        textAdded = true;
    }

    @Nullable public String getLastComparisonOperator() {
        return lastComparisonOperator;
    }

    public void setLastComparisonOperator(@Nullable String lastComparisonOperator) {
        this.lastComparisonOperator = lastComparisonOperator;
    }

    public int getLocationOfSubqInst() {
        return locationOfSubqInst;
    }

    public void setLocationOfSubqInst(int locationOfSubqInst) {
        this.locationOfSubqInst = locationOfSubqInst;
    }
}

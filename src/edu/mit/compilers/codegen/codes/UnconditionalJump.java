package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UnconditionalJump extends Instruction {
    public final BasicBlock target;

    public BasicBlock getTarget() {
        return target;
    }

    public UnconditionalJump(BasicBlock target) {
        super(null);
        this.target = target;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "goto", getTarget().getLabel().getLabel());
    }

    @Override
    public String syntaxHighlightedToString() {
        var goTo =  Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s", DOUBLE_INDENT, goTo, getTarget().getLabel().getLabel());
    }

    @Override
    public Instruction copy() {
        return new UnconditionalJump(getTarget());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnconditionalJump that = (UnconditionalJump) o;
        return Objects.equals(getTarget().getLabel(), that.getTarget().getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTarget().getLabel());
    }
}

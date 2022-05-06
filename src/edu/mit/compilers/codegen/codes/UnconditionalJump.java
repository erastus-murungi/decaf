package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UnconditionalJump extends Instruction {
    public final Label goToLabel;

    public UnconditionalJump(Label goToLabel) {
        super(null);
        this.goToLabel = goToLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "goto", goToLabel.label);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        var goTo =  Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_PURPLE_BOLD);
        return String.format("%s%s %s", DOUBLE_INDENT, goTo, goToLabel.label);
    }

    @Override
    public Instruction copy() {
        return new UnconditionalJump(goToLabel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnconditionalJump that = (UnconditionalJump) o;
        return Objects.equals(goToLabel, that.goToLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goToLabel);
    }
}

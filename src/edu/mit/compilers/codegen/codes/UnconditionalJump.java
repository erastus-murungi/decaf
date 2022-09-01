package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.utils.Utils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class UnconditionalJump extends Instruction {
    public final Label goToLabel;

    public UnconditionalJump(Label goToLabel) {
        super(null);
        this.goToLabel = goToLabel;
        if (goToLabel == null) {
            System.out.println("stop");
        }
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "goto", goToLabel.getLabel());
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
    public String syntaxHighlightedToString() {
        var goTo =  Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
//        var goTo =  "goto";
        if (goToLabel == null)
            System.out.println("stop");
        return String.format("%s%s %s", DOUBLE_INDENT, goTo, goToLabel.getLabel());
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

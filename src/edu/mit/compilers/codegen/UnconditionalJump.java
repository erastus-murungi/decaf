package edu.mit.compilers.codegen;

public class UnconditionalJump extends ThreeAddressCode {
    public final Label goToLabel;

    public UnconditionalJump(Label goToLabel) {
        super(null);
        this.goToLabel = goToLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "GoTo", goToLabel.label);
    }
}

package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class JumpIfTrue extends ThreeAddressCode {
    public final String condition;
    public final Label trueLabel;

    public JumpIfTrue(AST source, String condition, Label trueLabel) {
        super(source);
        this.condition = condition;
        this.trueLabel = trueLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s %s", DOUBLE_INDENT, "IfTrue", condition, "GoTo", trueLabel.label);
    }
}

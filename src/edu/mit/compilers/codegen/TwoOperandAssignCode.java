package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class TwoOperandAssignCode extends AbstractAssignment {
    String fstOperand;
    String operator;
    String sndOperand;


    public TwoOperandAssignCode(AST source, String result, String fstOperand, String operator, String sndOperand, String comment) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, dst, fstOperand, operator, sndOperand, DOUBLE_INDENT, " <<<< " + getComment().get());
        return String.format("%s%s = %s %s %s", DOUBLE_INDENT, dst, fstOperand, operator, sndOperand);
    }
}

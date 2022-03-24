package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class TwoOperandAssignCode extends Assignment {
    String result;
    String fstOperand;
    String operator;
    String sndOperand;


    public TwoOperandAssignCode(AST source, String result, String fstOperand, String operator, String sndOperand) {
        super(source);
        this.result = result;
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    @Override
    public String toString() {
        return String.format("    %s = %s %s %s", result, fstOperand, operator, sndOperand);
    }
}

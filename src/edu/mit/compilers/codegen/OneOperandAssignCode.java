package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class OneOperandAssignCode extends AbstractAssignment {
    String result;
    String operand;
    String operator;

    public OneOperandAssignCode(AST source, String result, String operand, String operator) {
        super(source);
        this.result = result;
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return String.format("%s = %s %s", result, operator, operand);
    }
}

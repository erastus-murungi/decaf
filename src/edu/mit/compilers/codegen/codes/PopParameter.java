package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

import java.util.List;

public class PopParameter extends Instruction implements HasOperand {
    public AssignableName parameterName;
    public int parameterIndex;

    public PopParameter(AssignableName parameterName, AST source, int parameterIndex) {
        super(source);
        this.parameterName = parameterName;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s%s%s", DOUBLE_INDENT, "pop", parameterName, DOUBLE_INDENT, getComment().orElse(""));
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(parameterName);
    }

    @Override
    public String repr() {
        var pop =  Utils.coloredPrint("pop", Utils.ANSIColorConstants.ANSI_PURPLE_BOLD);
        return String.format("%s%s %s%s%s", DOUBLE_INDENT, pop, parameterName.repr(), DOUBLE_INDENT, getComment().orElse(""));
    }

    @Override
    public Instruction copy() {
        return new PopParameter(parameterName, source, parameterIndex);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(parameterName);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(parameterName);
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (parameterName.equals(oldVariable)) {
            parameterName = (AssignableName) replacer;
            replaced = true;
        }
        return replaced;
    }

}

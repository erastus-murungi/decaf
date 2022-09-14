package edu.mit.compilers.codegen.codes;

import java.util.List;
import java.util.Optional;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.utils.Operators;

public class UnaryInstruction extends StoreInstruction {
    public IrValue operand;
    public String operator;

    public UnaryInstruction(IrAssignableValue result, String operator, IrValue operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return List.of(getDestination(), operand);
    }

    @Override
    public String syntaxHighlightedToString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, getDestination(), Operators.getColoredUnaryOperatorName(operator), operand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, getDestination(), Operators.getColoredUnaryOperatorName(operator), operand.repr());
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, getDestination(), Operators.getUnaryOperatorName(operator), operand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, getDestination(), Operators.getOperatorName(operator), operand.repr());
    }

    @Override
    public Instruction copy() {
        return new UnaryInstruction(getDestination(), operator, operand, source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (operand instanceof IrMemoryAddress || getDestination() instanceof IrMemoryAddress)
            return Optional.empty();
        return Optional.of(new UnaryOperand(this));
    }

    @Override
    public UnaryInstruction clone() {
        UnaryInstruction clone = (UnaryInstruction) super.clone();
        clone.operand = operand;
        clone.operator = operator;
        clone.setComment(getComment().orElse(null));
        clone.setDestination(destination);
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnaryOperand(this);
    }

    public boolean replaceValue(IrValue oldVariable, IrValue replacer) {
        var replaced = false;
        if (operand.equals(oldVariable)) {
            operand = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public List<IrValue> getOperandValues() {
        return List.of(operand);
    }

}

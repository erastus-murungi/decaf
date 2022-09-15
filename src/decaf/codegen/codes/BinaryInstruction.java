package decaf.codegen.codes;

import java.util.List;
import java.util.Optional;

import decaf.asm.AsmWriter;
import decaf.codegen.names.IrAssignableValue;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrRegister;
import decaf.common.Operators;
import decaf.ast.AST;
import decaf.codegen.names.IrValue;
import decaf.dataflow.operand.BinaryOperand;
import decaf.dataflow.operand.Operand;

/**
 * A quad has four fields which we call op arg1, arg2, and result.
 * The op field contains an internal code for the operator.
 * For instance, the three address instruction x = y + z is represented by placing + in op, y in arg1, z in arg2 and x in result.
 */

public class BinaryInstruction extends StoreInstruction {
    public IrValue fstOperand;
    public String operator;
    public IrValue sndOperand;


    public BinaryInstruction(IrAssignableValue result, IrValue fstOperand, String operator, IrValue sndOperand, String comment, AST source) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    public BinaryInstruction(IrRegister result, IrValue fstOperand, String operator, IrValue sndOperand) {
        this(result, fstOperand, operator, sndOperand, String.format("%s = %s %s %s", result, fstOperand, operator, sndOperand), null);
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return List.of(getDestination(), fstOperand, sndOperand);
    }

    @Override
    public Instruction copy() {
        return new BinaryInstruction(getDestination(), fstOperand, operator, sndOperand, getComment().orElse(null), source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (getDestination() instanceof IrMemoryAddress || fstOperand instanceof IrMemoryAddress || sndOperand instanceof IrMemoryAddress)
            return Optional.empty();
        return Optional.of(new BinaryOperand(this));
    }

    public boolean replaceValue(IrValue oldVariable, IrValue replacer) {
        var replaced = false;
        if (fstOperand.equals(oldVariable)) {
            fstOperand = replacer;
            replaced = true;
        }
        if (sndOperand.equals(oldVariable)) {
            sndOperand = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public Operand getOperand() {
        return new BinaryOperand(this);
    }

    @Override
    public List<IrValue> getOperandValues() {
        return List.of(fstOperand, sndOperand);
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, getDestination(), fstOperand, operator, sndOperand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s %s", DOUBLE_INDENT, getDestination(), fstOperand, operator, sndOperand);
    }

    public String syntaxHighlightedToString() {
        if (getComment().isPresent())
            return String.format("%s%s: %s = %s %s, %s %s%s", DOUBLE_INDENT, getDestination(), getDestination().getType().getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand, sndOperand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s: %s = %s %s, %s", DOUBLE_INDENT, getDestination(), getDestination().getType().getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand, sndOperand);
    }

}

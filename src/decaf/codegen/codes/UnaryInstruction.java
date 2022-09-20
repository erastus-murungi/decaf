package decaf.codegen.codes;

import java.util.List;
import java.util.Optional;

import decaf.asm.AsmWriter;
import decaf.ast.AST;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrValue;
import decaf.common.Operators;
import decaf.dataflow.operand.Operand;
import decaf.dataflow.operand.UnaryOperand;

public class UnaryInstruction extends StoreInstruction {
    public IrValue operand;
    public String operator;

    public UnaryInstruction(IrAssignable result, String operator, IrValue operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> genIrValuesSurface() {
        return List.of(getDestination(), operand);
    }

    @Override
    public String syntaxHighlightedToString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, getDestination(), Operators.getColoredUnaryOperatorName(operator), operand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, getDestination(), Operators.getColoredUnaryOperatorName(operator), operand);
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, getDestination(), Operators.getUnaryOperatorName(operator), operand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, getDestination(), Operators.getOperatorName(operator), operand);
    }

    @Override
    public Instruction copy() {
        return new UnaryInstruction(getDestination(), operator, operand,
                                    getSource()
        );
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
        clone.setSource(getSource());
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
    public List<IrValue> genOperandIrValuesSurface() {
        return List.of(operand);
    }

}

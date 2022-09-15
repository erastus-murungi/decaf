package decaf.codegen.codes;

import java.util.List;
import java.util.Optional;

import decaf.asm.AsmWriter;
import decaf.codegen.names.IrAssignableValue;
import decaf.codegen.names.IrMemoryAddress;
import decaf.common.Utils;
import decaf.dataflow.operand.UnmodifiedOperand;
import decaf.ast.AST;
import decaf.codegen.names.IrValue;
import decaf.dataflow.operand.Operand;

public class CopyInstruction extends StoreInstruction implements Cloneable {
    private IrValue irValue;

    public CopyInstruction(IrAssignableValue dst, IrValue operand, AST source, String comment) {
        super(dst, source, comment);
        this.irValue = operand;
    }

    public static CopyInstruction noAstConstructor(IrAssignableValue dst, IrValue operand) {
        return new CopyInstruction(dst, operand, null, String.format("%s = %s", dst, operand));
    }

    public static CopyInstruction noMetaData(IrAssignableValue dst, IrValue operand) {
        return new CopyInstruction(dst, operand, null, "");
    }

    public IrValue getValue() {
        return irValue;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return List.of(getDestination(), irValue);
    }

    @Override
    public Instruction copy() {
        return new CopyInstruction(getDestination(), irValue, source, getComment().orElse(null));
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (irValue instanceof IrMemoryAddress)
            return Optional.empty();
        return Optional.of(new UnmodifiedOperand(irValue));
    }

    public boolean contains(IrValue name) {
        return getDestination().equals(name) || irValue.equals(name);
    }

    @Override
    public CopyInstruction clone() {
        CopyInstruction clone = (CopyInstruction) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.irValue = irValue;
        clone.setComment(getComment().orElse(null));
        clone.setDestination(getDestination());
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(irValue);
    }

    @Override
    public List<IrValue> getOperandValues() {
        return List.of(irValue);
    }

    public boolean replaceValue(IrValue oldVariable, IrValue replacer) {
        var replaced = false;
        if (irValue.equals(oldVariable)) {
            irValue = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s, %s", DOUBLE_INDENT, "store", irValue, getDestination());
    }

    @Override
    public String syntaxHighlightedToString() {
        var storeString = Utils.coloredPrint("store", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s, %s", DOUBLE_INDENT, storeString, irValue, getDestination());
    }

}

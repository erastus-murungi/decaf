package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

import java.util.List;
import java.util.Optional;

public class CopyInstruction extends StoreInstruction implements Cloneable {
    private Value value;

    public Value getValue() {
        return value;
    }

    public CopyInstruction(LValue dst, Value operand, AST source, String comment) {
        super(dst, source, comment);
        this.value = operand;
    }
    public static CopyInstruction noAstConstructor(LValue dst, Value operand) {
        return new CopyInstruction(dst, operand, null, String.format("%s = %s", dst, operand));
    }

    public static CopyInstruction noMetaData(LValue dst, Value operand) {
        return new CopyInstruction(dst, operand, null, "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return List.of(getStore(), value);
    }

    @Override
    public Instruction copy() {
        return new CopyInstruction(getStore(), value, source, getComment().orElse(null));
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (value instanceof MemoryAddress)
            return Optional.empty();
        return Optional.of(new UnmodifiedOperand(value));
    }

    public boolean contains(Value name) {
        return getStore().equals(name) || value.equals(name);
    }

    @Override
    public CopyInstruction clone() {
        CopyInstruction clone = (CopyInstruction) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.value = value;
        clone.setComment(getComment().orElse(null));
        clone.setStore(getStore());
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(value);
    }

    @Override
    public List<Value> getOperandValues() {
        return List.of(value);
    }

    public boolean replace(Value oldVariable, Value replacer) {
        var replaced = false;
        if (value.equals(oldVariable)) {
            value = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s, %s", DOUBLE_INDENT, "store", value.repr(), getStore().repr());
    }

    @Override
    public String syntaxHighlightedToString() {
        var storeString = Utils.coloredPrint("store", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s, %s", DOUBLE_INDENT, storeString, value.repr(), getStore().repr());
    }

}

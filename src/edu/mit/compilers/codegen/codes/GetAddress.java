package edu.mit.compilers.codegen.codes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.GetAddressOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;


public class GetAddress extends StoreInstruction {
    private final NumericalConstant length;
    private Value baseAddress;
    private Value index;

    public GetAddress(AST source, Value baseAddress, Value index, LValue dest, NumericalConstant length) {
        super(dest, source);
        this.baseAddress = baseAddress;
        this.index = index;
        this.length = length;
    }

    public Optional<NumericalConstant> getLength() {
        return Optional.ofNullable(length);
    }

    public Value getIndex() {
        return index;
    }

    public Value getBaseAddress() {
        if (baseAddress == null)
            throw new IllegalStateException("the base address is null");
        return baseAddress;
    }

    public MemoryAddress getDestination() {
        return (MemoryAddress) super.getDestination();
    }

    @Override
    public void accept(AsmWriter visitor) {
        visitor.emitInstruction(this);
    }

    @Override
    public List<Value> getAllValues() {
        return List.of(baseAddress, getIndex(), getDestination());
    }

    @Override
    public String syntaxHighlightedToString() {
        final var getAddressString = Utils.coloredPrint("getaddr", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s = %s %s %s, %s", DOUBLE_INDENT, getDestination().repr(), getAddressString, baseAddress.repr(), baseAddress.getType().getColoredSourceCode(), getIndex().repr());
    }

    @Override
    public Instruction copy() {
        return new GetAddress(source, baseAddress, index, getDestination(), length);
    }

    @Override
    public Operand getOperand() {
        return new GetAddressOperand(this);
    }

    @Override
    public List<Value> getOperandValues() {
        if (index != null)
            return List.of(baseAddress, index);
        return List.of(baseAddress);
    }

    @Override
    public boolean replaceValue(Value oldName, Value newName) {
        var replaced = false;
        if (oldName == baseAddress) {
            baseAddress = newName;
            replaced = true;
        }
        if (oldName == index) {
            index = newName;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAddress that = (GetAddress) o;
        return Objects.equals(getBaseAddress(), that.getBaseAddress()) && Objects.equals(getIndex(), that.getIndex()) && Objects.equals(getLength(), that.getLength());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBaseAddress(), getIndex(), getLength());
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        return Optional.empty();
    }
}

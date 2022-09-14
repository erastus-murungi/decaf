package edu.mit.compilers.codegen.codes;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.dataflow.operand.GetAddressOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;


public class GetAddress extends StoreInstruction {
    private final long length;
    private IrValue baseAddress;
    private IrValue index;

    public GetAddress(IrValue baseAddress, IrValue index, IrAssignableValue dest, long length, AST source) {
        super(dest, source);
        this.baseAddress = baseAddress;
        this.index = index;
        this.length = length;
    }

    public Long getLength() {
        return length;
    }

    public IrValue getIndex() {
        return index;
    }

    public IrValue getBaseAddress() {
        if (baseAddress == null)
            throw new IllegalStateException("the base address is null");
        return baseAddress;
    }

    public IrMemoryAddress getDestination() {
        return (IrMemoryAddress) super.getDestination();
    }

    @Override
    public void accept(AsmWriter visitor) {
        visitor.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return List.of(baseAddress, getIndex(), getDestination());
    }

    @Override
    public String toString() {
        return String.format("%s%s = %s %s %s, %s", DOUBLE_INDENT, getDestination(), "getaddr", baseAddress, baseAddress.getType().getColoredSourceCode(), getIndex());
    }

    @Override
    public String syntaxHighlightedToString() {
        final var getAddressString = Utils.coloredPrint("getaddr", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s = %s %s %s, %s", DOUBLE_INDENT, getDestination(), getAddressString, baseAddress, baseAddress.getType().getColoredSourceCode(), getIndex());
    }

    @Override
    public Instruction copy() {
        return new GetAddress(baseAddress, index, getDestination(), length, source);
    }

    @Override
    public Operand getOperand() {
        return new GetAddressOperand(this);
    }

    @Override
    public List<IrValue> getOperandValues() {
        if (index != null)
            return List.of(baseAddress, index);
        return List.of(baseAddress);
    }

    @Override
    public boolean replaceValue(IrValue oldName, IrValue newName) {
        var replaced = false;
        if (oldName == baseAddress) {
            if (!(newName instanceof IrRegister)) {
                throw new IllegalArgumentException();
            } else {
                baseAddress = (IrAssignableValue) newName;
                replaced = true;
            }
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

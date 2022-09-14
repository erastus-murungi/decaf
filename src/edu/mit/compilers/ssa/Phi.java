package edu.mit.compilers.ssa;

import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.PhiOperand;

public class Phi extends StoreInstruction {
    private final Map<BasicBlock, IrValue> basicBlockValueMap;

    public Phi(IrRegister v, Map<BasicBlock, IrValue> xs) {
        super(v, null);
        assert xs.size() > 2;
        basicBlockValueMap = xs;
    }

    public void removePhiOperand(IrValue irValue) {
        assert basicBlockValueMap.containsValue(irValue);
        for (var entrySet: basicBlockValueMap.entrySet()) {
            if (entrySet.getValue().equals(irValue)) {
                basicBlockValueMap.remove(entrySet.getKey());
                return;
            }
        }
        throw new IllegalStateException();
    }

    @NotNull public IrValue getVariableForB(@NotNull BasicBlock B) {
        var value =  basicBlockValueMap.get(B);
        checkState(value != null, this + "\nno irAssignableValue for block: \n" + B);
        return value;
    }

    public void replaceBlock(@NotNull BasicBlock B, @NotNull BasicBlock R) {
        var ret = basicBlockValueMap.remove(B);
        checkState(ret != null);
        basicBlockValueMap.put(R, ret);
    }

    @NotNull public BasicBlock getBasicBlockForV(@NotNull IrValue irValue) {
        checkState(basicBlockValueMap.containsValue(irValue));
        for (var entrySet: basicBlockValueMap.entrySet()) {
            if (entrySet.getValue().equals(irValue))
                return entrySet.getKey();
        }
        throw new IllegalStateException();
    }

    @Override
    public Operand getOperand() {
        return new PhiOperand(this);
    }

    @Override
    public List<IrValue> getOperandValues() {
        return new ArrayList<>(basicBlockValueMap.values());
    }

    @Override
    public boolean replaceValue(IrValue oldName, IrValue newName) {
        var replaced = false;
        for (BasicBlock basicBlock : basicBlockValueMap.keySet()) {
            var abstractName = basicBlockValueMap.get(basicBlock);
            if (abstractName.equals(oldName)) {
                basicBlockValueMap.put(basicBlock, newName);
                replaced = true;
            }
        }
        return replaced;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
    }

    @Override
    public List<IrValue> getAllValues() {
        var allNames = getOperandValues();
        allNames.add(getDestination());
        return allNames;
    }

    @Override
    public Instruction copy() {
        return new Phi((IrRegister) getDestination(), basicBlockValueMap);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        return Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phi phi = (Phi) o;
        return Objects.equals(basicBlockValueMap.values(), phi.basicBlockValueMap.values()) && Objects.equals(getDestination(), phi.getDestination());
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicBlockValueMap.values());
    }

    @Override
    public String toString() {
        String rhs = basicBlockValueMap.values().stream().map(IrValue::toString).collect(Collectors.joining(", "));
        return String.format("%s%s: %s = phi (%s)", DOUBLE_INDENT, getDestination(), getDestination().getType().getSourceCode(), rhs);
    }

    @Override
    public String syntaxHighlightedToString() {
        String rhs = basicBlockValueMap.values().stream().map(IrValue::toString).collect(Collectors.joining(", "));
        return String.format("%s%s: %s = phi (%s)", DOUBLE_INDENT, getDestination(), getDestination().getType().getColoredSourceCode(), rhs);
    }
}

package edu.mit.compilers.ssa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.PhiOperand;

public class Phi extends StoreInstruction {
    private final Map<BasicBlock, Value> basicBlockToAssignableNameMapping;
    private final Map<Value, BasicBlock> valueBasicBlockMap;

    public Phi(LValue v, Map<BasicBlock, Value> xs) {
        super(v, null);
        assert xs.size() > 2;
        basicBlockToAssignableNameMapping = xs;
        valueBasicBlockMap = new HashMap<>();
        for (var entry: basicBlockToAssignableNameMapping.entrySet()) {
            valueBasicBlockMap.put(entry.getValue(), entry.getKey());
        }
    }

    public Value getVariableForB(BasicBlock B) {
        return basicBlockToAssignableNameMapping.get(B);
    }

    public BasicBlock getBasicBlockForV(Value value) {
        assert valueBasicBlockMap.containsKey(value);
        return valueBasicBlockMap.get(value);
    }

    @Override
    public Operand getOperand() {
        return new PhiOperand(this);
    }

    @Override
    public List<Value> getOperandValues() {
        return new ArrayList<>(basicBlockToAssignableNameMapping.values());
    }

    @Override
    public boolean replace(Value oldName, Value newName) {
        var replaced = false;
        for (BasicBlock basicBlock: basicBlockToAssignableNameMapping.keySet()) {
            var abstractName = basicBlockToAssignableNameMapping.get(basicBlock);
            if (abstractName.equals(oldName)) {
                basicBlockToAssignableNameMapping.put(basicBlock, newName);
                replaced = true;
            }
        }
        return replaced;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return null;
    }

    @Override
    public List<Value> getAllValues() {
        var allNames = getOperandValues();
        allNames.add(getDestination());
        return allNames;
    }

    @Override
    public Instruction copy() {
        return new Phi(getDestination(), basicBlockToAssignableNameMapping);
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
        return Objects.equals(basicBlockToAssignableNameMapping.values(), phi.basicBlockToAssignableNameMapping.values()) && Objects.equals(getDestination(), phi.getDestination());
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicBlockToAssignableNameMapping.values());
    }

    @Override
    public String toString() {
        String rhs = basicBlockToAssignableNameMapping.values().stream().map(Value::repr).collect(Collectors.joining(", "));
        return String.format("%s%s: %s = phi (%s)", DOUBLE_INDENT, getDestination().repr(), getDestination().getType().getSourceCode(), rhs);
    }

    @Override
    public String syntaxHighlightedToString() {
        String rhs = basicBlockToAssignableNameMapping.values().stream().map(Value::repr).collect(Collectors.joining(", "));
        return String.format("%s%s: %s = phi (%s)", DOUBLE_INDENT, getDestination().repr(), getDestination().getType().getColoredSourceCode(), rhs);
    }
}

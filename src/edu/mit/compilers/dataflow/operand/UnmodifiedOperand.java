package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Objects;

public class UnmodifiedOperand extends Operand {
    public Value value;
    public String operator;

    public UnmodifiedOperand(Value value) {
        this.value = value;
        this.operator = DecafScanner.ASSIGN;
    }

    @Override
    public boolean contains(Value comp) {
        return comp.equals(value);
    }

    @Override
    public boolean isContainedIn(StoreInstruction storeInstruction) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnmodifiedOperand that = (UnmodifiedOperand) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

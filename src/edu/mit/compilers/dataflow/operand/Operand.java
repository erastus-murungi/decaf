package edu.mit.compilers.dataflow.operand;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.GlobalAddress;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.grammar.DecafScanner;

public abstract class Operand {
    private static int indexCounter;
    private final int index;

    public Operand() {
        this.index = indexCounter++;
    }

    public static boolean operatorIsCommutative(String operator) {
        return operator.equals(DecafScanner.PLUS) || operator.equals(DecafScanner.MULTIPLY);
    }

    public abstract boolean contains(Value comp);

    public boolean containsAny(Set<GlobalAddress> names) {
        return names.stream().anyMatch(this::contains);
    }

    public int getIndex() {
        return index;
    }

    public abstract List<Value> getNames();

    public abstract boolean isContainedIn(StoreInstruction storeInstruction);

}

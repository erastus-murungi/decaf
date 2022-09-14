package edu.mit.compilers.dataflow.operand;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.IrGlobal;
import edu.mit.compilers.codegen.names.IrValue;
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

    public abstract boolean contains(IrValue comp);

    public boolean containsAny(Set<IrGlobal> names) {
        return names.stream().anyMatch(this::contains);
    }

    public int getIndex() {
        return index;
    }

    public abstract List<IrValue> getNames();

    public abstract boolean isContainedIn(StoreInstruction storeInstruction);

}

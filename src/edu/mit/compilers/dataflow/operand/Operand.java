package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Collection;

public abstract class Operand {
    private final int index;

    private static int indexCounter;

    public Operand() {
        this.index = indexCounter++;
    }

    public static boolean operatorIsCommutative(String operator) {
        return operator.equals(DecafScanner.PLUS) || operator.equals(DecafScanner.MULTIPLY);
    }

    public abstract boolean contains(AbstractName comp);

    public boolean containsAny(Collection<AbstractName> names) {
        return names.stream().anyMatch(this::contains);
    }

    public int getIndex() {
        return index;
    }

    public abstract boolean isContainedIn(StoreInstruction storeInstruction);

}

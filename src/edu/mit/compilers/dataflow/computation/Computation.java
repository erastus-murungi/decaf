package edu.mit.compilers.dataflow.computation;

import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Collection;

public abstract class Computation {
    private final int index;
    private static int indexCounter;

    public Computation() {
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

    public abstract boolean isContainedIn(HasResult hasResult);
}

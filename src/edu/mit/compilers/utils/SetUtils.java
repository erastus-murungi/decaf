package edu.mit.compilers.utils;

import java.util.HashSet;
import java.util.Set;

import edu.mit.compilers.codegen.names.Value;

public class SetUtils {
    public static Set<Value> difference(Set<Value> first, Set<Value> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.removeAll(second);
        return firstCopy;
    }

    public static Set<Value> union(Set<Value> first, Set<Value> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.addAll(second);
        return firstCopy;
    }
}

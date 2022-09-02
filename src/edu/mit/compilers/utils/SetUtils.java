package edu.mit.compilers.utils;

import java.util.HashSet;
import java.util.Set;

public class SetUtils {
    public static <T> Set<T> difference(Set<T> first, Set<T> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.removeAll(second);
        return firstCopy;
    }

    public static <T> Set<T> union(Set<T> first, Set<T> second) {
        var firstCopy = new HashSet<>(first);
        firstCopy.addAll(second);
        return firstCopy;
    }
}

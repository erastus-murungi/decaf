package edu.mit.compilers.utils;

public record Pair<T1, T2>(T1 first, T2 second) {
    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}

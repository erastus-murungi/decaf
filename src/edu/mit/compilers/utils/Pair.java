package edu.mit.compilers.utils;

import java.util.Objects;

public class Pair<T1, T2>{
    T1 first;
    T2 second;
    public Pair(T1 first, T2 second) {
       this.first = first;
       this.second = second;
    }

    public T1 first(){
        return this.first;
    }

    public T2 second(){
        return this.second;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair<?, ?> pair)) return false;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}

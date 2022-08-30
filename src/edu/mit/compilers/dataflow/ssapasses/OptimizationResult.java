package edu.mit.compilers.dataflow.ssapasses;

public class OptimizationResult<T> {
    T before;
    T after;

    public OptimizationResult(Class<?> optimizationClass, T before, T after) {
        this.before = before;
        this.after = after;
    }

    @Override
    public String toString() {
        return "OptimizationResult{" +
                "before=" + before +
                ", after=" + after +
                '}';
    }
}

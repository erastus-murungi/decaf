package edu.mit.compilers.dataflow.ssapasses;

import java.util.Objects;

// This class represents lattice values for constants.
public class LatticeElement {
    private enum LatticeElementType {
        /** Maybe a constant */
        TOP,
        /** Definitely not a constant */
        BOTTOM,
        /** Definitely a constant */
        CONSTANT,
    }

    public LatticeElementType latticeElementType;
    private final Long value;

    private LatticeElement(LatticeElementType latticeElementType, Long value) {
        this.latticeElementType = latticeElementType;
        this.value = value;
    }

    public static LatticeElement top() {
        return new LatticeElement(LatticeElementType.TOP, null);
    }

    public static LatticeElement bottom() {
        return new LatticeElement(LatticeElementType.BOTTOM, null);
    }

    public static LatticeElement constant(Long value) {
        return new LatticeElement(LatticeElementType.CONSTANT, value);
    }

    @Override
    public String toString() {
        switch (latticeElementType) {
            case TOP:
                return "⊤";
            case BOTTOM:
                return "⊥";
            default:
                return String.format("const %s", value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LatticeElement that = (LatticeElement) o;
        return latticeElementType == that.latticeElementType && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latticeElementType, value);
    }
}

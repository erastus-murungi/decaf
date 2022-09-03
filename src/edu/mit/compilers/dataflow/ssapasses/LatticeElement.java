package edu.mit.compilers.dataflow.ssapasses;

import java.util.Collection;
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
        return switch (latticeElementType) {
            case TOP -> "⊤";
            case BOTTOM -> "⊥";
            default -> String.format("const %s", value);
        };
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


    /**
     * TOP Λ x = x		∀ x
     * 	ci Λ cj = ci 		if ci = cj
     * 	ci Λ cj = BOT 	if ci ≠ cj
     * 	BOT Λ x = BOT	∀ x
     *
     * @param x a lattice element
     * @param y another lattice element
     * @return x Λ y
     */
    public static LatticeElement meet(LatticeElement x, LatticeElement y) {
        assert x != null && y != null;
        if (x.latticeElementType == LatticeElementType.TOP)
            return y;
        else if (y.latticeElementType == LatticeElementType.TOP || x.equals(y))
            return x;
        return LatticeElement.bottom();
    }

    public static LatticeElement meet(Collection<LatticeElement> xs) {
        return xs.stream().reduce(LatticeElement.top(), LatticeElement::meet);
    }

    public boolean isBottom() {
        return latticeElementType.equals(LatticeElementType.BOTTOM);
    }

    public boolean isTop() {
        return latticeElementType.equals(LatticeElementType.TOP);
    }

    public boolean isConstant() {
        return latticeElementType.equals(LatticeElementType.CONSTANT);
    }

    public Long getValue() {
        assert value != null && latticeElementType.equals(LatticeElementType.CONSTANT);
        return value;
    }
}

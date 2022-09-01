package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;

public class Label extends Instruction {
    private final Integer labelIndex;
    private String label = null;

    public String getLabel() {
        if (this.label == null)
            return "L" + labelIndex;
        return label;
    }

    public String getLabelForAsm() {
        if (this.label == null)
            return "L" + labelIndex;
        return label;
    }

    public Label(Integer labelIndex) {
        super(null);
        this.labelIndex = labelIndex;
    }

    public Label(String label) {
        super(null);
        this.label = label;
        this.labelIndex = null;
    }

    public boolean isExitLabel() {
        return getLabel().startsWith("exit");
    }

    @Override
    public String toString() {
        return String.format("%s%s:", INDENT, getLabel());
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public String syntaxHighlightedToString() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new Label(labelIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label1 = (Label) o;
        return Objects.equals(getLabel(), label1.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }
}

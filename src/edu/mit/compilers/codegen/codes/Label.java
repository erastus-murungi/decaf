package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Label extends Instruction {
    public final String label;
    public final BasicBlock cfgBlock;
    public List<String> aliasLabels;

    public Label(String label, BasicBlock cfgBlock) {
        this(cfgBlock == null || cfgBlock.lines.isEmpty() ? null : cfgBlock.lines.get(0), label, cfgBlock, new ArrayList<>());
    }

    public Label(AST source, String label, BasicBlock cfgBlock, List<String> aliasLabels) {
        super(source);
        this.cfgBlock = cfgBlock;
        this.label = label;
        this.aliasLabels = aliasLabels;
    }

    @Override
    public String toString() {
        return String.format("%s%s:%s%s", INDENT, label, DOUBLE_INDENT + DOUBLE_INDENT, aliasLabels.size() == 0 ? "" : "(" + String.join(", ", aliasLabels) + ")");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new Label(source, label, cfgBlock, aliasLabels);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Label label1 = (Label) o;
        return Objects.equals(label, label1.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}

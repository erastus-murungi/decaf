package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Label extends ThreeAddressCode {
    public final String label;
    public final BasicBlock cfgBlock;
    public List<String> aliasLabels;

    public Label(String label, BasicBlock cfgBlock) {
        super(cfgBlock == null || cfgBlock.lines.isEmpty() ? null : cfgBlock.lines.get(0));
        this.cfgBlock = cfgBlock;
        this.label = label;
        this.aliasLabels = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("%s%s:%s%s", INDENT, label, DOUBLE_INDENT + DOUBLE_INDENT, aliasLabels.size() == 0 ? "" : "(" + String.join(", ", aliasLabels) + ")");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }
}

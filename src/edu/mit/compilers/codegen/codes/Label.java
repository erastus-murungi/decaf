package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.cfg.CFGBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class Label extends ThreeAddressCode {
    public final String label;
    public final CFGBlock cfgBlock;
    public List<String> aliasLabels;

    public Label(String label, CFGBlock cfgBlock) {
        super(cfgBlock == null? null : cfgBlock.lines.get(0).ast);
        this.cfgBlock = cfgBlock;
        this.label = label;
        this.aliasLabels = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("%s%s:%s%s", INDENT, label, DOUBLE_INDENT + DOUBLE_INDENT, aliasLabels.size() == 0 ? "" : "(" + String.join(", ", aliasLabels) + ")");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}

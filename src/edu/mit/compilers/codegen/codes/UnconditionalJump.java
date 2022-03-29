package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Collections;
import java.util.List;

public class UnconditionalJump extends ThreeAddressCode {
    public final Label goToLabel;

    public UnconditionalJump(Label goToLabel) {
        super(null);
        this.goToLabel = goToLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "GoTo", goToLabel.label);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }
}

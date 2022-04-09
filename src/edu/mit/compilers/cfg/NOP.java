package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Optional;

public class NOP extends BasicBlockBranchLess {
    String nopLabel;
    public NOP() {
        super(null);
    }

    public NOP(String label) {
        super(null);
        this.nopLabel = label;
    }

    public Optional<String> getNopLabel() {
        return Optional.ofNullable(nopLabel);
    }

    public<T> T accept(BasicBlockVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    };

    @Override
    public String getLabel() {
        return String.format("NOP{%s}", getNopLabel().orElse(""));
    }
}

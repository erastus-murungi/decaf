package edu.mit.compilers.cfg;

import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NOP extends BasicBlockBranchLess {
    String nopLabel;
    public NOP() {
        super(null);
    }

    public NOP(String label) {
        super(null);
        this.nopLabel = label;
        setLabel(new Label(nopLabel));
    }

    public Optional<String> getNopLabel() {
        return Optional.ofNullable(nopLabel);
    }

    public<T> T accept(BasicBlockVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public List<BasicBlock> getSuccessors() {
        if (getSuccessor() == null)
            return Collections.emptyList();
        return List.of(getSuccessor());
    }

    @Override
    public String getLinesOfCodeString() {
        return String.format("NOP{%s}", getNopLabel().orElse(""));
    }
}

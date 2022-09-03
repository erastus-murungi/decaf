package edu.mit.compilers.cfg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.codegen.codes.Label;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NOP extends BasicBlock {
    String nopLabel;
    public NOP() {
        super(BasicBlockType.NOP);
    }

    public NOP(String label) {
        super(BasicBlockType.NOP, new Label(label));
        this.nopLabel = label;
    }

    public Optional<String> getNopLabel() {
        return Optional.ofNullable(nopLabel);
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

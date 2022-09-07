package edu.mit.compilers.cfg;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NOP extends BasicBlock {
    private String nopLabel;
    private final NOPType nopType;

    public enum NOPType {
        METHOD_ENTRY,
        METHOD_EXIT,
        NORMAL
    }

    public NOP() {
        super(BasicBlockType.NOP);
        this.nopType = NOPType.NORMAL;
    }

    public NOP(String label, NOPType nopType) {
        super(BasicBlockType.NOP);
        this.nopLabel = label;
        this.nopType = nopType;
    }

    public boolean isExitNop() {
        return nopType.equals(NOPType.METHOD_EXIT);
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
        var label = switch (nopType) {
            case METHOD_EXIT -> "exit_" + nopLabel;
            case METHOD_ENTRY -> "enter_" + nopLabel;
            default -> "";
        };
        return String.format("NOP{%s}", label);
    }

}

package edu.mit.compilers.cfg;

import java.util.Collections;
import java.util.List;

public class BasicBlockBranchLess extends BasicBlock {
    private BasicBlock successor;

    public BasicBlockBranchLess(BasicBlock successor) {
        this.successor = successor;
    }

    public BasicBlock getSuccessor() {
        return successor;
    }

    public void setSuccessor(BasicBlock successor) {
        this.successor = successor;
    }

    @Override
    public List<BasicBlock> getSuccessors() {
        if (successor == null)
            return Collections.emptyList();
        return List.of(successor);
    }

    public BasicBlockBranchLess() {
    }

    @Override
    public <T> T accept(BasicBlockVisitor<T> visitor) {
        return visitor.visit(this);
    }
}

package edu.mit.compilers.cfg;

import java.util.List;

public class CFGNonConditional extends CFGBlock {
    public CFGBlock autoChild;

    public CFGNonConditional(CFGBlock autoChild) {
        this.autoChild = autoChild;
    }

    @Override
    public List<CFGBlock> getSuccessors() {
        return List.of(autoChild);
    }

    public CFGNonConditional() {
    }
}

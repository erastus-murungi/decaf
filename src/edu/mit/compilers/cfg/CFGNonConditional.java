package edu.mit.compilers.cfg;

public class CFGNonConditional extends CFGBlock {
    public CFGBlock autoChild;

    public CFGNonConditional(CFGBlock autoChild) {
        this.autoChild = autoChild;
    }

    public CFGNonConditional() {
    }
}

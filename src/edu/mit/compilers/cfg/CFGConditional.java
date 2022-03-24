package edu.mit.compilers.cfg;

public class CFGConditional extends CFGBlock {
    public CFGBlock trueChild;
    public CFGBlock falseChild;

    public CFGExpression condition;

    public CFGConditional(CFGBlock trueChild, CFGBlock falseChild, CFGExpression condition) {
        this.trueChild = trueChild;
        this.falseChild = falseChild;
        this.condition = condition;
    }

    public CFGConditional(CFGExpression condition) {
        this.condition = condition;
    }
}

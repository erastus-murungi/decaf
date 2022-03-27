package edu.mit.compilers.cfg;

public class CFGConditional extends CFGBlock {
    public CFGBlock trueChild;
    public CFGBlock falseChild;

    public CFGExpression condition;

    public CFGConditional(CFGExpression condition, CFGBlock trueChild, CFGBlock falseChild) {
        this.trueChild = trueChild;
        this.falseChild = falseChild;
        this.condition = condition;
        lines.add(this.condition);
    }

    public CFGConditional(CFGExpression condition) {
        this.condition = condition;
        lines.add(this.condition);
    }
}

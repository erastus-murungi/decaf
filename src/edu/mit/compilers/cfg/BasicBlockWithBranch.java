package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.Expression;

import java.util.List;

public class BasicBlockWithBranch extends BasicBlock {
    private BasicBlock trueTarget;
    private BasicBlock falseTarget;

    private final Expression branchCondition;

    public Expression getBranchCondition() {
        return branchCondition;
    }

    public BasicBlockWithBranch(Expression branchCondition, BasicBlock trueTarget, BasicBlock falseTarget) {
        this.setTrueTarget(trueTarget);
        this.setFalseTarget(falseTarget);
        this.branchCondition = branchCondition;
        addAstNode(branchCondition);
    }

    public BasicBlockWithBranch(Expression condition) {
        this(condition, null, null);
    }

    @Override
    public List<BasicBlock> getSuccessors() {
        return List.of(getTrueTarget(), getFalseTarget());
    }

    @Override
    public <T> T accept(BasicBlockVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public BasicBlock getTrueTarget() {
        return trueTarget;
    }

    public void setTrueTarget(BasicBlock trueTarget) {
        this.trueTarget = trueTarget;
    }

    public BasicBlock getFalseTarget() {
        return falseTarget;
    }

    public void setFalseTarget(BasicBlock falseTarget) {
        this.falseTarget = falseTarget;
    }
}

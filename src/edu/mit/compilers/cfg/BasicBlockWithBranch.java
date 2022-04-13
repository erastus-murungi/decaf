package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.Expression;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.List;

public class BasicBlockWithBranch extends BasicBlock {
    public BasicBlock trueChild;
    public BasicBlock falseChild;

    public Expression condition;

    public BasicBlockWithBranch(Expression condition, BasicBlock trueChild, BasicBlock falseChild) {
        this.trueChild = trueChild;
        this.falseChild = falseChild;
        this.condition = condition;
        lines.add(this.condition);
    }

    public BasicBlockWithBranch(Expression condition) {
        this.condition = condition;
        lines.add(this.condition);
    }

    @Override
    public List<BasicBlock> getSuccessors() {
        return List.of(trueChild, falseChild);
    }

    @Override
    public <T> T accept(BasicBlockVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    }
}

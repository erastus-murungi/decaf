package edu.mit.compilers.cfg;

public interface BasicBlockVisitor<T> {

    T visit(BasicBlockBranchLess basicBlockBranchLess);

    T visit(BasicBlockWithBranch basicBlockWithBranch);

    T visit(NOP nop);
}

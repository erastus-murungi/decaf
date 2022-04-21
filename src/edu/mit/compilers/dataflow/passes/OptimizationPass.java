package edu.mit.compilers.dataflow.passes;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;

public abstract class OptimizationPass {
    Set<AbstractName> globalVariables;
    BasicBlock entryBlock;
    List<BasicBlock> basicBlocks;

    public OptimizationPass(Set<AbstractName> globalVariables, BasicBlock entryBlock) {
        this.globalVariables = globalVariables;
        this.entryBlock = entryBlock;
        this.basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    // return true if changes happened
    public abstract boolean run();
}

package edu.mit.compilers.dataflow.passes;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.grammar.DecafScanner;

public abstract class OptimizationPass {
    Set<AbstractName> globalVariables;
    BasicBlock entryBlock;
    MethodBegin methodBegin;
    List<BasicBlock> basicBlocks;

    public MethodBegin getMethod() {
        return methodBegin;
    }

    public OptimizationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        this.globalVariables = globalVariables;
        this.methodBegin = methodBegin;
        this.entryBlock = methodBegin.entryBlock;
        this.basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    // return true if changes happened
    public abstract boolean run();

    // return whether an instruction of the form x = x
    public static boolean isTrivialAssignment(Instruction instruction) {
        if (instruction instanceof Assign) {
            var assign = (Assign) instruction;
            return assign.store.equals(assign.operand);
        }
        return false;
    }
}

package edu.mit.compilers.dataflow.passes;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;

public abstract class OptimizationPass {
    Set<AbstractName> globalVariables;
    BasicBlock entryBlock;
    Method method;
    List<BasicBlock> basicBlocks;

    public Method getMethod() {
        return method;
    }

    public OptimizationPass(Set<AbstractName> globalVariables, Method method) {
        this.globalVariables = globalVariables;
        this.method = method;
        this.entryBlock = method.entryBlock;
        this.basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    // return true if changes happened
    public abstract boolean run();

    // return whether an instruction of the form x = x
    public static boolean isTrivialAssignment(Instruction instruction) {
        if (instruction instanceof Assign) {
            var assign = (Assign) instruction;
            return assign.getStore().equals(assign.operand);
        }
        return false;
    }
}

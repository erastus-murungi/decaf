package edu.mit.compilers.dataflow.passes;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.utils.TarjanSCC;

public abstract class OptimizationPass {
    Set<LValue> globalVariables;
    BasicBlock entryBlock;
    Method method;
    List<BasicBlock> basicBlocks;

    public Method getMethod() {
        return method;
    }

    public OptimizationPass(Set<LValue> globalVariables, Method method) {
        this.globalVariables = globalVariables;
        this.method = method;
        this.entryBlock = method.entryBlock;
        this.basicBlocks = TarjanSCC.getReversePostOrder(entryBlock);
    }

    // return true if changes happened
    public abstract boolean runFunctionPass();

    // return whether an instruction of the form x = x
    public static boolean isTrivialAssignment(Instruction instruction) {
        if (instruction instanceof CopyInstruction) {
            var assign = (CopyInstruction) instruction;
            return assign.getDestination().equals(assign.getValue());
        }
        return false;
    }
}

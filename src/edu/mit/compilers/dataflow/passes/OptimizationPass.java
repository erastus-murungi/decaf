package edu.mit.compilers.dataflow.passes;

import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.GlobalAddress;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.dataflow.OptimizationContext;

public abstract class OptimizationPass {
    protected BasicBlock entryBlock;
    protected Method method;
    protected final OptimizationContext optimizationContext;

    public OptimizationPass(OptimizationContext optimizationContext, Method method) {
        this.optimizationContext = optimizationContext;
        this.method = method;
        this.entryBlock = method.entryBlock;
    }

    // return whether an instruction of the form x = x
    public static boolean isTrivialAssignment(Instruction instruction) {
        if (instruction instanceof CopyInstruction assign) {
            return assign.getDestination().equals(assign.getValue());
        }
        return false;
    }

    public Method getMethod() {
        return method;
    }

    public Set<GlobalAddress> globals() {
        return optimizationContext.globals();
    }

    public List<BasicBlock> getBasicBlocksList() {
        return optimizationContext.getBasicBlocks(method);
    }

    // return true if changes happened
    public abstract boolean runFunctionPass();
}

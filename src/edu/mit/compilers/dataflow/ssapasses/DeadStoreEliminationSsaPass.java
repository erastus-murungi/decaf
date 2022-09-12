package edu.mit.compilers.dataflow.ssapasses;

import java.util.HashSet;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.GlobalAddress;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.OptimizationContext;

public class DeadStoreEliminationSsaPass extends SsaOptimizationPass<Void> {
    public DeadStoreEliminationSsaPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    @Override
    public boolean runFunctionPass() {
        // look at the whole program and find all the uses
        var changesHappened = false;
        var used = new HashSet<Value>();

        for (BasicBlock basicBlock : getBasicBlocksList()) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand hasOperand) {
                    used.addAll(hasOperand.getOperandLValues());
                }
            }
        }
        for (BasicBlock basicBlock : getBasicBlocksList()) {
            var deadStores = basicBlock.getStoreInstructions()
                                       .stream()
                                       .filter(storeInstruction -> storeInstructionIsDead(storeInstruction, used))
                                       .toList();
            basicBlock.getInstructionList()
                      .removeAll(deadStores);
        }
        return changesHappened;
    }

    private static boolean storeInstructionIsDead(StoreInstruction storeInstruction, Set<Value> usedValues) {
        if (storeInstruction.getDestination() instanceof GlobalAddress || storeInstruction.getDestination() instanceof MemoryAddress) {
            return false;
        }
        if (storeInstruction instanceof CopyInstruction || storeInstruction instanceof UnaryInstruction || storeInstruction instanceof BinaryInstruction) {
            return !usedValues.contains(storeInstruction.getDestination());
        }
        return false;
    }
}

package decaf.dataflow.ssapasses;

import java.util.HashSet;
import java.util.Set;

import decaf.codegen.codes.HasOperand;
import decaf.codegen.names.IrMemoryAddress;
import decaf.cfg.BasicBlock;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrGlobal;
import decaf.codegen.names.IrValue;
import decaf.dataflow.OptimizationContext;

public class DeadStoreEliminationSsaPass extends SsaOptimizationPass {
    public DeadStoreEliminationSsaPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }
    @Override
    protected void resetForPass() {
    }

    @Override
    public boolean runFunctionPass() {
        // look at the whole program and find all the uses
        var changesHappened = false;
        var used = new HashSet<IrValue>();

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

    private static boolean storeInstructionIsDead(StoreInstruction storeInstruction, Set<IrValue> usedIrValues) {
        if (storeInstruction.getDestination() instanceof IrGlobal || storeInstruction.getDestination() instanceof IrMemoryAddress) {
            return false;
        }
        if (storeInstruction instanceof CopyInstruction || storeInstruction instanceof UnaryInstruction || storeInstruction instanceof BinaryInstruction) {
            return !usedIrValues.contains(storeInstruction.getDestination());
        }
        return false;
    }
}

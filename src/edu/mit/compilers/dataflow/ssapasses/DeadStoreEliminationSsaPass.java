package edu.mit.compilers.dataflow.ssapasses;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.AllocateInstruction;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;

public class DeadStoreEliminationSsaPass extends SsaOptimizationPass<Void> {
    public DeadStoreEliminationSsaPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    @Override
    public boolean runFunctionPass() {
        // look at the whole program and find all the uses
        var changesHappened = false;
        Set<Value> used = new HashSet<>();
        for (BasicBlock basicBlock : getBasicBlockList()) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand) {
                    var hasOperand = (HasOperand) instruction;
                    used.addAll(hasOperand.getLValues());
                }
            }
        }
        for (BasicBlock basicBlock : getBasicBlockList()) {
            var deadStores = basicBlock.getStores()
                                       .stream()
                                       .filter(storeInstruction -> !used.contains(storeInstruction.getStore()) && (storeInstruction instanceof CopyInstruction || storeInstruction instanceof UnaryInstruction || storeInstruction instanceof BinaryInstruction || storeInstruction instanceof AllocateInstruction))
                                       .collect(Collectors.toUnmodifiableList());
            basicBlock.getInstructionList()
                      .removeAll(deadStores);
        }
        return changesHappened;
    }
}

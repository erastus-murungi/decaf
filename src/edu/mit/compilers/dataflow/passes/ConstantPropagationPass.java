package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.analyses.ReachingDefinitions;

public class ConstantPropagationPass extends OptimizationPass {
    public ConstantPropagationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private List<HasOperand> reachingDefinitions(int indexOfDefinition, InstructionList instructionList) {
        var rds = new ArrayList<HasOperand>();
        var store = ((Store) instructionList.get(indexOfDefinition)).getStore();
        for (int indexOfInstruction = indexOfDefinition + 1; indexOfInstruction < instructionList.size(); indexOfInstruction++) {
            var instruction = instructionList.get(indexOfInstruction);
            if (instruction instanceof Store) {
                if (((Store) instruction).getStore()
                                         .equals(store))
                    break;
            } else {
                if (instruction instanceof HasOperand)
                    rds.add((HasOperand) instruction);
            }
        }
        return rds;
    }


    public void runLocalConstantPropagation(BasicBlock basicBlock) {
        int indexOfInstruction = -1;
        for (Instruction instruction : basicBlock.getInstructionList()) {
            indexOfInstruction++;
            if (instruction instanceof Assign) {
                if (((Assign) instruction).operand instanceof ConstantName) {
                    ConstantName constant = (ConstantName) ((Assign) instruction).operand;
                    for (HasOperand hasOperand : reachingDefinitions(indexOfInstruction, basicBlock.getInstructionList())) {
                        hasOperand.replace(((Assign) instruction).store, constant);
                    }
                }
            }
        }
    }

    private static Map<AbstractName, ConstantName> getStoreToConstantMapping(Collection<Store> storeInstructions) {
        var storeNameToStoreInstructionMap = new HashMap<AbstractName, ConstantName>();
        for (Store storeInstruction : storeInstructions) {
            if (storeInstruction instanceof Assign) {
                var assignment = (Assign) storeInstruction;
                if (assignment.operand instanceof ConstantName) {
                    storeNameToStoreInstructionMap.put(storeInstruction.getStore(), (ConstantName) assignment.operand);
                }
            }
        }
        return storeNameToStoreInstructionMap;
    }

    public void runGlobalConstantPropagation() {
        final ReachingDefinitions reachingDefinitions = new ReachingDefinitions(entryBlock);
        for (BasicBlock basicBlock : basicBlocks) {
            runLocalConstantPropagation(basicBlock);
            var storeToConstantMapping = getStoreToConstantMapping(reachingDefinitions.in.get(basicBlock));
            if (storeToConstantMapping.isEmpty())
                continue;
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand) {
                    HasOperand hasOperand = (HasOperand) instruction;
                    for (AbstractName name : hasOperand.getOperandNames()) {
                        if (storeToConstantMapping.containsKey(name)) {
                            hasOperand.replace(name, storeToConstantMapping.get(name));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        runGlobalConstantPropagation();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}

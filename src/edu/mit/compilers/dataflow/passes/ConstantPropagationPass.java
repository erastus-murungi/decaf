package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.dataflow.analyses.ReachingDefinitions;

public class ConstantPropagationPass extends OptimizationPass {
    public ConstantPropagationPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private List<HasOperand> reachingDefinitions(int indexOfDefinition, InstructionList instructionList) {
        var rds = new ArrayList<HasOperand>();
        var store = ((StoreInstruction) instructionList.get(indexOfDefinition)).getDestination();
        for (int indexOfInstruction = indexOfDefinition + 1; indexOfInstruction < instructionList.size(); indexOfInstruction++) {
            var instruction = instructionList.get(indexOfInstruction);
            if (instruction instanceof StoreInstruction) {
                if (((StoreInstruction) instruction).getDestination()
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
            if (instruction instanceof CopyInstruction) {
                if (((CopyInstruction) instruction).getValue() instanceof NumericalConstant) {
                    NumericalConstant constant = (NumericalConstant) ((CopyInstruction) instruction).getValue();
                    for (HasOperand hasOperand : reachingDefinitions(indexOfInstruction, basicBlock.getInstructionList())) {
                        hasOperand.replace(((CopyInstruction) instruction).getDestination(), constant);
                    }
                }
            }
        }
    }

    private static Map<Value, NumericalConstant> getStoreToConstantMapping(Collection<StoreInstruction> storeInstructionInstructions) {
        var storeNameToStoreInstructionMap = new HashMap<Value, NumericalConstant>();
        for (StoreInstruction storeInstruction : storeInstructionInstructions) {
            if (storeInstruction instanceof CopyInstruction) {
                var assignment = (CopyInstruction) storeInstruction;
                if (assignment.getValue() instanceof NumericalConstant) {
                    storeNameToStoreInstructionMap.put(storeInstruction.getDestination(), (NumericalConstant) assignment.getValue());
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
                    for (Value name : hasOperand.getOperandValues()) {
                        if (storeToConstantMapping.containsKey(name)) {
                            hasOperand.replace(name, storeToConstantMapping.get(name));
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean runFunctionPass() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        runGlobalConstantPropagation();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}

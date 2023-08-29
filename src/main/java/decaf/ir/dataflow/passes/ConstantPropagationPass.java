package decaf.ir.dataflow.passes;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import decaf.ir.InstructionList;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.dataflow.analyses.ReachingDefinitions;
import decaf.ir.names.IrIntegerConstant;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;

public class ConstantPropagationPass extends OptimizationPass {
  public ConstantPropagationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  private static Map<IrValue, IrIntegerConstant> getStoreToConstantMapping(Collection<StoreInstruction> storeInstructionInstructions) {
    var storeNameToStoreInstructionMap = new HashMap<IrValue, IrIntegerConstant>();
    for (StoreInstruction storeInstruction : storeInstructionInstructions) {
      if (storeInstruction instanceof CopyInstruction assignment) {
        if (assignment.getValue() instanceof IrIntegerConstant) {
          storeNameToStoreInstructionMap.put(
              storeInstruction.getDestination(),
              (IrIntegerConstant) assignment.getValue()
          );
        }
      }
    }
    return storeNameToStoreInstructionMap;
  }

  private List<HasOperand> reachingDefinitions(
      int indexOfDefinition,
      InstructionList instructionList
  ) {
    var rds = new ArrayList<HasOperand>();
    var store = ((StoreInstruction) instructionList.get(indexOfDefinition)).getDestination();
    for (int indexOfInstruction = indexOfDefinition + 1;
         indexOfInstruction < instructionList.size(); indexOfInstruction++) {
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
        if (((CopyInstruction) instruction).getValue() instanceof IrIntegerConstant constant) {
          for (HasOperand hasOperand : reachingDefinitions(
              indexOfInstruction,
              basicBlock.getInstructionList()
          )) {
            hasOperand.replaceValue(
                ((CopyInstruction) instruction).getDestination(),
                constant
            );
          }
        }
      }
    }
  }

  public void runGlobalConstantPropagation() {
    final ReachingDefinitions reachingDefinitions = new ReachingDefinitions(entryBlock);
    for (BasicBlock basicBlock : getBasicBlocksList()) {
      runLocalConstantPropagation(basicBlock);
      var storeToConstantMapping = getStoreToConstantMapping(reachingDefinitions.in.get(basicBlock));
      if (storeToConstantMapping.isEmpty())
        continue;
      for (Instruction instruction : basicBlock.getInstructionList()) {
        if (instruction instanceof HasOperand hasOperand) {
          for (IrValue name : hasOperand.genOperandIrValuesFiltered(IrSsaRegister.class)) {
            if (storeToConstantMapping.containsKey(name)) {
              hasOperand.replaceValue(
                  name,
                  storeToConstantMapping.get(name)
              );
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

package edu.mit.compilers.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.Value;

public class SccpSsaPass extends SsaOptimizationPass<Void> {
    public SccpSsaPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private void substituteVariablesWithConstants(Map<Value, LatticeElement> latticeValues) {
        for (BasicBlock basicBlock : getBasicBlockList()) {
            var instructionList = new ArrayList<Instruction>();
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction || instruction instanceof CopyInstruction) {
                    var dest = ((StoreInstruction) instruction).getDestination();
                    var latticeValue = latticeValues.get(dest);
                    if (latticeValue.isConstant()) {
                        instructionList.add(CopyInstruction.noAstConstructor(dest, new NumericalConstant(latticeValue.getValue(), dest.getType())));
                        continue;
                    }
                }
                instructionList.add(instruction);
            }
            basicBlock.getInstructionList()
                      .reset(instructionList);

        }
    }

    private void removeUnreachableBasicBlocks(Set<BasicBlock> basicBlocks) {
        for (BasicBlock basicBlock : basicBlocks) {
        }

    }


    @Override
    public boolean runFunctionPass() {
        var sccp = new SCCP(method.entryBlock);
        substituteVariablesWithConstants(sccp.getLatticeValues());
        removeUnreachableBasicBlocks(sccp.getReachableBasicBlocks());
        return false;
    }
}

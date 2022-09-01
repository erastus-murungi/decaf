package edu.mit.compilers.dataflow.ssapasses;

import java.util.ArrayList;
import java.util.Set;

import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ssa.Phi;


/**
 * The redundant PHI instruction is defined as follows:
 * 1) x=phi(x,x,x) (remove only)
 * 2) x=phi(y,y,y) (regard as `x=y` and do copy propagation)
 * 3) x=phi(y,y,x) (regard as `x=y` and do copy propagation)
 */
public class RedundantPhiEliminationPass extends SsaOptimizationPass<Void> {
    public RedundantPhiEliminationPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }

    private boolean performRedundantPhiElimination () {
        var changesHappened = false;
        for (var basicBlock : getBasicBlockList()) {
            if (basicBlock.phiPresent()) {
                var instructionList = new ArrayList<Instruction>();
                for (var instruction : basicBlock.getInstructionList()) {
                    if (instruction instanceof Phi phi) {
                         // x=phi(x,x,x) (remove only)
                        if (phi.getOperandLValues().stream().allMatch(lValue -> lValue.equals(phi.getStore()))) {
                            changesHappened = true;
                            continue;
                        }
                        // 2) x=phi(y,y,y) (regard as `x=y` and do copy propagation)
                        if (phi.getOperandLValues().stream().distinct().count() == 1) {
                            var y = phi.getOperandLValues().stream().findFirst().orElseThrow();
                            instructionList.add(CopyInstruction.noMetaData(phi.getStore(), y));
                            changesHappened = true;
                            continue;
                        }
                        // 3) x=phi(y,y,x) (regard as `x=y` and do copy propagation)
                        if (phi.getOperandLValues().stream().filter(value -> value.equals(phi.getStore())).count() == 1) {
                            var y = phi.getOperandLValues().stream().filter(value -> !value.equals(phi.getStore())).findFirst().orElseThrow();
                            instructionList.add(CopyInstruction.noMetaData(phi.getStore(), y));
                            changesHappened = true;
                            continue;
                        }
                    }
                    instructionList.add(instruction);
                }
                basicBlock.getInstructionList().reset(instructionList);
            }
        }
        return changesHappened;
    }

    @Override
    public boolean runFunctionPass() {
        return performRedundantPhiElimination();
    }
}

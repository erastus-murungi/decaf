package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;

public class PeepHoleOptimizationPass extends OptimizationPass {
    public PeepHoleOptimizationPass(Set<LValue> globalVariables, Method method) {
        super(globalVariables, method);
    }


    private void removeTrivialBoundsChecks() {
        for (var basicBlock : basicBlocks) {
            var indicesToRemove = new ArrayList<Integer>();
            for (var indexOfInstruction = 0; indexOfInstruction < basicBlock.getInstructionList().size(); indexOfInstruction++) {
                var instruction = basicBlock.getInstructionList().get(indexOfInstruction);
                if (instruction instanceof ArrayBoundsCheck arrayBoundsCheck) {
                    if (arrayBoundsCheck.getAddress.getIndex() instanceof NumericalConstant) {
                        var index = Long.parseLong(arrayBoundsCheck.getAddress.getIndex().getLabel());
                        var length = Long.parseLong(arrayBoundsCheck.getAddress.getLength()
                                .orElseThrow().getLabel());
                        if (index >= 0 && index < length) {
                            indicesToRemove.add(indexOfInstruction);
                        }
                    }
                }
            }

            for (var index : indicesToRemove) {
                basicBlock.getInstructionList()
                        .set(index, null);
            }
            basicBlock.getInstructionList().reset(basicBlock.getCopyOfInstructionList()
                    .stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }
    }


    @Override
    public boolean runFunctionPass() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        removeTrivialBoundsChecks();
        return !oldCodes.equals(entryBlock.getInstructionList());
    }
}

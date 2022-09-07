package edu.mit.compilers.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.dataflow.dominator.DominatorTree;
import edu.mit.compilers.ssa.SSA;

public class CopyPropagationSsaPass extends SsaOptimizationPass<HasOperand> {
    List<SSACopyOptResult> resultList = new ArrayList<>();

    public CopyPropagationSsaPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    private boolean performGlobalCopyPropagation() {
        var changesHappened = false;
        var dom = new DominatorTree(getMethod().entryBlock);
        // maps (toBeReplaced -> replacer)
        var copiesMap = new HashMap<LValue, Value>();

        for (BasicBlock basicBlock : getBasicBlocksList()) {
            for (StoreInstruction storeInstruction : basicBlock.getStoreInstructions()) {
                if (storeInstruction instanceof CopyInstruction copyInstruction) {
                    var replacer = copyInstruction.getValue();
                    var toBeReplaced = copyInstruction.getDestination();
                    checkState(!copiesMap.containsKey(toBeReplaced), "CopyPropagation : Invalid SSA form: " + toBeReplaced + " found twice in program");
                    copiesMap.put(toBeReplaced, replacer);
                    // as a quick optimization, we could delete this storeInstruction here
                }
            }
        }

        for (BasicBlock basicBlock : dom.preorder()) {
            for (Instruction instruction : basicBlock.getNonPhiInstructions()) {
                if (instruction instanceof HasOperand hasOperand) {
                    for (LValue toBeReplaced : hasOperand.getOperandLValues()) {
                        if (copiesMap.containsKey(toBeReplaced)) {
                            var before = hasOperand.copy();
                            var replacer = copiesMap.get(toBeReplaced);
                            // we have to do this in a while loop because of how copy replacements propagate
                            // for instance, lets imagine we have a = k
                            // it possible that our copies map has y `replaces` k, and x `replaces` y and $0 `replaces` x
                            // we want to eventually propagate so that a = $0
                            while (replacer instanceof LValue && copiesMap.containsKey((LValue) replacer)) {
                                replacer = copiesMap.get(replacer);
                            }
                            hasOperand.replaceValue(toBeReplaced, replacer);
                            resultList.add(new SSACopyOptResult(before, instruction, toBeReplaced, replacer));
                            changesHappened = true;
                        }
                    }
                }
            }
        }
        resultList.forEach(System.out::println);
        return changesHappened;
    }

    @Override
    public boolean runFunctionPass() {
        resultList.clear();
        var changesHappened = performGlobalCopyPropagation();
        SSA.verify(method);
        return changesHappened;
    }
}

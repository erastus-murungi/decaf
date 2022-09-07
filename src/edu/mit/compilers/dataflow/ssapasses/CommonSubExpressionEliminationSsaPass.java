package edu.mit.compilers.dataflow.ssapasses;

import java.util.HashMap;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.OptimizationContext;
import edu.mit.compilers.dataflow.dominator.DominatorTree;
import edu.mit.compilers.dataflow.operand.Operand;

public class CommonSubExpressionEliminationSsaPass extends SsaOptimizationPass<StoreInstruction> {

    public CommonSubExpressionEliminationSsaPass(OptimizationContext optimizationContext, Method method) {
        super(optimizationContext, method);
    }

    @Override
    public boolean runFunctionPass() {
        return performGlobalCSE();
    }


    private void performReplacement(StoreInstruction storeInstruction,
                                    InstructionList instructionList,
                                    LValue expressionStoreLocation,
                                    Integer indexOfInstructionToBeReplaced) {

        var replacer = CopyInstruction.noMetaData(storeInstruction.getDestination(), expressionStoreLocation);
        instructionList.replaceIfContainsInstructionAtIndex(indexOfInstructionToBeReplaced, storeInstruction, replacer);
    }


    public boolean performGlobalCSE() {
        var dom = new DominatorTree(getMethod().entryBlock);
        var expressionToBasicBlock = new HashMap<Operand, BasicBlock>();
        var expressionToIndexInBasicBlock = new HashMap<Operand, Integer>();
        var changesHappened = false;

        for (BasicBlock basicBlock : dom.preorder()) {
            var instructionList = basicBlock.getInstructionList();
            for (int indexOfInstruction = 0; indexOfInstruction < instructionList.size(); indexOfInstruction++) {
                Instruction instruction = instructionList.get(indexOfInstruction);
                if (instruction instanceof BinaryInstruction || instruction instanceof UnaryInstruction) {
                    // check to see whether this expression has already been cached
                    StoreInstruction storeInstruction = (StoreInstruction) instruction;
                    var cachableExpression = storeInstruction.getOperand();
                    if (expressionToBasicBlock.containsKey(cachableExpression)) {
                        BasicBlock X = expressionToBasicBlock.get(cachableExpression);
                        StoreInstruction Y = (StoreInstruction) X.getInstructionList()
                                .get(expressionToIndexInBasicBlock.get(cachableExpression));
                        // do the replacement
                        performReplacement(storeInstruction, instructionList, Y.getDestination(), indexOfInstruction);
                        changesHappened = true;
                    } else {
                        expressionToBasicBlock.put(cachableExpression, basicBlock);
                        expressionToIndexInBasicBlock.put(cachableExpression, indexOfInstruction);
                    }
                }
            }
        }
        return changesHappened;
    }
}

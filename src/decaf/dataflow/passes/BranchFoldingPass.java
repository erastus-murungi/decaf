package decaf.dataflow.passes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import decaf.cfg.NOP;
import decaf.common.StronglyConnectedComponentsTarjan;
import decaf.ast.AST;
import decaf.cfg.BasicBlock;

public class BranchFoldingPass {
    public static void run(Collection<BasicBlock> basicBlocks) {
        for (BasicBlock entryBlock : basicBlocks) {
            StronglyConnectedComponentsTarjan.correctPredecessors(entryBlock);
            mergeTails(StronglyConnectedComponentsTarjan.getReversePostOrder(entryBlock));
        }
    }

    private static boolean allAtIndexEqual(List<ArrayList<AST>> instructionLists, int indexOfInstruction) {
        return instructionLists.stream()
                .map(instructionList -> instructionList.get(indexOfInstruction))
                .map(AST::getSourceCode)
                .distinct()
                .count() == 1;
    }

    private static boolean haveSameSuccessor(List<BasicBlock> basicBlocks) {
        return basicBlocks.stream()
                .map(BasicBlock::getSuccessors)
                .distinct()
                .count() == 1;
    }

    private static void mergeTails(List<BasicBlock> basicBlocks) {
        for (BasicBlock basicBlock : basicBlocks) {
            if (basicBlock.getPredecessors().size() > 1 && haveSameSuccessor(basicBlock.getPredecessors())) {
                var commonInstructions = getCommonInstructions(basicBlock.getPredecessors());
                if (commonInstructions.isPresent()) {
                    var common = BasicBlock.noBranch();
                    common.addAstNodes(commonInstructions.get());
                    realignPointers(basicBlock.getPredecessors(), common, basicBlock);
                }
            }
        }
    }


    private static void realignPointers(List<BasicBlock> basicBlocks, BasicBlock common, BasicBlock successor) {
        assert haveSameSuccessor(basicBlocks);
        for (BasicBlock basicBlock : basicBlocks) {
            // remove the instructions
            basicBlock.removeAstNodes(common.getAstNodes());
            basicBlock.setSuccessor(common);
            common.addPredecessor(basicBlock);
        }
        successor.clearPredecessors();
        common.clearPredecessors();
        successor.addPredecessor(common);
        common.setSuccessor(successor);
    }


    private static Optional<List<AST>> getCommonInstructions(List<BasicBlock> basicBlocks) {
        List<AST> commonTailInstructions = new ArrayList<>();
        var minSize = basicBlocks.stream()
                .map(basicBlock -> basicBlock.getAstNodes().size())
                .mapToInt(Integer::intValue)
                .min();
        var collected = basicBlocks.stream()
                .map(basicBlock -> new ArrayList<>(basicBlock.getAstNodes()))
                .toList();
        collected.forEach(Collections::reverse);

        if (minSize.isPresent()) {
            var minSizeInt = minSize.getAsInt();
            for (int indexOfInstruction = 0; indexOfInstruction < minSizeInt; indexOfInstruction++) {
                if (allAtIndexEqual(collected, indexOfInstruction)) {
                    commonTailInstructions.add(collected.get(0).get(indexOfInstruction));
                }
            }
        }
        if (commonTailInstructions.isEmpty())
            return Optional.empty();
        Collections.reverse(commonTailInstructions);
        return Optional.of(commonTailInstructions);
    }
}

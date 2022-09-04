package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.dataflow.passes.BranchFoldingPass;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.utils.Utils;

import java.util.*;

public class ControlFlowGraph {
    public AST rootNode;
    public GlobalDescriptor globalDescriptor;
    public List<DecafException> errors = new ArrayList<>();

    public ControlFlowGraph(AST ast, GlobalDescriptor globalDescriptor) {
        this.rootNode = ast;
        this.globalDescriptor = globalDescriptor;
    }

    private void catchFalloutError(NOP exitNop, MethodDescriptor methodDescriptor) {
        final MethodDefinition methodDefinition = methodDescriptor.methodDefinition;
        if (methodDescriptor.type == Type.Void)
            return;

        exitNop.getPredecessors().removeIf(block -> !(block
                .getSuccessors()
                .contains(exitNop)));

        final List<BasicBlock> allExecutionPathsReturn = exitNop.getPredecessors()
                                                                .stream()
                                                                .filter(cfgBlock -> (!cfgBlock.getAstNodes()
                                                                                              .isEmpty() && (cfgBlock.lastASTLine() instanceof Return)))
                                                                .toList();

        final List<BasicBlock> allExecutionsPathsThatDontReturn = exitNop.getPredecessors()
                                                                         .stream()
                                                                         .filter(cfgBlock -> (cfgBlock.getAstNodes()
                                                                                                      .isEmpty() || (!(cfgBlock.lastASTLine() instanceof Return))))
                                                                         .toList();

        if (allExecutionPathsReturn.size() != allExecutionsPathsThatDontReturn.size()) {
            errors.addAll(allExecutionsPathsThatDontReturn
                    .stream()
                    .map(cfgBlock -> new DecafException(methodDefinition.tokenPosition,
                            methodDefinition.methodName.getLabel() + "'s execution path ends with" +
                                    (cfgBlock.getAstNodes()
                                             .isEmpty() ? "" : (cfgBlock
                                            .lastASTLine()
                                            .getSourceCode())) + " instead of a return statement"))
                    .toList());
        }
        if (allExecutionPathsReturn.isEmpty()) {
            errors.add(new DecafException(
                    methodDefinition.tokenPosition,
                    methodDefinition.methodName.getLabel() + " method does not return expected type " + methodDefinition.returnType));
        }
    }

    public ControlFlowGraphVisitor build() {
        final ControlFlowGraphVisitor visitor = new ControlFlowGraphVisitor();
        final MaximalBasicBlocksUtil maximalVisitor = new MaximalBasicBlocksUtil();
        final NOPRemovalUtil NOPRemovalUtil = new NOPRemovalUtil();

        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);

        visitor.methodCFGBlocks.forEach((k, v) -> {
            NOPRemovalUtil.exit = visitor.methodToExitNOP.get(k);
            assert v.getSuccessor() != null;
            NOPRemovalUtil.visit(v.getSuccessor());
        });

        visitor.methodCFGBlocks.forEach((k, v) -> {
            maximalVisitor.exitNOP = visitor.methodToExitNOP.get(k);
            maximalVisitor.visit(v);
            catchFalloutError(maximalVisitor.exitNOP, (MethodDescriptor) globalDescriptor.methodsSymbolTable
                    .getDescriptorFromValidScopes(k)
                    .orElseThrow());
        });
        NOPRemovalUtil.exit = (NOP) visitor.initialGlobalBlock.getSuccessor();
        NOPRemovalUtil.visit(visitor.initialGlobalBlock);
        HashMap<String, BasicBlock> methodBlocksCFG = new HashMap<>();
        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (v
                    .getLinesOfCodeString()
                    .isBlank()) {
                if (v.getSuccessor() != null) {
                    v.getSuccessor().removePredecessor(v);
                    v = v.getSuccessor();
                }
            }
            methodBlocksCFG.put(k, v);
        });
        visitor.methodCFGBlocks = methodBlocksCFG;
        maximalVisitor.exitNOP = (NOP) visitor.initialGlobalBlock.getSuccessor();
        maximalVisitor.visit(visitor.initialGlobalBlock);
        BranchFoldingPass.run(methodBlocksCFG.values());
        return visitor;
    }

    public static void printAllParents(BasicBlock block) {
        Stack<BasicBlock> toVisit = new Stack<>();
        toVisit.add(block);
        Set<BasicBlock> seen = new HashSet<>();
        while (!toVisit.isEmpty()) {
            block = toVisit.pop();
            if (seen.contains(block) || block == null) {
                continue;
            }
            seen.add(block);
            System.out.println(Utils.coloredPrint(block.getLinesOfCodeString(), Utils.ANSIColorConstants.ANSI_PURPLE) + "  >>>>   ");
            for (BasicBlock parent : block.getPredecessors()) {
                System.out.println(Utils.indentBlock(parent.getLinesOfCodeString()));
                System.out.println();
            }
            if (block.hasBranch()) {
                toVisit.add(block.getFalseTarget());
                toVisit.add(block.getTrueTarget());
            } else {
                toVisit.add(block.getSuccessor());
            }
        }
    }

    public static void printAllChildren(BasicBlock block) {
        Stack<BasicBlock> toVisit = new Stack<>();
        toVisit.add(block);
        Set<BasicBlock> seen = new HashSet<>();
        while (!toVisit.isEmpty()) {
            block = toVisit.pop();
            if (seen.contains(block) || block == null) {
                continue;
            }
            seen.add(block);
            System.out.println(Utils.coloredPrint(block.getLinesOfCodeString(), Utils.ANSIColorConstants.ANSI_BLUE) + "  <<<<   ");

            if (block.hasBranch()) {
                BasicBlock falseChild = block.getFalseTarget();
                BasicBlock trueChild = block.getTrueTarget();
                toVisit.add(falseChild);
                toVisit.add(trueChild);
                if (trueChild != null) {
                    System.out.print("T ---- ");
                    System.out.println(Utils.coloredPrint(trueChild.getLinesOfCodeString(), Utils.ANSIColorConstants.ANSI_GREEN));
                }
                System.out.println();
                if (falseChild != null) {
                    System.out.print("F ---- ");
                    System.out.println(Utils.coloredPrint(falseChild.getLinesOfCodeString(), Utils.ANSIColorConstants.ANSI_RED));
                }
                System.out.println();

            } else {
                BasicBlock autoChild = block.getSuccessor();
                toVisit.add(autoChild);
                if (autoChild != null) {
                    System.out.print("A ---- ");
                    System.out.println(autoChild.getLinesOfCodeString());
                    System.out.println();
                }
            }
        }
    }
}

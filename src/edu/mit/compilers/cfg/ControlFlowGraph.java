package edu.mit.compilers.cfg;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.dataflow.passes.BranchFoldingPass;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.utils.GraphVizPrinter;

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
                        .isEmpty() && (cfgBlock.lastAstLine() instanceof Return)))
                .toList();

        final List<BasicBlock> allExecutionsPathsThatDontReturn = exitNop.getPredecessors()
                .stream()
                .filter(cfgBlock -> (cfgBlock.getAstNodes()
                        .isEmpty() || (!(cfgBlock.lastAstLine() instanceof Return))))
                .toList();

        if (allExecutionPathsReturn.size() != allExecutionsPathsThatDontReturn.size()) {
            errors.addAll(allExecutionsPathsThatDontReturn
                    .stream()
                    .map(cfgBlock -> new DecafException(methodDefinition.tokenPosition,
                            methodDefinition.methodName.getLabel() + "'s execution path ends with" +
                                    (cfgBlock.getAstNodes()
                                            .isEmpty() ? "" : (cfgBlock
                                            .lastAstLine()
                                            .getSourceCode())) + " instead of a return statement"))
                    .toList());
        }
        if (allExecutionPathsReturn.isEmpty()) {
            errors.add(new DecafException(
                    methodDefinition.tokenPosition,
                    methodDefinition.methodName.getLabel() + " method does not return expected type " + methodDefinition.returnType));
        }
    }

    public ControlFlowGraphASTVisitor build() {
        final ControlFlowGraphASTVisitor visitor = new ControlFlowGraphASTVisitor();
        final MaximalBasicBlocksUtil maximalVisitor = new MaximalBasicBlocksUtil();

        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);

        var methodNameToEntryBlock = visitor.methodNameToEntryBlock;

        methodNameToEntryBlock.forEach((k, v) -> {
            removeNOPs(v.getSuccessor(), visitor.methodNameToExitNop.get(k));
        });

        methodNameToEntryBlock.forEach((k, v) -> {
            maximalVisitor.exitNOP = visitor.methodNameToExitNop.get(k);
            checkNotNull(v.getSuccessor());
            maximalVisitor.visit(v.getSuccessor());
            catchFalloutError(maximalVisitor.exitNOP, (MethodDescriptor) globalDescriptor.methodsSymbolTable
                    .getDescriptorFromValidScopes(k)
                    .orElseThrow());
        });
        removeNOPs(visitor.global, (NOP) visitor.global.getSuccessor());
        HashMap<String, BasicBlock> methodBlocksCFG = new HashMap<>();
        visitor.methodNameToEntryBlock.forEach((k, v) -> {
            if (v.getLinesOfCodeString()
                    .isBlank()) {
                if (v.getSuccessor() != null) {
                    v.getSuccessor().removePredecessor(v);
                    v = v.getSuccessor();
                }
            }
            methodBlocksCFG.put(k, v);
        });

        visitor.methodNameToEntryBlock = methodBlocksCFG;
        maximalVisitor.exitNOP = (NOP) visitor.global.getSuccessor();
        maximalVisitor.visit(visitor.global);
        BranchFoldingPass.run(methodBlocksCFG.values());
        return visitor;
    }


    public static void removeNOPs(BasicBlock basicBlock, NOP methodExitNop) {
        var seen = new HashSet<BasicBlock>();
        visit(basicBlock, seen, methodExitNop);
    }

    public static void visit(BasicBlock basicBlock, Set<BasicBlock> seen, NOP methodExitNop) {
        switch (basicBlock.getBasicBlockType()) {
            case NO_BRANCH -> visitBasicBlockBranchLess(basicBlock, seen, methodExitNop);
            case BRANCH -> visitBasicBlockWithBranch(basicBlock, seen, methodExitNop);
            default -> visitNOP((NOP) basicBlock, seen, methodExitNop);
        }
    }

    public static void visitBasicBlockBranchLess(BasicBlock basicBlockBranchLess, Set<BasicBlock> seen, NOP methodExitNop) {
        if (!seen.contains(basicBlockBranchLess)) {
            seen.add(basicBlockBranchLess);
            // we assume this is the first instance of the
            if (basicBlockBranchLess.getSuccessor() != null) {
                visit(basicBlockBranchLess.getSuccessor(), seen, methodExitNop);
            }
        }
    }

    public static void visitBasicBlockWithBranch(BasicBlock basicBlockWithBranch, Set<BasicBlock> seen, NOP methodExitNop) {
        if (!seen.contains(basicBlockWithBranch)) {
            seen.add(basicBlockWithBranch);
            if (basicBlockWithBranch.getTrueTarget() != null) {
                visit(basicBlockWithBranch.getTrueTarget(), seen, methodExitNop);
            }
            if (basicBlockWithBranch.getFalseTarget() != null) {
                visit(basicBlockWithBranch.getFalseTarget(), seen, methodExitNop);
            }
        }
    }

    public static void visitNOP(NOP nop, Set<BasicBlock> seen, NOP methodExitNop) {
        if (!seen.contains(nop)) {
            List<BasicBlock> parentsCopy = new ArrayList<>(nop.getPredecessors());
            seen.add(nop);
            BasicBlock endBlock;
            if (nop == methodExitNop) {
                nop.setSuccessor(null);
                return;
            }
            if (nop.getSuccessor() != null) {
                visit(nop.getSuccessor(), seen, methodExitNop);
                endBlock = nop.getSuccessor();
            } else {
                endBlock = methodExitNop;
            }
            for (BasicBlock parent : parentsCopy) {
                // connecting parents to child
                if (parent.hasBranch()) {
                    if (parent.getTrueTarget() == nop) {
                        parent.setTrueTarget(endBlock);
                    } else if (parent.getFalseTarget() == nop) {
                        parent.setFalseTargetUnchecked(endBlock);
                    }
                } else {
                    if (parent.getSuccessor() == nop) {
                        parent.setSuccessor(endBlock);
                    }
                }
                endBlock.removePredecessor(nop);
                endBlock.addPredecessor(parent);
            }
        }
    }

}

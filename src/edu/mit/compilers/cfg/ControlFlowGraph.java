package edu.mit.compilers.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.dataflow.passes.BranchFoldingPass;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.exceptions.DecafException;

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

    public ControlFlowGraphVisitor build() {
        final ControlFlowGraphVisitor visitor = new ControlFlowGraphVisitor();
        final MaximalBasicBlocksUtil maximalVisitor = new MaximalBasicBlocksUtil();
        final NOPRemovalUtil NOPRemovalUtil = new NOPRemovalUtil();

        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);

        visitor.methodNameToEntryBlock.forEach((k, v) -> {
            NOPRemovalUtil.exit = visitor.methodNameToExitNop.get(k);
            assert v.getSuccessor() != null;
            NOPRemovalUtil.visit(v.getSuccessor());
        });

        visitor.methodNameToEntryBlock.forEach((k, v) -> {
            maximalVisitor.exitNOP = visitor.methodNameToExitNop.get(k);
            maximalVisitor.visit(v);
            catchFalloutError(maximalVisitor.exitNOP, (MethodDescriptor) globalDescriptor.methodsSymbolTable
                    .getDescriptorFromValidScopes(k)
                    .orElseThrow());
        });
        NOPRemovalUtil.exit = (NOP) visitor.global.getSuccessor();
        NOPRemovalUtil.visit(visitor.global);
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

}

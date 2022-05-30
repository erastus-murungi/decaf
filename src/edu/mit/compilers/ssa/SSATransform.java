package edu.mit.compilers.ssa;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;

public class SSATransform {
    List<BasicBlock> basicBlocks;
    Set<AssignableName> allVariables;
    ImmediateDominator immediateDominator;

    private void computeDominanceFrontiers(BasicBlock entryBlock) {
        immediateDominator = new ImmediateDominator(entryBlock);
    }

    public SSATransform(BasicBlock entryBlock) {
        computeBasicBlocks(entryBlock);
        computeAllVariablesSet();
        computeDominanceFrontiers(entryBlock);
        placePhiFunctions(entryBlock);
        transform(entryBlock);
        verify();
//        unRename();
    }

    private void computeBasicBlocks(BasicBlock entryBlock) {
        basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    private void computeAllVariablesSet() {
        allVariables = (
                basicBlocks.stream()
                           .flatMap(basicBlock -> basicBlock.getInstructionList()
                                                            .stream())
                           .flatMap(instruction -> instruction.getAllNames()
                                                              .stream())
                           .filter(abstractName -> abstractName instanceof AssignableName)
                           .map(abstractName -> (AssignableName) abstractName)
                           .collect(Collectors.toUnmodifiableSet())
        );
    }

    private Set<BasicBlock> getBasicBlocksModifyingVariable(AssignableName V) {
        return basicBlocks.stream()
                          .filter(basicBlock -> basicBlock.getStores()
                                                          .stream()
                                                          .map(StoreInstruction::getStore)
                                                          .anyMatch(abstractName -> abstractName.equals(V)))
                          .collect(Collectors.toUnmodifiableSet());
    }

    private Set<AssignableName> getStoreLocations(BasicBlock X) {
        return X.getStores()
                .stream()
                .map(StoreInstruction::getStore)
                .map(AssignableName::copy)
                .map(name -> (AssignableName) name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void initialize(Map<AssignableName, Stack<Integer>> stacks, Map<AssignableName, Integer> counters) {
        allVariables.stream()
                    .map(AbstractName::copy)
                    .map(name -> (AssignableName) name)
                    .forEach(
                            a -> {
                                counters.put(a, 0);
                                stacks.put(a, new Stack<>());
                                stacks.get(a)
                                      .push(0);
                            });
    }

    private void genName(AssignableName V, Map<AssignableName, Integer> counters, Map<AssignableName, Stack<Integer>> stacks) {
        var i = counters.get(V);
        stacks.get(V)
              .push(i);
        counters.put(V, i + 1);
        V.renameForSsa(i);
    }

    private void rename(BasicBlock X, Map<AssignableName, Integer> counters, Map<AssignableName, Stack<Integer>> stacks) {
        // rename all phi nodes
        final var stores = getStoreLocations(X);

        for (Phi phi : X.getPhiFunctions()) {
            genName(phi.getStore(), counters, stacks);
        }

        for (Instruction instruction : X.getInstructionList()) {
            if (instruction instanceof Phi)
                continue;
            if (instruction instanceof HasOperand) {
                var Vs = ((HasOperand) instruction).getAssignables();
                for (var V : Vs) {
                    V.renameForSsa(stacks.get(V)
                                         .peek());
                }
            }
            if (instruction instanceof StoreInstruction) {
                var V = ((StoreInstruction) instruction).getStore();
                genName(V, counters, stacks);
            }
        }

        for (var Y : X.getSuccessors()) {
            for (Phi phi : Y.getPhiFunctions()) {
                var V = phi.getVariableForB(X);
                V.renameForSsa(stacks.get(V)
                                     .peek());
            }
        }

        for (var C : immediateDominator.getChildren(X)) {
            rename(C, counters, stacks);
        }

        for (var store : stores) {
            stacks.get(store)
                  .pop();
        }
    }

    /**
     * Places phi functions to create a pruned SSA form
     *
     * @param entryBlock the first basic block of the function
     */
    private void placePhiFunctions(BasicBlock entryBlock) {
        var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);
        for (var V : allVariables) {
            var hasAlready = new HashSet<BasicBlock>();
            var everOnWorkList = new HashSet<BasicBlock>();
            var workList = new ArrayDeque<BasicBlock>();

            var basicBlocksModifyingV = getBasicBlocksModifyingVariable(V);
            for (var basicBlock : basicBlocksModifyingV) {
                everOnWorkList.add(basicBlock);
                workList.add(basicBlock);
            }
            while (!workList.isEmpty()) {
                var X = workList.pop();
                for (var Y : immediateDominator.getDominanceFrontier(X)) {
                    if (!hasAlready.contains(Y)) {
                        // we only insert a phi node for variable V if is live on entry to X
                        if (liveVariableAnalysis.liveIn(X)
                                                .contains(V)) {
                            int nCopiesOfV = (int) Y.getPredecessors()
                                                    .stream()
                                                    .filter(basicBlocksModifyingV::contains)
                                                    .count();
                            if (nCopiesOfV > 1) {
                                var blockToVariable = new HashMap<BasicBlock, AssignableName>();
                                for (var P : Y.getPredecessors()) {
                                    blockToVariable.put(P, V.copy());
                                }
                                Y.getInstructionList()
                                 .add(1, new Phi(V.copy(), blockToVariable));
                            }
                        }
                        hasAlready.add(Y);
                        if (!everOnWorkList.contains(Y)) {
                            everOnWorkList.add(Y);
                            workList.add(Y);
                        }
                    }
                }
            }
        }
    }

    private void transform(BasicBlock entryBlock) {
        var stacks = new HashMap<AssignableName, Stack<Integer>>();
        var counters = new HashMap<AssignableName, Integer>();
        initialize(stacks, counters);
        rename(entryBlock, counters, stacks);
    }

    private void verify() {
        var seen = new HashSet<AssignableName>();
        for (var B : basicBlocks) {
            for (var store : B.getStores()) {
                if (seen.contains(store.getStore())) {
                    throw new IllegalArgumentException(store.getStore() + " store redefined");
                } else {
                    seen.add(store.getStore());
                }
                if (store instanceof Phi) {
                    var s = store.getOperandNames()
                                 .size();
                    if (s < 2) {
                        throw new IllegalArgumentException(store.repr() + " has " + s + "operands");
                    }
                }
            }
        }
    }


    private void unRename() {
        basicBlocks.forEach(this::unRenameInBasicBlock);
    }

    private void unRenameInBasicBlock(BasicBlock X) {
        X.getInstructionList()
         .stream()
         .flatMap(instruction -> instruction.getAllNames()
                                            .stream())
         .filter(abstractName -> abstractName instanceof AssignableName)
         .map(abstractName -> (AssignableName) abstractName)
         .forEach(AssignableName::unRenameForSsa);
    }
}

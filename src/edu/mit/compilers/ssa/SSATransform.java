package edu.mit.compilers.ssa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.UnionFind;
import edu.mit.compilers.utils.Utils;

public class SSATransform {
    List<BasicBlock> basicBlocks;
    ImmediateDominator immediateDominator;

    private void computeDominanceFrontiers(BasicBlock entryBlock) {
        immediateDominator = new ImmediateDominator(entryBlock);
    }

    public SSATransform(Method method) {
        var entryBlock = method.entryBlock;
        computeBasicBlocks(entryBlock);
        computeDominanceFrontiers(entryBlock);
        placePhiFunctions(entryBlock);
        renameVariables(entryBlock);
        verify();
        Utils.printSsaCfg(List.of(method), "ssa_before");
        unRename(entryBlock);
        Utils.printSsaCfg(List.of(method), "ssa_after");
    }

    private void computeBasicBlocks(BasicBlock entryBlock) {
        basicBlocks = DataFlowAnalysis.getReversePostOrder(entryBlock);
    }

    private Set<LValue> computeAllVariablesSet() {
        return (
                basicBlocks.stream()
                           .flatMap(basicBlock -> basicBlock.getInstructionList()
                                                            .stream())
                           .flatMap(instruction -> instruction.getAllNames()
                                                              .stream())
                           .filter(abstractName -> abstractName instanceof LValue)
                           .map(abstractName -> (LValue) abstractName)
                           .collect(Collectors.toUnmodifiableSet())
        );
    }

    private Set<BasicBlock> getBasicBlocksModifyingVariable(LValue V) {
        return basicBlocks.stream()
                          .filter(basicBlock -> basicBlock.getStores()
                                                          .stream()
                                                          .map(StoreInstruction::getStore)
                                                          .anyMatch(abstractName -> abstractName.equals(V)))
                          .collect(Collectors.toUnmodifiableSet());
    }

    private Set<LValue> getStoreLocations(BasicBlock X) {
        return X.getStores()
                .stream()
                .map(StoreInstruction::getStore)
                .map(LValue::copy)
                .map(name -> (LValue) name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void initialize(Set<LValue> allVariables, Map<LValue, Stack<Integer>> stacks, Map<LValue, Integer> counters) {
        allVariables.stream()
                    .map(Value::copy)
                    .map(name -> (LValue) name)
                    .forEach(
                            a -> {
                                counters.put(a, 0);
                                stacks.put(a, new Stack<>());
                                stacks.get(a)
                                      .push(0);
                            });
    }

    private void genName(LValue V, Map<LValue, Integer> counters, Map<LValue, Stack<Integer>> stacks) {
        var i = counters.get(V);
        var copyV = (LValue) V.copy();
        V.renameForSsa(i);
        stacks.get(copyV)
              .push(i);
        counters.put(copyV, i + 1);
    }

    private void rename(BasicBlock X, Map<LValue, Integer> counters, Map<LValue, Stack<Integer>> stacks) {
        // rename all phi nodes
        final var stores = getStoreLocations(X);

        for (Phi phi : X.getPhiFunctions()) {
            genName(phi.getStore(), counters, stacks);
        }

        for (Instruction instruction : X.getInstructionList()) {
            if (instruction instanceof Phi)
                continue;
            if (instruction instanceof HasOperand) {
                var Vs = ((HasOperand) instruction).getLValues();
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

    private List<Phi> getAllPhiNodes() {
        return basicBlocks.stream()
                          .map(
                                  BasicBlock::getPhiFunctions
                          )
                          .flatMap(List::stream)
                          .toList();
    }

    private void initializeForSsaDestruction(HashMap<LValue, Stack<Integer>> stacks) {
        var counters = new HashMap<LValue, Integer>();
        initialize(computeAllVariablesSet(), stacks, counters);
    }

    private Collection<Set<LValue>> phiWebDiscovery() {
        var unionFind = new UnionFind<>(computeAllVariablesSet());
        for (var phiNode : getAllPhiNodes()) {
            for (var operand : phiNode.getOperandNames()) {
                unionFind.union(phiNode.getStore(), (LValue) operand);
            }
        }
        return unionFind.toSets();
    }

    private void addPhiNodeForVatY(LValue V, BasicBlock Y, Collection<BasicBlock> basicBlocksModifyingV) {
        int nCopiesOfV = (int) Y.getPredecessors()
                                .stream()
                                .filter(basicBlocksModifyingV::contains)
                                .count();
        if (nCopiesOfV > 1) {
            var blockToVariable = new HashMap<BasicBlock, Value>();
            for (var P : Y.getPredecessors()) {
                blockToVariable.put(P, V.copy());
            }
            Y.getInstructionList()
             .add(1, new Phi(V.copy(), blockToVariable));
        }
    }

    /**
     * Places phi functions to create a pruned SSA form
     *
     * @param entryBlock the first basic block of the function
     */
    private void placePhiFunctions(BasicBlock entryBlock) {
        var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        var allVariables = computeAllVariablesSet();
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
                            addPhiNodeForVatY(V, Y, basicBlocksModifyingV);
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

    private void renameVariables(BasicBlock entryBlock) {
        var stacks = new HashMap<LValue, Stack<Integer>>();
        var counters = new HashMap<LValue, Integer>();
        initialize(computeAllVariablesSet(), stacks, counters);
        rename(entryBlock, counters, stacks);
    }

    private void verify() {
        var seen = new HashSet<LValue>();
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
                        throw new IllegalArgumentException(store.syntaxHighlightedToString() + " has " + s + "operands");
                    }
                }
            }
        }
    }


    private void unRename(BasicBlock entryBlock) {
        Collection<Set<LValue>> webs = phiWebDiscovery();
        System.out.println(webs);
        LiveVariableAnalysis liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);
        var stacks = new HashMap<LValue, Stack<Integer>>();
        initializeForSsaDestruction(stacks);


        insertCopies(entryBlock, liveVariableAnalysis, stacks);
        removePhiNodes();
    }

    private void removePhiNodes() {
        for (BasicBlock basicBlock: basicBlocks) {
            basicBlock.getInstructionList().reset(
                    basicBlock.getInstructionList().stream().filter(instruction -> !(instruction instanceof Phi)).collect(Collectors.toList())
            );
        }
    }


    private void insertCopies(BasicBlock basicBlock, LiveVariableAnalysis liveVariableAnalysis, Map<LValue, Stack<Integer>> stacks) {
        var pushed = new ArrayList<LValue>();

        for (Instruction instruction : basicBlock.getInstructionList()) {
            if (instruction instanceof Phi)
                continue;
            if (instruction instanceof HasOperand) {
                var Vs = instruction.getAllLValues();
                for (var V : Vs) {
                    V.renameForSsa(stacks.get(V)
                                         .peek());
                }
            }
        }

        scheduleCopies(basicBlock, liveVariableAnalysis.liveOut(basicBlock), stacks, pushed);
        for (var child : immediateDominator.getChildren(basicBlock)) {
            insertCopies(child, liveVariableAnalysis, stacks);
        }

        for (var name: pushed) {
            stacks.get(name).pop();
        }
    }


    private void scheduleCopies(BasicBlock basicBlock, Set<Value> liveOut, Map<LValue, Stack<Integer>> stacks, ArrayList<LValue> pushed) {
        /* Pass One: Initialize the data structures */
        Stack<Pair<Value, LValue>> copySet = new Stack<>();
        Stack<Pair<Value, LValue>> workList = new Stack<>();
        Map<Value, Value> map = new HashMap<>();
        Set<Value> usedByAnother = new HashSet<>();
        Map<Value, BasicBlock> phiDstToOwnerBasicBlockMapping = new HashMap<>();

        for (var successor : basicBlock.getSuccessors()) {
            for (var phi : successor.getPhiFunctions()) {
                var src = phi.getVariableForB(basicBlock);
                var dst = phi.getStore();
                copySet.add(new Pair<>(src, dst));
                map.put(src, src);
                map.put(dst, dst);
                usedByAnother.add(src);
                phiDstToOwnerBasicBlockMapping.put(dst, successor);
            }
        }


        /* Pass Two: Set up the worklist of initial copies */
        for (var srcDest : new ArrayList<>(copySet)) {
            var dst = srcDest.second();
            if (!usedByAnother.contains(dst)) {
                workList.add(srcDest);
                copySet.remove(srcDest);
            }
        }

        /* Pass Three: Iterate over the worklist, inserting copies */
        while (!workList.isEmpty() || !copySet.isEmpty()) {
            while (!workList.isEmpty()) {
                var srcDest = workList.pop();
                var src = srcDest.first();
                var dst = srcDest.second();
                if (liveOut.contains(dst) && !src.equals(dst)) {
                    var t = dst.copyWithIncrementedVersionNumber();
                    var copyInstruction = new CopyInstruction(t, dst.copy());
                    map.put(t, t);
                    var bb = phiDstToOwnerBasicBlockMapping.get(dst);
                    if (bb == null) {
                        throw new IllegalStateException();
                    }
                    bb.getInstructionList()
                      .add(1, copyInstruction);
                    stacks.get(dst)
                          .push(t.getVersionNumber());
                    pushed.add(dst);

                    copyInstruction = new CopyInstruction(dst, map.get(src));
                    bb.addInstructionToTail(copyInstruction);
                    map.put(src, dst);

                    var subWorkList = getPairsWhoseDestinationEquals(src, copySet);
                    workList.addAll(subWorkList);
                    subWorkList.forEach(copySet::remove);
                }
            }

            if (!copySet.isEmpty()) {
                var srcDest = copySet.pop();
                var dst = srcDest.second();
                var t = dst.copyWithIncrementedVersionNumber();
                var copyInstruction = new CopyInstruction(dst.copy(), t);
                basicBlock.addInstructionToTail(copyInstruction);
                map.put(dst, t);
                workList.add(srcDest);
            }
        }
    }

    private List<Pair<Value, LValue>> getPairsWhoseDestinationEquals(Value src, Collection<Pair<Value, LValue>> pairs) {
        var results = new ArrayList<Pair<Value, LValue>>();
        for (var pair : pairs) {
            var dst = pair.second();
            if (dst.equals(src))
                results.add(pair);
        }
        return results;
    }

    private void unRenameInBasicBlock(BasicBlock X) {
        X.getInstructionList()
         .stream()
         .flatMap(instruction -> instruction.getAllNames()
                                            .stream())
         .filter(abstractName -> abstractName instanceof LValue)
         .map(abstractName -> (LValue) abstractName)
         .forEach(LValue::unRenameForSsa);
    }
}

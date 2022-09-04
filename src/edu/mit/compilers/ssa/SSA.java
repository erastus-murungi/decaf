package edu.mit.compilers.ssa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.registerallocation.Coalesce;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.TarjanSCC;
import edu.mit.compilers.utils.Utils;

public class SSA {
    public static void construct(Method method) {
        var entryBlock = method.entryBlock;
        var basicBlocks = TarjanSCC.getReversePostOrder(entryBlock);
        var immediateDominator = new ImmediateDominator(entryBlock);
        placePhiFunctions(entryBlock, basicBlocks, immediateDominator);
        renameVariables(entryBlock, immediateDominator, basicBlocks);
        verify(basicBlocks);
        Utils.printSsaCfg(List.of(method), "ssa_before_" + method.methodName());
    }

    public static void deconstruct(Method method, ProgramIr programIr) {
        Utils.printSsaCfg(List.of(method), "ssa_after_opt_" + method.methodName());

        var entryBlock = method.entryBlock;
        var basicBlocks = TarjanSCC.getReversePostOrder(entryBlock);
        var immediateDominator = new ImmediateDominator(entryBlock);
        verify(basicBlocks);
        deconstructSsa(entryBlock, basicBlocks,immediateDominator);
        Coalesce.doIt(method, programIr);
        Utils.printSsaCfg(List.of(method), "ssa_after_" + method.methodName());
    }
    private SSA() {
    }

    private static Set<BasicBlock> getBasicBlocksModifyingVariable(LValue V, List<BasicBlock> basicBlocks) {
        return basicBlocks.stream()
                          .filter(basicBlock -> basicBlock.getStoreInstructions()
                                                          .stream()
                                                          .map(StoreInstruction::getDestination)
                                                          .anyMatch(abstractName -> abstractName.equals(V)))
                          .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<LValue> getStoreLocations(BasicBlock X) {
        return X.getStoreInstructions()
                .stream()
                .map(StoreInstruction::getDestination)
                .map(LValue::copy)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void initialize(Set<LValue> allVariables, Map<LValue, Stack<Integer>> stacks, Map<LValue, Integer> counters) {
        allVariables.stream()
                    .map(Value::copy)
                    .map(name -> (LValue) name)
                    .forEach(
                            a -> {
                                counters.put(a, 0);
                                stacks.put(a, new Stack<>());
                                stacks.get(a)
                                      .add(0);
                            });
    }

    private static void genName(LValue V, Map<LValue, Integer> counters, Map<LValue, Stack<Integer>> stacks) {
        var i = counters.get(V);
        var copyV = (LValue) V.copy();
        V.renameForSsa(i);
        stacks.get(copyV)
              .push(i);
        counters.put(copyV, i + 1);
    }

    private static void rename(BasicBlock X, ImmediateDominator immediateDominator, Map<LValue, Integer> counters, Map<LValue, Stack<Integer>> stacks) {
        // rename all phi nodes
        final var stores = getStoreLocations(X);

        for (Phi phi : X.getPhiFunctions()) {
            genName(phi.getDestination(), counters, stacks);
        }

        for (Instruction instruction : X.getInstructionList()) {
            if (instruction instanceof Phi)
                continue;
            if (instruction instanceof HasOperand) {
                var Vs = ((HasOperand) instruction).getOperandLValues();
                for (var V : Vs) {
                    V.renameForSsa(stacks.get(V)
                                         .peek());
                }
            }
            if (instruction instanceof StoreInstruction) {
                var V = ((StoreInstruction) instruction).getDestination();
                genName(V, counters, stacks);
            }
        }

        for (var Y : X.getSuccessors()) {
            for (Phi phi : Y.getPhiFunctions()) {
                var V = phi.getVariableForB(X);
                if (stacks.get(V)
                          .isEmpty())
                    continue;
                V.renameForSsa(stacks.get(V)
                                     .peek());
            }
        }

        for (var C : immediateDominator.getChildren(X)) {
            rename(C, immediateDominator, counters, stacks);
        }

        for (var store : stores) {
            stacks.get(store)
                  .pop();
        }
    }

    private static void initializeForSsaDestruction(List<BasicBlock> basicBlocks, HashMap<LValue, Stack<LValue>> stacks) {
        Utils.getAllLValuesInBasicBlocks(basicBlocks).stream()
                                               .map(LValue::copy)
                                               .forEach(a -> {
                                            stacks.put(a, new Stack<>());
                                            stacks.get(a)
                                                  .add(a.copy());
                                        }
                                );
    }

    private static void addPhiNodeForVatY(LValue V, BasicBlock Y, Collection<BasicBlock> basicBlocksModifyingV) {
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
    private static void placePhiFunctions(BasicBlock entryBlock, List<BasicBlock> basicBlocks, ImmediateDominator immediateDominator) {
        var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

        var allVariables = Utils.getAllLValuesInBasicBlocks(basicBlocks);
        for (var V : allVariables) {
            var hasAlready = new HashSet<BasicBlock>();
            var everOnWorkList = new HashSet<BasicBlock>();
            var workList = new ArrayDeque<BasicBlock>();

            var basicBlocksModifyingV = getBasicBlocksModifyingVariable(V, basicBlocks);
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

    private static void renameVariables(BasicBlock entryBlock, ImmediateDominator immediateDominator, List<BasicBlock> basicBlocks) {
        var stacks = new HashMap<LValue, Stack<Integer>>();
        var counters = new HashMap<LValue, Integer>();
        initialize(Utils.getAllLValuesInBasicBlocks(basicBlocks), stacks, counters);
        rename(entryBlock, immediateDominator, counters, stacks);
    }

    private static void verify(List<BasicBlock> basicBlocks) {
        var seen = new HashSet<LValue>();
        for (var B : basicBlocks) {
            for (var store : B.getStoreInstructions()) {
                if (seen.contains(store.getDestination())) {
                    throw new IllegalStateException(store.getDestination() + " store redefined");
                } else {
                    seen.add(store.getDestination());
                }
                if (store instanceof Phi) {
                    var s = store.getOperandValues()
                                 .size();
                    if (s < 2) {
                        throw new IllegalStateException(store.syntaxHighlightedToString() + " has " + s + "operands");
                    }
                }
            }
        }
    }

    private static void deconstructSsa(BasicBlock entryBlock, List<BasicBlock> basicBlocks, ImmediateDominator immediateDominator) {
        var stacks = new HashMap<LValue, Stack<LValue>>();
        initializeForSsaDestruction(basicBlocks, stacks);
        insertCopies(entryBlock, immediateDominator, new LiveVariableAnalysis(entryBlock), stacks);
        removePhiNodes(basicBlocks);
    }

    private static void removePhiNodes(List<BasicBlock> basicBlocks) {
        for (BasicBlock basicBlock : basicBlocks) {
            basicBlock.getInstructionList()
                      .reset(
                              basicBlock.getInstructionList()
                                        .stream()
                                        .filter(instruction -> !(instruction instanceof Phi))
                                        .collect(Collectors.toList())
                      );
        }
    }

    private static void insertCopies(BasicBlock basicBlock, ImmediateDominator immediateDominator, LiveVariableAnalysis liveVariableAnalysis, Map<LValue, Stack<LValue>> stacks) {
        var pushed = new ArrayList<LValue>();

        for (Instruction instruction : basicBlock.getInstructionList()) {
            if (instruction instanceof Phi)
                continue;
            if (instruction instanceof HasOperand) {
                var Vs = ((HasOperand) instruction).getOperandLValues();
                for (var V : Vs) {
                    if (instruction instanceof StoreInstruction storeInstruction) {
                        if (storeInstruction.getDestination().equals(stacks.get(V).peek()))
                            continue;
                    }
                        V.renameForSsa(stacks.get(V)
                                         .peek());
                }
            }
        }

        scheduleCopies(basicBlock, liveVariableAnalysis.liveOut(basicBlock), stacks, pushed);
        for (var child : immediateDominator.getChildren(basicBlock)) {
            insertCopies(child, immediateDominator, liveVariableAnalysis, stacks);
        }

        for (var name : pushed) {
            stacks.get(name)
                  .pop();
        }
        pushed.clear();
    }


    private static void scheduleCopies(BasicBlock basicBlock, Set<Value> liveOut, Map<LValue, Stack<LValue>> stacks, ArrayList<LValue> pushed) {
        /* Pass One: Initialize the data structures */
        Stack<Pair<Value, LValue>> copySet = new Stack<>();
        Stack<Pair<Value, LValue>> workList = new Stack<>();
        Map<Value, Value> map = new HashMap<>();
        Set<Value> usedByAnother = new HashSet<>();
        Map<Value, BasicBlock> phiDstToOwnerBasicBlockMapping = new HashMap<>();

        for (var successor : basicBlock.getSuccessors()) {
            for (var phi : successor.getPhiFunctions()) {
                var src = phi.getVariableForB(basicBlock);
                var dst = phi.getDestination();
                copySet.add(new Pair<>(src, dst));
                map.put(src, src);
                map.put(dst, dst);
                usedByAnother.add(src);
                phiDstToOwnerBasicBlockMapping.put(dst, successor);
            }
        }


        /* Pass Two: Set up the workList of initial copies */
        for (var srcDest : new ArrayList<>(copySet)) {
            var dst = srcDest.second();
            if (!usedByAnother.contains(dst)) {
                workList.add(srcDest);
                copySet.remove(srcDest);
            }
        }

        /* Pass Three: Iterate over the workList, inserting copies */
        while (!workList.isEmpty() || !copySet.isEmpty()) {
            while (!workList.isEmpty()) {
                var srcDest = workList.pop();
                var src = srcDest.first();
                var dst = srcDest.second();
                if (liveOut.contains(dst) && !src.equals(dst)) {
                    var temp = Variable.genTemp(dst.getType());
                    var copyInstruction = CopyInstruction.noAstConstructor(temp, dst.copy());
                    var dstOwner = Objects.requireNonNull(phiDstToOwnerBasicBlockMapping.get(dst), "dest " + dst + " does not have a source basic block");
                    dstOwner.getInstructionList()
                            .add(1, copyInstruction);
                    stacks.get(dst)
                          .push(temp);
                    stacks.put(temp, new Stack<>());
                    stacks.get(temp).add(temp.copy());
                    pushed.add(dst);
                }
                var copyInstruction = CopyInstruction.noAstConstructor(dst, map.get(src));
                basicBlock.addInstructionToTail(copyInstruction);
                map.put(src, dst);

                var subWorkList = getPairsWhoseDestinationEquals(src, copySet);
                workList.addAll(subWorkList);
                subWorkList.forEach(copySet::remove);
            }
            if (!copySet.isEmpty()) {
                var srcDest = copySet.pop();
                var dst = srcDest.second();
                var t = dst.copyWithIncrementedVersionNumber();
                var copyInstruction = CopyInstruction.noAstConstructor(dst.copy(), t);
                basicBlock.addInstructionToTail(copyInstruction);
                map.put(dst, t);
                workList.add(srcDest);
            }
        }
    }

    private static List<Pair<Value, LValue>> getPairsWhoseDestinationEquals(Value src, Collection<Pair<Value, LValue>> pairs) {
        var results = new ArrayList<Pair<Value, LValue>>();
        for (var pair : pairs) {
            var dst = pair.second();
            if (dst.equals(src))
                results.add(pair);
        }
        return results;
    }

}

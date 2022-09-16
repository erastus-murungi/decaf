package decaf.ssa;

import static decaf.common.StronglyConnectedComponentsTarjan.getReversePostOrder;
import static decaf.common.Utils.getAllVirtualRegistersInBasicBlocks;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrRegister;
import decaf.codegen.names.IrValue;
import decaf.dataflow.analyses.LiveVariableAnalysis;
import decaf.dataflow.dominator.DominatorTree;
import decaf.regalloc.InterferenceGraph;
import decaf.regalloc.LiveIntervalsManager;
import decaf.common.CompilationContext;
import decaf.common.GraphVizManager;
import decaf.common.Pair;
import decaf.common.ProgramIr;
import decaf.common.UnionFind;
import decaf.common.Utils;

public class SSA {
  private SSA() {
  }

  private static void buildCfgGuava(@NotNull List<BasicBlock> basicBlocks) {
    // Gr
    MutableGraph<BasicBlock> graph = GraphBuilder.directed()
                                                 .build();
    basicBlocks.forEach(basicBlock -> {
      graph.addNode(basicBlock);
      basicBlock.getSuccessors()
                .forEach(successor -> {
                  graph.addNode(successor);
                  graph.putEdge(
                      basicBlock,
                      successor
                  );
                });
    });
  }

  public static void construct(@NotNull Method method) {
    var entryBlock = method.getEntryBlock();
    var basicBlocks = getReversePostOrder(entryBlock);
//    buildCfgGuava(basicBlocks);
    var dominatorTree = new DominatorTree(entryBlock);
    if (CompilationContext.isDebugModeOn()) GraphVizManager.printDominatorTree(
        dominatorTree,
        "dom_" + method.methodName()
    );
    if (CompilationContext.isDebugModeOn()) Utils.printSsaCfg(
        List.of(method),
        "cfg_norm" + method.methodName()
    );
    placePhiFunctions(
        entryBlock,
        basicBlocks,
        dominatorTree
    );
    renameVariables(
        method,
        dominatorTree,
        basicBlocks
    );
    verifySsa(basicBlocks);
    if (CompilationContext.isDebugModeOn()) Utils.printSsaCfg(
        List.of(method),
        "ssa_before_" + method.methodName()
    );
  }

  private static void renameMethodArgs(@NotNull Method method) {
    for (var V : method.getParameterNames()) {
      V.renameForSsa(0);
    }
  }

  public static void deconstruct(
      @NotNull Method method,
      @NotNull ProgramIr programIr
  ) {
    if (CompilationContext.isDebugModeOn()) Utils.printSsaCfg(
        List.of(method),
        "ssa_after_opt_" + method.methodName()
    );

    var entryBlock = method.getEntryBlock();
    var basicBlocks = getReversePostOrder(entryBlock);
    var immediateDominator = new DominatorTree(entryBlock);
    verifySsa(basicBlocks);
    deconstructSsa(
        entryBlock,
        basicBlocks,
        immediateDominator
    );
    coalesce(
        method,
        programIr
    );
    if (CompilationContext.isDebugModeOn()) Utils.printSsaCfg(
        List.of(method),
        "ssa_after_" + method.methodName()
    );
  }

  private static Set<BasicBlock> getBasicBlocksModifyingVariable(
      @NotNull IrRegister V,
      @NotNull List<BasicBlock> basicBlocks
  ) {
    return basicBlocks.stream()
                      .filter(basicBlock -> basicBlock.getStoreInstructions()
                                                      .stream()
                                                      .map(StoreInstruction::getDestination)
                                                      .anyMatch(abstractName -> abstractName.equals(V)))
                      .collect(Collectors.toUnmodifiableSet());
  }

  private static Set<IrRegister> getStoreLocations(@NotNull BasicBlock X) {
    return X.getStoreInstructions()
            .stream()
            .map(StoreInstruction::getDestination)
            .filter(lValue -> lValue instanceof IrRegister)
            .map(lValue -> (IrRegister) lValue)
            .map(IrRegister::copy)
            .collect(Collectors.toSet());
  }

  private static void initialize(
      @NotNull Set<IrRegister> allVariables,
      @NotNull Map<IrRegister, Stack<Integer>> stacks,
      @NotNull Map<IrRegister, Integer> counters
  ) {
    allVariables.stream()
                .map(IrValue::copy)
                .map(name -> (IrRegister) name)
                .forEach(a -> {
                  counters.put(
                      a,
                      0
                  );
                  stacks.put(
                      a,
                      new Stack<>()
                  );
                  stacks.get(a)
                        .add(0);
                });
  }

  private static void genName(
      IrRegister V,
      Map<IrRegister, Integer> counters,
      Map<IrRegister, Stack<Integer>> stacks
  ) {
    var i = counters.get(V);
    var copyV = (IrRegister) V.copy();
    V.renameForSsa(i);
    stacks.get(copyV)
          .push(i);
    counters.put(
        copyV,
        i + 1
    );
  }

  private static void rename(
      BasicBlock X,
      DominatorTree dominatorTree,
      Map<IrRegister, Integer> counters,
      Map<IrRegister, Stack<Integer>> stacks
  ) {
    // rename all phi nodes
    final var stores = getStoreLocations(X);

    for (Phi phi : X.getPhiFunctions()) {
      genName(
          (IrRegister) phi.getDestination(),
          counters,
          stacks
      );
    }

    for (Instruction instruction : X.getInstructionList()) {
      if (instruction instanceof Phi) continue;
      if (instruction instanceof HasOperand hasOperand) {
        var Vs = hasOperand.getOperandVirtualRegisters();
        for (var V : Vs) {
          V.renameForSsa(stacks.get(V)
                               .peek());
        }
      }
      if (instruction instanceof StoreInstruction) {
        var V = ((StoreInstruction) instruction).getDestination();
        if (V instanceof IrRegister irRegister) {
          genName(
              irRegister,
              counters,
              stacks
          );
        }
      }
    }

    for (var Y : X.getSuccessors()) {
      for (Phi phi : Y.getPhiFunctions()) {
        var V = phi.getVariableForB(X);
        if (V instanceof IrRegister irRegister) {
          if (stacks.get(V)
                    .isEmpty()) continue;
          irRegister.renameForSsa(stacks.get(V)
                                        .peek());
        }
      }
    }

    for (var C : dominatorTree.getChildren(X)) {
      rename(
          C,
          dominatorTree,
          counters,
          stacks
      );
    }

    for (var store : stores) {
      stacks.get(store)
            .pop();
    }
  }

  private static void initializeForSsaDestruction(
      List<BasicBlock> basicBlocks,
      HashMap<IrRegister, Stack<IrRegister>> stacks
  ) {
    getAllVirtualRegistersInBasicBlocks(basicBlocks).stream()
                                                    .map(IrRegister::copy)
                                                    .forEach(a -> {
                                                      stacks.put(
                                                          a,
                                                          new Stack<>()
                                                      );
                                                      stacks.get(a)
                                                            .add(a.copy());
                                                    });
  }

  private static int getNCopiesOfV(
      @NotNull BasicBlock X,
      @NotNull Collection<BasicBlock> basicBlocksModifyingV
  ) {
    int nCopiesOfV = (int) X.getPredecessors()
                            .stream()
                            .filter(basicBlocksModifyingV::contains)
                            .count();
    return nCopiesOfV;
  }

  private static int genNCopiesOfV(
      @NotNull BasicBlock X,
      @NotNull Collection<BasicBlock> basicBlocksModifyingV
  ) {
    // we find the number of predecessors of V which can be reached by all the blocks modifying V
    return (int) X.getPredecessors()
                  .stream()
                  .filter(predecessor -> basicBlocksModifyingV.stream()
                                                              .anyMatch(modifier -> Utils.isReachable(
                                                                  predecessor,
                                                                  modifier
                                                              )))
                  .count();
  }

  private static void addPhiNodeForVatY(
      @NotNull IrRegister V,
      @NotNull BasicBlock X,
      @NotNull Collection<BasicBlock> basicBlocksModifyingV
  ) {
    var copiesOfV = genNCopiesOfV(X, basicBlocksModifyingV);
    if (copiesOfV > 1) {
      var blockToVariable = new HashMap<BasicBlock, IrValue>();
      for (var P : X.getPredecessors()) {
        blockToVariable.put(
            P,
            V.copy()
        );
      }
      X.getInstructionList()
       .add(
           0,
           new Phi(
               V.copy(),
               blockToVariable
           )
       );
    }
  }

  /**
   * Places phi functions to create a pruned SSA form
   *
   * @param entryBlock the first basic block of the function
   */
  private static void placePhiFunctions(
      @NotNull BasicBlock entryBlock,
      @NotNull List<BasicBlock> basicBlocks,
      @NotNull DominatorTree dominatorTree
  ) {
    var liveVariableAnalysis = new LiveVariableAnalysis(entryBlock);

    for (var V : getAllVirtualRegistersInBasicBlocks(basicBlocks)) {
      var hasAlready = new HashSet<BasicBlock>();
      var everOnWorkList = new HashSet<BasicBlock>();
      var workList = new ArrayDeque<BasicBlock>();

      var basicBlocksModifyingV = getBasicBlocksModifyingVariable(
          V,
          basicBlocks
      );
      for (var basicBlock : basicBlocksModifyingV) {
        everOnWorkList.add(basicBlock);
        workList.add(basicBlock);
      }
      while (!workList.isEmpty()) {
        var X = workList.pop();
        for (var Y : dominatorTree.getDominanceFrontier(X)) {
          if (!hasAlready.contains(Y)) {
            // we only insert a phi node for irAssignableValue V if is live on entry to X
            if (liveVariableAnalysis.liveIn(X)
                                    .contains(V)) {
              addPhiNodeForVatY(
                  V,
                  Y,
                  basicBlocksModifyingV
              );
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

  private static void renameVariables(
      @NotNull Method method,
      @NotNull DominatorTree dominatorTree,
      @NotNull List<BasicBlock> basicBlocks
  ) {
    var stacks = new HashMap<IrRegister, Stack<Integer>>();
    var counters = new HashMap<IrRegister, Integer>();
    initialize(
        getAllVirtualRegistersInBasicBlocks(basicBlocks),
        stacks,
        counters
    );
    rename(
        method.getEntryBlock(),
        dominatorTree,
        counters,
        stacks
    );
    renameMethodArgs(method);
  }

  private static void verifySsa(@NotNull List<BasicBlock> basicBlocks) {
    var seen = new HashSet<IrRegister>();
    for (var B : basicBlocks) {
      for (var store : B.getStoreInstructions()) {
        if (store.getDestination() instanceof IrRegister irRegisterDestination) {
          if (seen.contains(irRegisterDestination)) {
            throw new IllegalStateException(store.getDestination() + " store redefined");
          } else {
            seen.add(irRegisterDestination);
          }
          if (store instanceof Phi) {
            var numPhiOperands = store.getOperandValues()
                                      .size();
            if (numPhiOperands < 1) {
              throw new IllegalStateException(
                  store.syntaxHighlightedToString() + " has " + numPhiOperands + " operands");
            }
          }
        }
      }
    }
  }

  public static void verifySsa(@NotNull Method method) {
    var basicBlocks = getReversePostOrder(method.getEntryBlock());
    SSA.verifySsa(basicBlocks);
  }

  private static void deconstructSsa(
      @NotNull BasicBlock entryBlock,
      @NotNull List<BasicBlock> basicBlocks,
      @NotNull DominatorTree dominatorTree
  ) {
    var stacks = new HashMap<IrRegister, Stack<IrRegister>>();
    initializeForSsaDestruction(
        basicBlocks,
        stacks
    );
    insertCopies(
        entryBlock,
        dominatorTree,
        new LiveVariableAnalysis(entryBlock),
        stacks
    );
    removePhiNodes(basicBlocks);
  }

  private static void removePhiNodes(@NotNull List<BasicBlock> basicBlocks) {
    for (BasicBlock basicBlock : basicBlocks) {
      basicBlock.getInstructionList()
                .reset(basicBlock.getInstructionList()
                                 .stream()
                                 .filter(instruction -> !(instruction instanceof Phi))
                                 .collect(Collectors.toList()));
    }
  }

  private static void insertCopies(
      @NotNull BasicBlock basicBlock,
      @NotNull DominatorTree dominatorTree,
      @NotNull LiveVariableAnalysis liveVariableAnalysis,
      @NotNull Map<IrRegister, Stack<IrRegister>> stacks
  ) {
    var pushed = new ArrayList<IrRegister>();

    for (Instruction instruction : basicBlock.getInstructionList()) {
      if (instruction instanceof Phi) continue;
      if (instruction instanceof HasOperand) {
        var Vs = ((HasOperand) instruction).getOperandVirtualRegisters();
        for (var V : Vs) {
          if (instruction instanceof StoreInstruction storeInstruction) {
            if (storeInstruction.getDestination()
                                .equals(stacks.get(V)
                                              .peek())) continue;
          }
          V.renameForSsa(stacks.get(V)
                               .peek());
        }
      }
    }

    scheduleCopies(
        basicBlock,
        liveVariableAnalysis.liveOut(basicBlock),
        stacks,
        pushed
    );
    for (var child : dominatorTree.getChildren(basicBlock)) {
      insertCopies(
          child,
          dominatorTree,
          liveVariableAnalysis,
          stacks
      );
    }

    for (var name : pushed) {
      stacks.get(name)
            .pop();
    }
    pushed.clear();
  }


  private static void scheduleCopies(
      @NotNull BasicBlock basicBlock,
      @NotNull Set<IrValue> liveOut,
      @NotNull Map<IrRegister, Stack<IrRegister>> stacks,
      @NotNull ArrayList<IrRegister> pushed
  ) {
    /* Pass One: Initialize the data structures */
    Stack<Pair<IrValue, IrRegister>> copySet = new Stack<>();
    Stack<Pair<IrValue, IrRegister>> workList = new Stack<>();
    Map<IrValue, IrValue> map = new HashMap<>();
    Set<IrValue> usedByAnother = new HashSet<>();
    Map<IrValue, BasicBlock> phiDstToOwnerBasicBlockMapping = new HashMap<>();

    for (var successor : basicBlock.getSuccessors()) {
      for (var phi : successor.getPhiFunctions()) {
        var src = phi.getVariableForB(basicBlock);
        var dst = phi.getDestination();
        if (dst instanceof IrRegister dstVirtual) {
          copySet.add(new Pair<>(
              src,
              dstVirtual
          ));
          map.put(
              src,
              src
          );
          map.put(
              dst,
              dst
          );
          usedByAnother.add(src);
          phiDstToOwnerBasicBlockMapping.put(
              dst,
              successor
          );
        }
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
          var temp = (IrRegister) IrRegister.gen(dst.getType());
          var copyInstruction = CopyInstruction.noAstConstructor(
              temp,
              dst.copy()
          );
          var dstOwner = Objects.requireNonNull(
              phiDstToOwnerBasicBlockMapping.get(dst),
              "dest " + dst + " does not have a source basic block"
          );
          dstOwner.getInstructionList()
                  .add(
                      0,
                      copyInstruction
                  );
          stacks.get(dst)
                .push(temp);
          stacks.put(
              temp,
              new Stack<>()
          );
          stacks.get(temp)
                .add(temp.copy());
          pushed.add(dst);
        }
        var copyInstruction = CopyInstruction.noAstConstructor(
            dst,
            map.get(src)
        );
        basicBlock.addInstructionToTail(copyInstruction);
        map.put(
            src,
            dst
        );

        var subWorkList = getPairsWhoseDestinationEquals(
            src,
            copySet
        );
        workList.addAll(subWorkList);
        subWorkList.forEach(copySet::remove);
      }
      if (!copySet.isEmpty()) {
        var srcDest = copySet.pop();
        var dst = srcDest.second();
        var temp = IrRegister.gen(dst.getType());
        var copyInstruction = CopyInstruction.noAstConstructor(
            dst.copy(),
            temp
        );
        basicBlock.addInstructionToTail(copyInstruction);
        map.put(
            dst,
            temp
        );
        workList.add(srcDest);
      }
    }
  }

  private static List<Pair<IrValue, IrRegister>> getPairsWhoseDestinationEquals(
      IrValue src,
      Collection<Pair<IrValue, IrRegister>> pairs
  ) {
    var results = new ArrayList<Pair<IrValue, IrRegister>>();
    for (var pair : pairs) {
      var dst = pair.second();
      if (dst.equals(src)) results.add(pair);
    }
    return results;
  }

  public static void coalesce(
      Method method,
      ProgramIr programIr
  ) {
    while (true) {
      var changesHappened = false;
      var liveIntervalsUtil = new LiveIntervalsManager(
          method,
          programIr
      );
      if (CompilationContext.isDebugModeOn()) liveIntervalsUtil.prettyPrintLiveIntervals(method);
      var interferenceGraph = new InterferenceGraph(
          liveIntervalsUtil,
          method
      );
      var unionFind = new UnionFind<>(interferenceGraph.getMoveNodes());
      for (var pair : interferenceGraph.getUniqueMoveEdges()) {
        unionFind.union(
            pair.first(),
            pair.second()
        );
      }
      var sets = unionFind.toSets();
      var allUses = sets.stream()
                        .filter(nodes -> nodes.size() > 1)
                        .map(nodes -> nodes.stream()
                                           .filter(liveInterval -> liveInterval.irAssignableValue() instanceof IrRegister)
                                           .map(liveInterval -> (IrRegister) liveInterval.irAssignableValue()
                                                                                         .copy())
                                           .collect(Collectors.toUnmodifiableSet()))
                        .toList();
      if (allUses.isEmpty()) break;
      for (var uses : allUses) {
        if (uses.size() >= 2) {
          changesHappened = changesHappened | renameAllUses(
              uses,
              method
          );
        }
      }
      if (!changesHappened) break;
    }
    if (CompilationContext.isDebugModeOn()) new LiveIntervalsManager(
        method,
        programIr
    ).prettyPrintLiveIntervals(method);
  }

  private static boolean renameAllUses(
      Collection<IrRegister> uses,
      Method method
  ) {
    var changesHappened = false;
    var basicBlocks = getReversePostOrder(method.getEntryBlock());
    var newName = uses.stream()
                      .min(Comparator.comparing(Object::toString))
                      .orElseThrow()
                      .copy();
    for (BasicBlock basicBlock : basicBlocks) {
      List<Instruction> instructions = new ArrayList<>();
      for (Instruction instruction : basicBlock.getInstructionList()) {
        for (IrRegister irRegister : instruction.getAllVirtualRegisters()) {
          if (uses.contains(irRegister)) {
            changesHappened = true;
            irRegister.renameForSsa(newName);
          }
        }
        if (instruction instanceof CopyInstruction copyInstruction) {
          if (copyInstruction.getDestination()
                             .equals(copyInstruction.getValue())) continue;
        }
        instructions.add(instruction);
      }
      basicBlock.getInstructionList()
                .reset(instructions);
    }
    return changesHappened;
  }

}

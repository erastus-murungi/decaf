package edu.mit.compilers.dataflow.ssapasses;

import static edu.mit.compilers.utils.TarjanSCC.getReversePostOrder;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.dominator.ImmediateDominator;
import edu.mit.compilers.dataflow.ssapasses.worklistitems.SsaEdge;
import edu.mit.compilers.ssa.Phi;
import edu.mit.compilers.utils.Utils;

public class SCCP {
    Stack<BasicBlock> flowGraphWorkList = new Stack<>();
    Stack<SsaEdge> ssaWorkList = new Stack<>();
    Set<BasicBlock> reachableBasicBlocks = new HashSet<>();
    Map<Value, LatticeElement> latticeValues = new HashMap<>();
    Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
    Set<SsaEdge> ssaEdgeList = Collections.emptySet();
    BasicBlock entryBlock;

    public SCCP(BasicBlock entryBlock) {
        this.entryBlock = entryBlock;
        initializeWorkSets();
        runWorkList();
    }

    public void initializeWorkSets() {
        for (BasicBlock basicBlock : getReversePostOrder(entryBlock)) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                for (Value v : instruction.getAllValues()) {
                    if (v instanceof LValue) {
                        latticeValues.put(v, LatticeElement.top());
                    } else if (v instanceof NumericalConstant numericalConstant) {
                        latticeValues.put(v, LatticeElement.constant(numericalConstant.getValue()));
                    }
                }
            }
        }
        this.flowGraphWorkList.add(entryBlock);
        computeSsaEdges();
    }

    private List<SsaEdge> getSsaEdgesForVariable(LValue lValue) {
        return ssaEdgeList.stream()
                          .filter(ssaEdge -> ssaEdge.getValue()
                                                    .equals(lValue))
                          .toList();
    }

    private void visitPhi(Phi phi) {
        var values = phi.getOperandValues()
                        .stream()
                        .map(value -> reachableBasicBlocks.contains(phi.getBasicBlockForV(value)) ? latticeValues.get(value) : LatticeElement.top())
                        .collect(Collectors.toList());
        var newLatticeElement = LatticeElement.meet(values);
        if (!newLatticeElement.equals(latticeValues.get(phi.getDestination()))) {
            latticeValues.put(phi.getDestination(), LatticeElement.meet(values));
            ssaWorkList.addAll(getSsaEdgesForVariable(phi.getDestination()));
        }
    }

    private void visitExpression(Instruction instruction, BasicBlock basicBlock) {
        if (instruction instanceof CopyInstruction copyInstruction) {
            var updated = latticeValues.get(copyInstruction.getValue());
            if (!updated.equals(latticeValues.get(copyInstruction.getDestination()))) {
                latticeValues.put(copyInstruction.getDestination(), updated);
                ssaWorkList.addAll(getSsaEdgesForVariable(copyInstruction.getDestination()));
            }
        } else if (instruction instanceof BinaryInstruction binaryInstruction) {
            var a = latticeValues.get(binaryInstruction.fstOperand);
            var b = latticeValues.get(binaryInstruction.sndOperand);
            if (!a.isBottom() && !b.isBottom()) {
                @NotNull LatticeElement updated;
                if (a.isConstant() && b.isConstant()) {
                    var longVal = Utils.symbolicallyEvaluate(String.format("%d %s %d", a.getValue(), binaryInstruction.operator, b.getValue()))
                                       .orElseThrow();
                    updated = LatticeElement.constant(longVal);
                } else {
                    updated = LatticeElement.meet(a, b);
                }
                if (updated != latticeValues.get(binaryInstruction.getDestination())) {
                    latticeValues.put(binaryInstruction.getDestination(), updated);
                    ssaWorkList.addAll(getSsaEdgesForVariable(binaryInstruction.getDestination()));
                }
            }
        } else if (instruction instanceof UnaryInstruction unaryInstruction) {
            var a = latticeValues.get(unaryInstruction.operand);
            if (a.isConstant()) {
                var longVal = Utils.symbolicallyEvaluate(String.format("%s (%d)", unaryInstruction.operator, a.getValue()))
                                   .orElseThrow();
                var updated = LatticeElement.constant(longVal);
                if (updated.equals(latticeValues.get(unaryInstruction.getDestination()))) {
                    latticeValues.put(unaryInstruction.getDestination(), updated);
                    ssaWorkList.addAll(getSsaEdgesForVariable(unaryInstruction.getDestination()));
                }
            }
        } else if (instruction instanceof ConditionalBranch conditionalBranch) {
            var condition = latticeValues.get(conditionalBranch.condition);
            if (LatticeElement.meet(condition, LatticeElement.constant(0L))
                              .isBottom()) {
                flowGraphWorkList.add(basicBlock.getTrueTarget());
            }
            if (LatticeElement.meet(condition, LatticeElement.constant(1L))
                              .isBottom()) {
                flowGraphWorkList.add(basicBlock.getFalseTarget());
            }
        } else if (instruction instanceof UnconditionalJump unconditionalJump) {
            if (!reachableBasicBlocks.contains(unconditionalJump.getTarget()))
                flowGraphWorkList.add(unconditionalJump.getTarget());
        }

    }

    public int countIncomingExecutableEdges(BasicBlock block) {
        int count = 0;
        for (var pred : block.getPredecessors()) {
            if (reachableBasicBlocks.contains(pred)) {
                count++;
            }
        }
        return count;
    }

    private boolean isReachable(BasicBlock flowEdge) {
        return reachableBasicBlocks.contains(flowEdge);
    }

    private void setReachableBasicBlocks(BasicBlock flowEdge) {
        reachableBasicBlocks.add(flowEdge);
    }

    private void computeSsaEdges() {
        var immediateDominator = new ImmediateDominator(entryBlock);
        var ssaEdges = new HashSet<SsaEdge>();
        var lValueToDefMapping = new HashMap<LValue, StoreInstruction>();

        for (BasicBlock basicBlock : immediateDominator.preorder()) {
            basicBlock.getStoreInstructions()
                      .forEach(
                              storeInstruction -> lValueToDefMapping.put(storeInstruction.getDestination(), storeInstruction)
                      );
        }

        for (BasicBlock basicBlock : immediateDominator.preorder()) {
            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof HasOperand hasOperand) {
                    for (LValue lValue : hasOperand.getOperandLValues()) {
                        if (instruction instanceof ReturnInstruction && !lValueToDefMapping.containsKey(lValue))
                            continue;
                        ssaEdges.add(new SsaEdge(lValueToDefMapping.computeIfAbsent(lValue, key -> {
                            throw new IllegalStateException(lValue + " not found" + basicBlock.getInstructionList());
                        }), hasOperand, basicBlock));
                    }
                }
            }
        }


        this.ssaEdgeList = ssaEdges;
        this.ssaWorkList.addAll(ssaEdges);
    }

    /**
     * ♦ Use two wor lists: SSAWorkList & CFGWorkList:
     * ♦ SSAWorkList determines values.
     * ♦ CFGWorkList governs reachability.
     * ♦ Don’t propagate into operation until its block is reachable.
     */

    public void runWorkList() {
        while (!flowGraphWorkList.isEmpty() || !ssaWorkList.isEmpty()) {
            while (!flowGraphWorkList.isEmpty()) {
                var flowEdge = flowGraphWorkList.pop();
                if (!isReachable(flowEdge)) {
                    setReachableBasicBlocks(flowEdge);
                    // (b) Perform Visit-phi for all the phi functions at the destination node
                    flowEdge.getPhiFunctions()
                            .forEach(this::visitPhi);

                    // (c) If only one of the ExecutableFlags associated with the incoming
                    //     program flow graph edges is true (i.e. this the first time this
                    //     node has been evaluated), then perform VisitExpression for all expressions
                    //     in this node.
                    if (!visitedBasicBlocks.contains(flowEdge)) {
                        flowEdge.getNonPhiInstructions()
                                .forEach(instruction -> visitExpression(instruction, flowEdge));
                        visitedBasicBlocks.add(flowEdge);
                    }

                    // (d) If then node only contains one outgoing flow edge, add that edge to the
                    //     flowWorkList
                    if (flowEdge.hasNoBranchNotNOP()) {
                        flowGraphWorkList.add(flowEdge.getSuccessor());
                    }
                }
            }
            while (!ssaWorkList.isEmpty()) {
                var ssaEdge = ssaWorkList.pop();

                // (4) If the item is an SSA edge from the SSAWorkList and the destination of that
                //     edge is a phi-function, perform visit-phi

                // (5) If the item is an SSA edge from the SSA Work list and the destination of that
                //     edge is an expression, then examine ExecutableFlags for the program flow edges
                //     reaching that node. If any of them are true, perform VisitExpression.
                //     Otherwise, do nothing.

                if (ssaEdge.useIsPhi()) {
                    visitPhi((Phi) ssaEdge.use());
                } else if (countIncomingExecutableEdges(ssaEdge.useSite()) > 0) {
                    visitExpression(ssaEdge.use(), ssaEdge.useSite());
                }
            }
        }
    }

    public Map<Value, LatticeElement> getLatticeValues() {
        return latticeValues;
    }

    public Set<BasicBlock> getReachableBasicBlocks() {
        return reachableBasicBlocks;
    }
}

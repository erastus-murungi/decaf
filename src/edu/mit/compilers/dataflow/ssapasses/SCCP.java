package edu.mit.compilers.dataflow.ssapasses;

import static edu.mit.compilers.utils.TarjanSCC.getReversePostOrder;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

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
    Queue<BasicBlock> flowEdges = new ArrayDeque<>();
    Queue<SsaEdge> ssaEdges = new ArrayDeque<>();
    Set<BasicBlock> reachable = new HashSet<>();

    Map<Value, LatticeElement> valueLatticeElementMap = new HashMap<>();

    Set<BasicBlock> visited = new HashSet<>();

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
                        valueLatticeElementMap.put(v, LatticeElement.top());
                    } else if (v instanceof NumericalConstant numericalConstant) {
                        valueLatticeElementMap.put(v, LatticeElement.constant(numericalConstant.getValue()));
                    }
                }
            }
        }
        this.flowEdges.add(entryBlock);
        getSsaEdges();
    }

    private void visitPhi(Phi phi) {
        var values = phi.getOperandValues()
                        .stream()
                        .map(valueLatticeElementMap::get)
                        .toList();
        valueLatticeElementMap.put(phi.getStore(), LatticeElement.meet(values));
    }

    private void visitExpression(Instruction instruction, BasicBlock basicBlock) {
        if (instruction instanceof CopyInstruction copyInstruction) {
            valueLatticeElementMap.put(copyInstruction.getStore(), valueLatticeElementMap.get(copyInstruction.getValue()));
        } else if (instruction instanceof BinaryInstruction binaryInstruction) {
            var a = valueLatticeElementMap.get(binaryInstruction.fstOperand);
            var b = valueLatticeElementMap.get(binaryInstruction.sndOperand);
            if (!a.isBottom() && !b.isBottom()) {
                if (a.isConstant() && b.isConstant()) {
                    var longVal = Utils.symbolicallyEvaluate(String.format("%d %s %d", a.getValue(), binaryInstruction.operator, b.getValue()));
                    longVal.ifPresent(aLong -> valueLatticeElementMap.put(binaryInstruction.getStore(), LatticeElement.constant(aLong)));
                } else {
                    valueLatticeElementMap.put(binaryInstruction.getStore(), LatticeElement.meet(a, b));
                }
            }
        } else if (instruction instanceof UnaryInstruction unaryInstruction) {
            var a = valueLatticeElementMap.get(unaryInstruction.operand);
            if (a.isConstant()) {
                var longVal = Utils.symbolicallyEvaluate(String.format("%s (%d)", unaryInstruction.operator, a.getValue()));
                longVal.ifPresent(aLong -> valueLatticeElementMap.put(unaryInstruction.getStore(), LatticeElement.constant(aLong)));
            }
        } else if (instruction instanceof ConditionalBranch conditionalBranch) {
            var condition = valueLatticeElementMap.get(conditionalBranch.condition);
            if (LatticeElement.meet(condition, LatticeElement.constant(0L))
                              .isBottom()) {
                flowEdges.add(basicBlock.getTrueTarget());
            }
            if (LatticeElement.meet(condition, LatticeElement.constant(1L))
                              .isBottom()) {
                flowEdges.add(basicBlock.getFalseTarget());
            }
        } else if (instruction instanceof UnconditionalJump unconditionalJump) {
            if (!reachable.contains(unconditionalJump.getTarget()))
                flowEdges.add(unconditionalJump.getTarget());
        }

    }

    public int countIncomingExecutableEdges(BasicBlock block) {
        int count = 0;
        for (var pred : block.getPredecessors()) {
            if (reachable.contains(pred)) {
                count++;
            }
        }
        return count;
    }

    private boolean isReachable(BasicBlock flowEdge) {
        return reachable.contains(flowEdge);
    }

    private void setReachable(BasicBlock flowEdge) {
        reachable.add(flowEdge);
    }

    private void getSsaEdges() {
        ImmediateDominator immediateDominator = new ImmediateDominator(entryBlock);
        Set<SsaEdge> ssaEdges = new HashSet<>();
        Map<LValue, StoreInstruction> lValueToDefMapping = new HashMap<>();
        for (BasicBlock basicBlock : immediateDominator.preorder()) {

            for (Instruction instruction : basicBlock.getInstructionList()) {
                if (instruction instanceof StoreInstruction storeInstruction) {
                    lValueToDefMapping.put(storeInstruction.getStore(), storeInstruction);
                }

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
        this.ssaEdges.addAll(ssaEdges);
    }

    /**
     * ♦ Use two wor lists: SSAWorkList & CFGWorkList:
     * ♦ SSAWorkList determines values.
     * ♦ CFGWorkList governs reachability.
     * ♦ Don’t propagate into operation until its block is reachable.
     */

    public void runWorkList() {
        while (!flowEdges.isEmpty() && !ssaEdges.isEmpty()) {
            while (!flowEdges.isEmpty()) {
                var flowEdge = flowEdges.remove();
                if (!isReachable(flowEdge)) {
                    setReachable(flowEdge);
                    // (b) Perform Visit-phi for all the phi functions at the destination node
                    flowEdge.getPhiFunctions()
                            .forEach(this::visitPhi);

                    // (c) If only one of the ExecutableFlags associated with the incoming
                    //     program flow graph edges is true (i.e. this the first time this
                    //     node has been evaluated), then perform VisitExpression for all expressions
                    //     in this node.
                    if (!visited.contains(flowEdge)) {
                        flowEdge.getNonPhiInstructions()
                                .forEach(instruction -> visitExpression(instruction, flowEdge));
                        visited.add(flowEdge);
                    }

                    // (d) If then node only contains one outgoing flow edge, add that edge to the
                    //     flowWorkList
                    if (flowEdge.hasNoBranchNotNOP()) {
                        flowEdges.add(flowEdge.getSuccessor());
                    }
                }
            }
            while (!ssaEdges.isEmpty()) {
                var ssaEdge = ssaEdges.remove();

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
}

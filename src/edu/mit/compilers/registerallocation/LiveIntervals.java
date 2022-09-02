package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.asm.X64Register;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.GraphVizPrinter;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

public class LiveIntervals {
    final private ProgramIr programIr;

    /**
     * Maps each unique instruction in the program to a set of variables live at that point
     */
    public final Map<Instruction, Set<Value>> instructionToLiveVariablesMap = new HashMap<>();


    /**
     * Maps each method to a list of its live intervals
     */
    public final Map<Method, List<LiveInterval>> methodToLiveIntervalsMap = new HashMap<>();

    public LiveIntervals(ProgramIr programIr) {
        this.programIr = programIr;
        linearizeCfg();
        computeLivenessInformation();
        computeLiveIntervals();
        if (CLI.debug)
            printLinearizedCfg();
        validateLiveIntervals();
    }


    public LiveIntervals(Method method, ProgramIr programIr) {
        this.programIr = programIr;
        linearizeMethodCfg(method);
        computeMethodLivenessInformation(method);
        var liveIntervals = computeMethodLiveIntervals(method);
        validateMethodLiveIntervals(liveIntervals);
        methodToLiveIntervalsMap.put(method, liveIntervals);
    }


    private void printLinearizedCfg() {
        var copy = new HashMap<String, BasicBlock>();
        programIr.methodList.forEach(methodBegin -> copy.put(methodBegin.methodName(), methodBegin.entryBlock));
        GraphVizPrinter.printGraph(copy, basicBlock -> basicBlock.getInstructionList()
                                                                 .toString(), "cfg_ir");
    }

    private void validateMethodLiveIntervals(Collection<LiveInterval> liveIntervals) {
        checkStartComesBeforeEnd(liveIntervals);
    }

    private void validateLiveIntervals() {
        methodToLiveIntervalsMap.values()
                                .forEach(this::checkStartComesBeforeEnd);
//        methodToLiveIntervalsMap.forEach((methodBegin, liveIntervals1) ->
//                checkEveryProgramPointVariablesDontCollide(liveIntervals1,
//                        liveIntervals1.get(0).instructionList,
//                        variableToRegisterMap.get(methodBegin)));
    }

    private void checkEveryProgramPointVariablesDontCollide(Collection<LiveInterval> liveIntervals,
                                                            InstructionList instructionList,
                                                            Map<Value, X64Register> nameToRegister) {
        for (int i = 0; i < instructionList.size(); i++) {
            var names = instructionList.get(i)
                                       .getAllValues()
                                       .stream()
                                       .filter(abstractName -> abstractName instanceof Variable)
                                       .collect(Collectors.toUnmodifiableSet());
            var variables = getLiveIntervalsAtPoint(liveIntervals, i).stream()
                                                                     .map(liveInterval -> liveInterval.variable)
                                                                     .filter(names::contains)
                                                                     .toList();
            boolean allDistinct = variables.stream()
                                           .map(nameToRegister::get)
                                           .distinct()
                                           .count() == variables.size();
            if (!allDistinct) {
                throw new IllegalStateException();
            }

        }
    }


    private List<LiveInterval> getLiveIntervalsAtPoint(Collection<LiveInterval> liveIntervals, int index) {
        return liveIntervals.stream()
                            .filter(liveInterval -> liveInterval.startPoint <= index && liveInterval.endPoint >= index)
                            .collect(Collectors.toList());
    }


    private boolean isLiveAtPoint(Instruction instruction, Value value) {
        return instructionToLiveVariablesMap.get(instruction).contains(value);
    }


    private void computeMethodLivenessInformation(Method method) {
        var liveVariableAnalysis = new LiveVariableAnalysis(method.entryBlock);
        var basicBlocks = DataFlowAnalysis.getReversePostOrder(method.entryBlock);
        basicBlocks.forEach(basicBlock -> {
            if (basicBlock.getInstructionList()
                          .size() > 1)
                throw new IllegalStateException("failed linearization of " + basicBlock.getLinesOfCodeString() + "\n" + basicBlock.getInstructionList()
                                                                                                                                  .toString());
            if (basicBlock.getInstructionList()
                          .size() == 1)
                instructionToLiveVariablesMap.put(basicBlock.getInstructionList()
                                                            .get(0), liveVariableAnalysis.liveOut(basicBlock));
        });
    }


    private void computeLivenessInformation() {
        for (Method method : programIr.methodList) {
            computeMethodLivenessInformation(method);
        }
    }

    private List<LiveInterval> computeMethodLiveIntervals(Method method) {
        var liveIntervalList = new ArrayList<LiveInterval>();
        InstructionList instructionList = TraceScheduler.flattenIr(method, false);
        var allVariables = getLiveVariablesAtInstruction(method.entryBlock.getInstructionList()
                                                                          .get(0));

        // remove global variables
        allVariables.removeAll(programIr.getGlobals());
        // all the variables later used will be on the first line of the program
        for (var variable : allVariables) {
            liveIntervalList.add(new LiveInterval((LValue) variable,
                    findFirstDefinition(instructionList, variable),
                    findLastUse(instructionList, variable), instructionList, method.methodName())

            );
        }
        return liveIntervalList;
    }

    private void computeLiveIntervals() {
        for (var method : programIr.methodList) {
            methodToLiveIntervalsMap.put(method, computeMethodLiveIntervals(method));
        }
    }

    private static int findFirstDefinition(InstructionList instructionList, Value variable) {
        int indexOfInstruction = 0;
        for (Instruction instruction : instructionList) {
            if (instruction.getAllValues()
                           .contains(variable))
                break;
            indexOfInstruction++;
        }
        return indexOfInstruction;
    }

    private int findLastUse(InstructionList instructionList, Value value) {
        int loc = instructionList.size() - 1;
        for (; loc >= 0; loc--) {
            if (getLiveVariablesAtInstruction(instructionList.get(loc)).contains(value))
                break;
        }
        return loc + 1;
    }

    private Set<Value> getLiveVariablesAtInstruction(Instruction instruction) {
        return instructionToLiveVariablesMap.getOrDefault(instruction, Collections.emptySet());
    }

    /**
     * flattens the CFG so that each basic block contains only one instruction which has an operand
     * this makes dataflow analysis easier for register allocation
     */

    private static void linearizeMethodCfg(Method method) {
        DataFlowAnalysis.correctPredecessors(method.entryBlock);
        var basicBlocks = DataFlowAnalysis.getReversePostOrder(method.entryBlock);
        for (BasicBlock basicBlock : basicBlocks) {
            int indexOfInstruction = 0;
            if (basicBlock instanceof NOP) {
                if (basicBlock.getInstructionList()
                              .size() > 1) {
                    var prevInstructions = basicBlock.getInstructionList()
                                                     .subList(0, 1);
                    var rest = new ArrayList<>(basicBlock.getInstructionList()
                                                         .subList(1, basicBlock.getInstructionList()
                                                                               .size() - 1));
                    var block = new BasicBlockBranchLess();
                    block.setInstructionList(new InstructionList(prevInstructions));
                    basicBlock.getInstructionList()
                              .reset(new ArrayList<>(basicBlock.getInstructionList()
                                                               .subList(basicBlock.getInstructionList()
                                                                                  .size() - 1, basicBlock.getInstructionList()
                                                                                                         .size())));
                    splitBasicBlock(rest, block, basicBlock);
                    LiveIntervals.correctSuccessors(basicBlock, block, method);
                }
            } else if (basicBlock instanceof BasicBlockBranchLess) {
                indexOfInstruction += 1;
                if (!basicBlock.getInstructionList()
                               .isEmpty()) {
                    var prevInstructions = basicBlock.getInstructionList()
                                                     .subList(0, indexOfInstruction);
                    var rest = new ArrayList<>(basicBlock.getInstructionList()
                                                         .subList(indexOfInstruction, basicBlock.getInstructionList()
                                                                                                .size()));
                    var prevBasicBlock = (BasicBlockBranchLess) basicBlock;
                    var beforeAutoChild = prevBasicBlock.getSuccessor();

                    basicBlock.getInstructionList()
                              .reset(new ArrayList<>(prevInstructions));

                    prevBasicBlock = splitBasicBlock(rest, prevBasicBlock, beforeAutoChild);
                    if (beforeAutoChild != null) {
                        beforeAutoChild.addPredecessor(prevBasicBlock);
                        beforeAutoChild.removePredecessor(basicBlock);
                    }
                }
            } else {
                // take the branch
                while (indexOfInstruction < basicBlock.getInstructionList()
                                                      .size()) {
                    if (basicBlock.getInstructionList()
                                  .get(indexOfInstruction) instanceof ConditionalBranch)
                        break;
                    indexOfInstruction += 1;
                }
                var instructionsToSplit = new InstructionList(basicBlock.getInstructionList()
                                                                        .subList(0, indexOfInstruction));
                basicBlock.getInstructionList()
                          .reset(new ArrayList<>(basicBlock.getInstructionList()
                                                           .subList(indexOfInstruction, basicBlock.getInstructionList()
                                                                                                  .size())));

                var block = basicBlock;
                BasicBlockBranchLess prevBasicBlock;
                if (!instructionsToSplit.isEmpty()) {
                    var prevInstructions = instructionsToSplit.subList(0, 1);
                    var rest = new ArrayList<>(instructionsToSplit.subList(1, instructionsToSplit.size()));

                    prevBasicBlock = new BasicBlockBranchLess();
                    prevBasicBlock.getInstructionList()
                                  .addAll(prevInstructions);
                    block = prevBasicBlock;

                    splitBasicBlock(rest, prevBasicBlock, basicBlock);
                }
                correctSuccessors(basicBlock, block, method);

            }
        }
        DataFlowAnalysis.correctPredecessors(method.entryBlock);
    }

    private void linearizeCfg() {
        for (Method method : programIr.methodList) {
            linearizeMethodCfg(method);
        }
    }

    private static void correctSuccessors(BasicBlock oldBlock, BasicBlock newBlock, Method method) {
        var predecessors = oldBlock.getPredecessors();
        for (BasicBlock predecessor : predecessors) {
            if (predecessor instanceof BasicBlockBranchLess branchLess) {
                branchLess.setSuccessor(newBlock);
            } else {
                var withBranch = (BasicBlockWithBranch) predecessor;
                if (withBranch.getTrueTarget() == oldBlock) {
                    withBranch.setTrueTarget(newBlock);
                } else {
                    if (withBranch.getFalseTarget() != oldBlock)
                        throw new IllegalStateException();
                    withBranch.setFalseTarget(newBlock);
                }
            }
        }
        if (method.entryBlock.getInstructionList() == oldBlock.getInstructionList()) {
            method.entryBlock = newBlock;
        }
    }

    private static BasicBlockBranchLess splitBasicBlock(ArrayList<Instruction> rest,
                                                        BasicBlockBranchLess prevBasicBlock,
                                                        BasicBlock beforeAutoChild) {
        for (var instruction : rest) {
            var bb = new BasicBlockBranchLess();
            bb.setInstructionList(InstructionList.of(instruction));
            bb.addPredecessor(prevBasicBlock);
            prevBasicBlock.setSuccessor(bb);
            prevBasicBlock = bb;
        }
        prevBasicBlock.setSuccessor(beforeAutoChild);
        return prevBasicBlock;
    }

    private void checkStartComesBeforeEnd(Collection<LiveInterval> liveIntervals) {
        for (var liveInterval : liveIntervals) {
            if (liveInterval.startPoint > liveInterval.endPoint) {
                throw new IllegalStateException(liveInterval.toString());
            }
        }
    }


    public void prettyPrintLiveIntervals(Method method) {
        var liveIntervals = methodToLiveIntervalsMap.get(method);
        if (liveIntervals.isEmpty())
            return;
        var instructionList = liveIntervals.stream().findAny().orElseThrow().getInstructionList();
        var allVariables = liveIntervals.stream().map(LiveInterval::getVariable).distinct().toList();
        Map<Value, Integer> spaces = new HashMap<>();
        int spaceCount = 1;
        for (var variable: allVariables) {
            spaces.put(variable, spaceCount);
            System.out.print(variable);
            System.out.print(" | ");
            spaceCount = (variable.toString().length() + 2);
        }
        System.out.println();
        int index = 0;
        for (var instruction: instructionList) {
            for (var variable: allVariables) {
                System.out.print(" ".repeat(spaces.get(variable)));
                if (isLiveAtPoint(instruction, variable)) {
                    System.out.print(Utils.coloredPrint("X", Utils.ANSIColorConstants.ANSI_GREEN_BOLD));
                } else {
                    System.out.print(" ");
                }
            }
            System.out.format("    %3d:  ", index);
            System.out.println(instruction.syntaxHighlightedToString());
            index += 1;
        }
    }
}

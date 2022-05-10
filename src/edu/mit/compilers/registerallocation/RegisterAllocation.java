package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.asm.X64Register;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

public class RegisterAllocation {
    ProgramIr programIr;
    private final Map<Instruction, BasicBlock> instructionBasicBlockMap = new HashMap<>();
    private final Map<BasicBlock, Set<AbstractName>> blockToLiveOut = new HashMap<>();
    private final Map<MethodBegin, List<LiveInterval>> liveIntervals = new HashMap<>();
    private final Map<MethodBegin, Map<AbstractName, X64Register>> variableToRegisterMapping;
    private final LinearScan linearScan;

    public RegisterAllocation(ProgramIr programIr) {
        this.programIr = programIr;
        linearizeCfg();
        computeInstructionBasicBlock();
        computeLiveness();
        if (CLI.debug)
            printLiveVariables();
        computeLiveIntervals();
        linearScan = new LinearScan(List.of(X64Register.availableRegs), liveIntervals);
        linearScan.allocate();
        variableToRegisterMapping = linearScan.getVariableToRegisterMapping();
        System.out.println(linearScan.getVariableToRegisterMapping());
    }

    public Map<MethodBegin, Map<AbstractName, X64Register>> getVariableToRegisterMapping() {
        return variableToRegisterMapping;
    }

    private void computeLiveness() {
        for (MethodBegin methodBegin : programIr.methodBeginList) {
            var liveVariableAnalysis = new LiveVariableAnalysis(methodBegin.entryBlock);
            var basicBlocks = DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock);
            basicBlocks.forEach(basicBlock -> blockToLiveOut.put(basicBlock, liveVariableAnalysis.liveOut(basicBlock)));
        }
    }


    void printLiveVariables() {
        var output = new ArrayList<String>();
        for (Instruction instruction : programIr.mergeProgram()) {
            // live out
            var liveOut = blockToLiveOut.get(instructionBasicBlockMap.get(instruction));
            if (liveOut != null) {
                var s = instruction.repr()
                        .split("#")[0];
                output.add(s + "\t// live out =  " + prettyPrintLive(liveOut));
            }
        }
        System.out.println(String.join("\n", output));
    }

    private String prettyPrintLive(Set<AbstractName> liveOut) {
        var stringBuilder = new ArrayList<String>();
        for (var variable : liveOut)
            stringBuilder.add(Utils.coloredPrint(variable.repr(), Utils.ANSIColorConstants.ANSI_PURPLE_BOLD));
        return "{" + String.join(", ", stringBuilder) + "}";

    }

    /**
     * flattens the CFG so that each basic block contains only one instruction which has an operand
     * this makes dataflow analysis easier for register allocation
     */

    private void linearizeCfg() {
        for (MethodBegin methodBegin : programIr.methodBeginList) {
            DataFlowAnalysis.correctPredecessors(methodBegin.entryBlock);
            final var string = methodBegin.entryBlock.instructionList.toString();
            var basicBlocks = DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock);
            for (BasicBlock basicBlock : basicBlocks) {
//                if (!basicBlock.instructionList.isEmpty() && basicBlock.instructionList.get(0).repr().contains("L11"))
//                    System.out.println("yes");
//                System.out.println("before");
//                System.out.println(Utils.coloredPrint(basicBlock.instructionList.toString(), Utils.ANSIColorConstants.ANSI_GREEN));
                int indexOfInstruction = 0;
                if (basicBlock instanceof BasicBlockBranchLess) {
                    indexOfInstruction += 1;
                    if (!basicBlock.instructionList.isEmpty()) {
                        var prevInstructions = basicBlock.instructionList.subList(0, indexOfInstruction);
                        var rest = new ArrayList<>(basicBlock.instructionList.subList(indexOfInstruction, basicBlock.instructionList.size()));
                        var prevBasicBlock = (BasicBlockBranchLess) basicBlock;
                        var beforeAutoChild = prevBasicBlock.autoChild;

                        basicBlock.instructionList.reset(new ArrayList<>(prevInstructions));

                        prevBasicBlock = splitBasicBlock(rest, prevBasicBlock, beforeAutoChild, basicBlock.instructionList.nextInstructionList);
                        if (beforeAutoChild != null) {
                            beforeAutoChild.addPredecessor(prevBasicBlock);
                            beforeAutoChild.removePredecessor(basicBlock);
                        }
                    }
                } else {
                    // take the branch
                    while (indexOfInstruction < basicBlock.instructionList.size()) {
                        if (basicBlock.instructionList.get(indexOfInstruction) instanceof ConditionalJump)
                            break;
                        indexOfInstruction += 1;
                    }
                    var instructionsToSplit = new InstructionList(basicBlock.instructionList.subList(0, indexOfInstruction));
                    basicBlock.instructionList.reset(new ArrayList<>(basicBlock.instructionList.subList(indexOfInstruction, basicBlock.instructionList.size())));

                    var block = basicBlock;
                    BasicBlockBranchLess prevBasicBlock;
                    if (!instructionsToSplit.isEmpty()) {
                        var prevInstructions = instructionsToSplit.subList(0, 1);
                        var rest = new ArrayList<>(instructionsToSplit.subList(1, instructionsToSplit.size()));

                        prevBasicBlock = new BasicBlockBranchLess();
                        prevBasicBlock.instructionList.addAll(prevInstructions);
                        block = prevBasicBlock;

                        splitBasicBlock(rest, prevBasicBlock, basicBlock, basicBlock.instructionList);
                    }
                    var predecessors = basicBlock.getPredecessors();
                    for (BasicBlock predecessor : predecessors) {
                        if (predecessor instanceof BasicBlockBranchLess) {
                            var branchLess = (BasicBlockBranchLess) predecessor;
                            branchLess.autoChild = block;
                            if (predecessor.instructionList.nextInstructionList == (basicBlock.instructionList)) {
                                branchLess.instructionList.nextInstructionList = block.instructionList;
                            }
                        } else {
                            var withBranch = (BasicBlockWithBranch) predecessor;
                            if (withBranch.trueChild == basicBlock) {
                                withBranch.trueChild = block;
                                if (predecessor.instructionList.nextInstructionList == (basicBlock.instructionList)) {
                                    withBranch.instructionList.nextInstructionList = block.instructionList;
                                }
                            } else {
                                if (withBranch.falseChild != basicBlock)
                                    throw new IllegalStateException();
                                withBranch.falseChild = block;
                                var bb = withBranch.instructionList;
                                while (bb.nextInstructionList != null && bb.nextInstructionList != (basicBlock.instructionList)) {
                                    bb = bb.nextInstructionList;
                                }
                                if (bb.nextInstructionList == basicBlock.instructionList)
                                    bb.nextInstructionList = block.instructionList;
                            }
                        }
                    }
                }
//                System.out.println("after");
//                System.out.println(Utils.coloredPrint(basicBlock.instructionList.toString(), Utils.ANSIColorConstants.ANSI_RED));
                if (!methodBegin.entryBlock.instructionList.toString().equals(string))
                    throw new IllegalStateException();
            }
            DataFlowAnalysis.correctPredecessors(methodBegin.entryBlock);
        }
    }

    private BasicBlockBranchLess splitBasicBlock(ArrayList<Instruction> rest,
                                                 BasicBlockBranchLess prevBasicBlock,
                                                 BasicBlock beforeAutoChild, InstructionList nextInstructionList) {
        for (var instruction : rest) {
            var bb = new BasicBlockBranchLess();
            bb.instructionList = InstructionList.of(instruction);
            bb.addPredecessor(prevBasicBlock);
            prevBasicBlock.autoChild = bb;
            prevBasicBlock.instructionList.nextInstructionList = bb.instructionList;
            prevBasicBlock = bb;
        }
        prevBasicBlock.autoChild = beforeAutoChild;
        prevBasicBlock.instructionList.nextInstructionList = nextInstructionList;
        return prevBasicBlock;
    }

    private void computeInstructionBasicBlock() {
        for (MethodBegin methodBegin : programIr.methodBeginList) {
            for (BasicBlock basicBlock : DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock)) {
                for (InstructionList instructionList : basicBlock.instructionList.getListOfInstructionLists()) {
                    for (Instruction instruction : instructionList) {
                        instructionBasicBlockMap.put(instruction, basicBlock);
                    }
                }
            }
        }
    }

    private Set<AbstractName> getLive(Instruction instruction) {
        return blockToLiveOut.get(instructionBasicBlockMap.get(instruction));
    }

    private int findFirstDefinition(InstructionList instructionList, AbstractName variable) {
        int indexOfInstruction = 0;
        for (Instruction instruction : instructionList) {
            if (instruction instanceof Store && ((Store) instruction).store.equals(variable)) {
                break;
            }
            indexOfInstruction++;
        }
        return indexOfInstruction;
    }

    private int findLastUse(InstructionList instructionList, AbstractName abstractName) {
        int loc = instructionList.size() - 1;
        for (;loc >= 0; loc--) {
            if (getLive(instructionList.get(loc)).contains(abstractName))
                break;
        }
        return loc + 1;
    }

    private void computeLiveIntervals() {
        for (var methodBegin : programIr.methodBeginList) {
            var liveIntervalList = new ArrayList<LiveInterval>();
            InstructionList instructionList = methodBegin.entryBlock.instructionList.flatten();
            var allVariables = getLive(methodBegin.entryBlock.instructionList.get(0));
            // all the variables later used will be on the first line of the program
//            System.out.println(allVariables);
            for (var variable: allVariables) {
                liveIntervalList.add(new LiveInterval((AssignableName) variable,
                        findFirstDefinition(instructionList, variable),
                        findLastUse(instructionList, variable), instructionList, methodBegin.methodName())

                );
            }
            liveIntervals.put(methodBegin, liveIntervalList);
//            System.out.println(liveIntervals);
        }
    }
}

package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.mit.compilers.asm.X64Register;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.GraphVizPrinter;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

public class RegisterAllocation {
    private final ProgramIr programIr;
    private final Map<Instruction, Set<AbstractName>> instructionToLiveVariablesMap = new HashMap<>();
    private final Map<MethodBegin, List<LiveInterval>> methodToLiveIntervalsMap = new HashMap<>();
    private final Map<MethodBegin, Map<AbstractName, X64Register>> variableToRegisterMap = new HashMap<>();
    private final Map<MethodBegin, Map<Instruction, Set<X64Register>>> methodToLiveRegistersInfo = new HashMap<>();
    private Set<AbstractName> globalVariables;

    private void getGlobals() {
        globalVariables = programIr.mergeProgram()
                .stream()
                .filter(instruction -> instruction instanceof GlobalAllocation)
                .map(instruction -> (GlobalAllocation) instruction)
                .map(globalAllocation -> globalAllocation.variableName)
                .filter(abstractName -> abstractName.size == Utils.WORD_SIZE)
                .collect(Collectors.toUnmodifiableSet());
    }


    private Map<Instruction, Set<X64Register>> computeInstructionToLiveRegistersMap(List<LiveInterval> liveIntervals, Map<AbstractName, X64Register> registerMap) {
        var instructionToLiveRegistersMap = new HashMap<Instruction, Set<X64Register>>();
        if (!liveIntervals.isEmpty()) {
            var instructionList = liveIntervals.get(0).instructionList;
            IntStream.range(0, instructionList.size())
                    .forEach(indexOfInstruction -> instructionToLiveRegistersMap.put(instructionList.get(indexOfInstruction),
                            getLiveRegistersAtPoint(liveIntervals, indexOfInstruction, registerMap)));
        }
        return instructionToLiveRegistersMap;
    }

    private void computeMethodToLiveRegistersInfo() {
        methodToLiveIntervalsMap.forEach(((methodBegin, liveIntervals) ->
                methodToLiveRegistersInfo.put(
                        methodBegin,
                        computeInstructionToLiveRegistersMap(liveIntervals,
                                variableToRegisterMap.get(methodBegin))
                )));
    }

    private void printLinearizedCfg() {
        var copy = new HashMap<String, BasicBlock>();
        programIr.methodBeginList.forEach(methodBegin -> copy.put(methodBegin.methodName(), methodBegin.entryBlock));
        GraphVizPrinter.printGraph(copy,
                (basicBlock -> basicBlock.instructionList.stream()
                        .map(Instruction::repr)
                        .collect(Collectors.joining("\n"))),
                "cfg_ir"
        );
    }

    public RegisterAllocation(ProgramIr programIr) {
        this.programIr = programIr;
        getGlobals();
        linearizeCfg();
        computeLiveness();
        computeLiveIntervals();
        LinearScan linearScan = new LinearScan(List.of(X64Register.regsToAllocate), methodToLiveIntervalsMap);
        linearScan.allocate();
        variableToRegisterMap.putAll(linearScan.getVariableToRegisterMapping());
        computeMethodToLiveRegistersInfo();
        validateLiveIntervals();
        if (CLI.debug) {
            printLiveVariables();
            printLinearizedCfg();
            programIr.methodBeginList.forEach(this::printLiveIntervals);
            variableToRegisterMap.forEach(((methodBegin, registerMap) ->
                    System.out.println(registerMap)));
        }
    }

    public Map<MethodBegin, Map<AbstractName, X64Register>> getVariableToRegisterMap() {
        return variableToRegisterMap;
    }

    public Map<MethodBegin, Map<Instruction, Set<X64Register>>> getMethodToLiveRegistersInfo() {
        return methodToLiveRegistersInfo;
    }

    private void computeLiveness() {
        for (MethodBegin methodBegin : programIr.methodBeginList) {
            var liveVariableAnalysis = new LiveVariableAnalysis(methodBegin.entryBlock);
            var basicBlocks = DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock);
            basicBlocks.forEach(basicBlock -> {
                if (basicBlock.instructionList.size() > 1)
                    throw new IllegalStateException("failed linearization of " + basicBlock.getLabel());
                if (basicBlock.instructionList.size() == 1)
                    instructionToLiveVariablesMap.put(basicBlock.instructionList.get(0), liveVariableAnalysis.liveOut(basicBlock));
            });
        }
    }


    void printLiveVariables() {
        printLiveVariables(programIr.mergeProgram());
    }

    void printLiveVariables(InstructionList instructionList) {
        var output = new ArrayList<String>();
        int index = 0;
        for (Instruction instruction : instructionList) {
            var liveOut = instructionToLiveVariablesMap.get(instruction);
            if (liveOut != null) {
                var s = instruction.repr()
                        .split("#")[0];
                output.add(String.format("%3d:    ", index) + s + "\t// live out =  " + prettyPrintLive(liveOut));
            } else {
                output.add(String.format("%3d:    ", index) + instruction.repr());
            }
            index += 1;
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

                    var toUpdate = new ArrayList<InstructionList>();
                    for (BasicBlock predecessor : predecessors) {
                        if (predecessor instanceof BasicBlockBranchLess) {
                            var branchLess = (BasicBlockBranchLess) predecessor;
                            branchLess.autoChild = block;
                            if (branchLess.instructionList.nextInstructionList == (basicBlock.instructionList)) {
                                toUpdate.add(branchLess.instructionList);
                            }
                        } else {
                            var withBranch = (BasicBlockWithBranch) predecessor;
                            if (withBranch.trueChild == basicBlock) {
                                withBranch.trueChild = block;
                                var bb = withBranch.instructionList;
                                while (bb.nextInstructionList != null && bb.nextInstructionList != (basicBlock.instructionList)) {
                                    bb = bb.nextInstructionList;
                                }
                                if (bb.nextInstructionList == (basicBlock.instructionList)) {
                                    toUpdate.add(bb);
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
                                    toUpdate.add(bb);
                            }
                        }
                    }
                    BasicBlock finalBlock = block;
                    toUpdate.forEach(instructionList -> instructionList.nextInstructionList = finalBlock.instructionList);

                    if (methodBegin.entryBlock.instructionList == basicBlock.instructionList) {
                        methodBegin.entryBlock.instructionList = block.instructionList;
                    }
                }
                if (!methodBegin.entryBlock.instructionList.toString()
                        .equals(string)) {
                    System.out.println(string);
                    System.out.println("*******");
                    System.out.println(methodBegin.entryBlock.instructionList.toString());
                    throw new IllegalStateException();
                }
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

    private Set<AbstractName> getLive(Instruction instruction) {
        return instructionToLiveVariablesMap.getOrDefault(instruction, Collections.emptySet());
    }

    private int findFirstDefinition(InstructionList instructionList, AbstractName variable) {
        int indexOfInstruction = 0;
        for (Instruction instruction : instructionList) {
            if (instruction.getAllNames().contains(variable))
                break;
            indexOfInstruction++;
        }
        return indexOfInstruction;
    }

    private int findLastUse(InstructionList instructionList, AbstractName abstractName) {
        int loc = instructionList.size() - 1;
        for (; loc >= 0; loc--) {
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

            // remove global variables
            allVariables.removeAll(globalVariables);
            // all the variables later used will be on the first line of the program
            for (var variable : allVariables) {
                liveIntervalList.add(new LiveInterval((AssignableName) variable,
                        findFirstDefinition(instructionList, variable),
                        findLastUse(instructionList, variable), instructionList, methodBegin.methodName())

                );
            }
            methodToLiveIntervalsMap.put(methodBegin, liveIntervalList);
        }
    }

    private void checkStartComesBeforeEnd(Collection<LiveInterval> liveIntervals) {
        for (var liveInterval : liveIntervals) {
            if (liveInterval.startPoint > liveInterval.endPoint) {
                printLiveVariables(liveInterval.instructionList);
                throw new IllegalStateException(liveInterval.toString());
            }
        }

    }

    private List<LiveInterval> getLiveIntervalsAtPoint(Collection<LiveInterval> liveIntervals, int index) {
        return liveIntervals.stream()
                .filter(liveInterval -> liveInterval.startPoint <= index && liveInterval.endPoint >= index)
                .collect(Collectors.toList());
    }

    private Set<X64Register> getLiveRegistersAtPoint(Collection<LiveInterval> liveIntervals, int index, Map<AbstractName, X64Register> registerMap) {
        return liveIntervals.stream()
                .filter(liveInterval -> liveInterval.startPoint <= index && liveInterval.endPoint >= index)
                .map(liveInterval -> liveInterval.variable)
                .filter(name -> !globalVariables.contains(name))
                .map(registerMap::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private void checkEveryProgramPointVariablesDontCollide(Collection<LiveInterval> liveIntervals,
                                                            InstructionList instructionList,
                                                            Map<AbstractName, X64Register> nameToRegister) {
        for (int i = 0; i < instructionList.size(); i++) {
            Set<AbstractName> names = instructionList.get(i)
                    .getAllNames()
                    .stream()
                    .filter(abstractName -> abstractName instanceof VariableName)
                    .collect(Collectors.toUnmodifiableSet());
            List<AssignableName> variables = getLiveIntervalsAtPoint(liveIntervals, i).stream()
                    .map(liveInterval -> liveInterval.variable)
                    .filter(names::contains)
                    .collect(Collectors.toList());
            boolean allDistinct = variables.stream()
                    .map(nameToRegister::get)
                    .distinct()
                    .count() == variables.size();
            if (!allDistinct) {
                throw new IllegalStateException();
            }

        }
    }

    private void printLiveIntervals(MethodBegin methodBegin) {
        // get all variables
        int maxIntervalLength = methodBegin.entryBlock.instructionList.flatten().size();
        var liveIntervals = this.methodToLiveIntervalsMap.get(methodBegin);
        int maxVariableLength = liveIntervals.stream().map(liveInterval -> liveInterval.variable).map(AssignableName::repr).mapToInt(String::length).max().orElse(0) + 2;
        String[] colors = {Utils.ANSIColorConstants.ANSI_GREEN, Utils.ANSIColorConstants.ANSI_CYAN, Utils.ANSIColorConstants.ANSI_PURPLE, Utils.ANSIColorConstants.ANSI_YELLOW,  Utils.ANSIColorConstants.ANSI_RED};
        int indexOfLiveInterval = 0;
        var toPrint = new ArrayList<String>();
        toPrint.add("live intervals: " + methodBegin.methodName());
        toPrint.add(String.format("%s%s", " ".repeat(maxVariableLength), IntStream.range(0, maxIntervalLength).mapToObj(integer -> String.format("%03d", integer)).collect(Collectors.joining(" "))));
        int sizeOfHeaderInstructions = programIr.getSizeOfHeaderInstructions();
        for (var liveInterval: liveIntervals) {
            // pick a color
            String color = colors[indexOfLiveInterval % colors.length];
            String repr = liveInterval.variable.repr();
            toPrint.add(String.format("%s%s%s", repr + " ".repeat(maxVariableLength - repr.length()), "    ".repeat(liveInterval.startPoint + sizeOfHeaderInstructions), Utils.coloredPrint(" *  ", color).repeat(liveInterval.endPoint - liveInterval.startPoint + sizeOfHeaderInstructions)));
            indexOfLiveInterval += 1;
        }
        System.out.println(String.join("\n", toPrint));
        System.out.println(liveIntervals);
    }

    private void validateLiveIntervals() {
        methodToLiveIntervalsMap.values()
                .forEach(this::checkStartComesBeforeEnd);
//        methodToLiveIntervalsMap.forEach((methodBegin, liveIntervals1) ->
//                checkEveryProgramPointVariablesDontCollide(liveIntervals1,
//                        liveIntervals1.get(0).instructionList,
//                        variableToRegisterMap.get(methodBegin)));
    }
}

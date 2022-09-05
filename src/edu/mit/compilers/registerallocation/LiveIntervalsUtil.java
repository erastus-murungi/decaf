package edu.mit.compilers.registerallocation;

import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.analyses.LiveVariableAnalysis;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.TarjanSCC;
import edu.mit.compilers.utils.Utils;

public class LiveIntervalsUtil {
    /**
     * Maps each unique instruction in the program to a set of variables live at that point
     */
    public final Map<Instruction, Set<Value>> instructionToLiveVariablesMap = new HashMap<>();
    /**
     * Maps each method to a list of its live intervals
     */
    public final Map<Method, List<LiveInterval>> methodToLiveIntervalsMap = new HashMap<>();
    final private ProgramIr programIr;
    /**
     * value to live intervals
     */

    private final Map<Method, Map<Value, LiveInterval>> methodToMappingOfValuesToLiveIntervals = new HashMap<>();

    public LiveIntervalsUtil(ProgramIr programIr) {
        this.programIr = programIr;
        computeLivenessInformation();
        computeLiveIntervals();
        validateLiveIntervals();
    }


    public LiveIntervalsUtil(Method method, ProgramIr programIr) {
        this.programIr = programIr;
        computeMethodLivenessInformation(method);
        var liveIntervals = computeMethodLiveIntervals(method);
        validateMethodLiveIntervals(liveIntervals);
        methodToLiveIntervalsMap.put(method, liveIntervals);
    }

    private static int findFirstDefSlot(InstructionList instructionList, Value variable) {
        int indexOfInstruction = 0;
        for (Instruction instruction : instructionList) {
            if (instruction.getAllValues()
                    .contains(variable))
                break;
            indexOfInstruction++;
        }
        return indexOfInstruction;
    }

    public static boolean liveIntervalsInterfere(LiveInterval a, LiveInterval b) {
        var overlap = Math.abs(Math.min(a.startPoint(), b.endPoint() - 1) - Math.max(a.startPoint(), b.endPoint() - 1));
        return overlap > 0;
    }

    public Collection<LiveInterval> getLiveIntervals(Method method) {
        return methodToLiveIntervalsMap.get(method);
    }

    public Map<Value, LiveInterval> getValueToLiveIntervalMappingForMethod(Method method) {
        return methodToMappingOfValuesToLiveIntervals.get(method);
    }

    private void validateMethodLiveIntervals(Collection<LiveInterval> liveIntervals) {
        checkStartComesBeforeEnd(liveIntervals);
    }

    private void validateLiveIntervals() {
        programIr.methodList.forEach(
                method ->
                        validateMethodLiveIntervals(getLiveIntervals(method))
        );
    }

    private boolean isLiveAtPoint(Method method, Value value, int index) {
        var liveInterval = methodToMappingOfValuesToLiveIntervals.get(method)
                .get(value);
        return liveInterval.startPoint() <= index && index < liveInterval.endPoint();
    }

    private void computeMethodLivenessInformation(Method method) {
        var basicBlocks = TarjanSCC.getReversePostOrder(method.entryBlock);
        basicBlocks.forEach(basicBlock ->
                computeLiveOutForSetBasicBlock(basicBlock, new LiveVariableAnalysis(method.entryBlock)));
    }

    private Set<Value> ref(Instruction instruction) {
        if (instruction instanceof HasOperand hasOperand)
            return new HashSet<>(hasOperand.getOperandLValues());
        return Collections.emptySet();
    }

    private Set<Value> def(Instruction instruction) {
        if (instruction instanceof StoreInstruction storeInstruction)
            return Set.of(storeInstruction.getDestination());
        return Collections.emptySet();
    }

    private void computeLiveOutForSetBasicBlock(BasicBlock basicBlock, LiveVariableAnalysis liveVariableAnalysis) {
        var liveIn = liveVariableAnalysis.liveIn(basicBlock);
        var liveOut = liveVariableAnalysis.liveOut(basicBlock);

        if (liveIn.equals(liveOut))
            basicBlock.getInstructionList()
                    .forEach(
                            instruction -> instructionToLiveVariablesMap.put(instruction, new HashSet<>(liveIn))
                    );

        Set<Value> outLive = new HashSet<>(liveOut);

        // values which are live outside this block and therefore should be conserved
        Set<Value> outGlobal = Sets.difference(outLive, Sets.difference(outLive, basicBlock.getStoreInstructions()
                .stream()
                .map(StoreInstruction::getDestination)
                .collect(Collectors.toUnmodifiableSet())));

        for (Instruction instruction : basicBlock.getInstructionListReversed()) {
            outLive = Sets.union(Sets.union(Sets.difference(outLive, def(instruction)), ref(instruction)), outGlobal);
            instructionToLiveVariablesMap.put(instruction, new HashSet<>(outLive));
        }
    }

    private void computeLivenessInformation() {
        for (Method method : programIr.methodList) {
            computeMethodLivenessInformation(method);
        }
    }

    private List<LiveInterval> computeMethodLiveIntervals(Method method) {
        var liveIntervalList = new ArrayList<LiveInterval>();
        var instructionList = TraceScheduler.flattenIr(method);
        var allVariables = Utils.getAllLValuesInInstructionList(instructionList);

        // remove global variables
        allVariables.removeAll(programIr.getGlobals());

        var varToLiveInterval = new HashMap<Value, LiveInterval>();
        for (var variable : allVariables) {
            var defSlot = findFirstDefSlot(instructionList, variable);
            var lastUseSlot = findLastUseSlot(instructionList, variable);
            // handles the case when a variable is defined but never used in the program
            if (lastUseSlot == 0)
                lastUseSlot = defSlot + 1;
            var liveInterval = new LiveInterval(variable, defSlot, lastUseSlot, instructionList, method.methodName());
            liveIntervalList.add(liveInterval);
            varToLiveInterval.put(variable, liveInterval);
        }
        methodToMappingOfValuesToLiveIntervals.put(method, varToLiveInterval);
        return liveIntervalList;
    }

    private void computeLiveIntervals() {
        for (var method : programIr.methodList) {
            methodToLiveIntervalsMap.put(method, computeMethodLiveIntervals(method));
        }
    }

    private int findLastUseSlot(InstructionList instructionList, Value value) {
        int loc = instructionList.size() - 1;
        for (; loc >= 0; loc--) {
            var instruction = instructionList.get(loc);
            if (instructionToLiveVariablesMap.getOrDefault(instruction, Collections.emptySet())
                    .contains(value))
                break;
        }
        return loc + 1;
    }

    private void checkStartComesBeforeEnd(Collection<LiveInterval> liveIntervals) {
        for (var liveInterval : liveIntervals) {
            if (liveInterval.startPoint() > liveInterval.endPoint()) {
                throw new IllegalStateException(liveInterval.toString());
            }
        }
    }

    public void prettyPrintLiveIntervals(Method method) {
        var liveIntervals = getLiveIntervals(method);
        if (liveIntervals.isEmpty())
            return;
        var instructionList = liveIntervals.stream()
                .findAny()
                .orElseThrow()
                .instructionList();
        var allVariables = liveIntervals.stream()
                .map(LiveInterval::variable)
                .distinct()
                .toList();
        Map<Value, Integer> spaces = new HashMap<>();
        int spaceCount = 1;
        for (var variable : allVariables) {
            spaces.put(variable, spaceCount);
            System.out.print(variable);
            System.out.print(" | ");
            spaceCount = (variable.toString()
                    .length() + 2);
        }
        System.out.println();
        int index = 0;
        for (var instruction : instructionList) {
            for (var variable : allVariables) {
                System.out.print(" ".repeat(spaces.get(variable)));
                if (isLiveAtPoint(method, variable, index)) {
                    System.out.print(Utils.coloredPrint("\u2588", Utils.ANSIColorConstants.ANSI_GREEN_BOLD));
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

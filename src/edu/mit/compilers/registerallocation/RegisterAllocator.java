package edu.mit.compilers.registerallocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.utils.ProgramIr;

public class RegisterAllocator {
    public final Map<Method, Map<IrValue, X64RegisterType>> variableToRegisterMap = new HashMap<>();

    public final Map<Method, Map<Instruction, Set<X64RegisterType>>> methodToLiveRegistersInfo = new HashMap<>();

    public final Map<Method, Map<Instruction, Set<IrValue>>> methodToLiveValuesInfo = new HashMap<>();

    public final LiveIntervalsUtil liveIntervalsUtil;

    public RegisterAllocator(ProgramIr programIr) {
        liveIntervalsUtil = new LiveIntervalsUtil(programIr);
        var linearScan = new LinearScan(List.copyOf(X64RegisterType.regsToAllocate), liveIntervalsUtil.methodToLiveIntervalsMap);
        linearScan.allocate();
        variableToRegisterMap.putAll(linearScan.getVariableToRegisterMapping());
        computeMethodToLiveRegistersInfo(programIr, liveIntervalsUtil.methodToLiveIntervalsMap);
    }

    private static Map<Instruction, Set<X64RegisterType>> computeInstructionToLiveRegistersMap(ProgramIr programIr, List<LiveInterval> liveIntervals, Map<IrValue, X64RegisterType> registerMap) {
        var instructionToLiveRegistersMap = new HashMap<Instruction, Set<X64RegisterType>>();
        if (!liveIntervals.isEmpty()) {
            var instructionList = liveIntervals.get(0).instructionList();
            IntStream.range(0, instructionList.size())
                    .forEach(indexOfInstruction ->
                            instructionToLiveRegistersMap.put(instructionList.get(indexOfInstruction), getLiveRegistersAtPoint(programIr, liveIntervals, indexOfInstruction, registerMap)));
        }
        return instructionToLiveRegistersMap;
    }

    private static Map<Instruction, Set<IrValue>> computeInstructionToLiveValuesMap(
            List<LiveInterval> liveIntervals) {
        var instructionToLiveRegistersMap = new HashMap<Instruction, Set<IrValue>>();
        if (!liveIntervals.isEmpty()) {
            var instructionList = liveIntervals.get(0).instructionList();
            IntStream.range(0, instructionList.size())
                     .forEach(indexOfInstruction ->
                             instructionToLiveRegistersMap.put(instructionList.get(indexOfInstruction), getLiveValuesAtPoint(liveIntervals, indexOfInstruction)));
        }
        return instructionToLiveRegistersMap;
    }

    private static Set<X64RegisterType> getLiveRegistersAtPoint(ProgramIr programIr, Collection<LiveInterval> liveIntervals, int index, Map<IrValue, X64RegisterType> registerMap) {
        return liveIntervals.stream()
                .filter(liveInterval -> liveInterval.startPoint() <= index && index < liveInterval.endPoint())
                .map(LiveInterval::irAssignableValue)
                .filter(name -> !programIr.getGlobals().contains(name))
                .map(registerMap::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<IrValue> getLiveValuesAtPoint(Collection<LiveInterval> liveIntervals, int index) {
        return liveIntervals.stream()
                            .filter(liveInterval -> liveInterval.startPoint() <= index && index < liveInterval.endPoint())
                            .map(LiveInterval::irAssignableValue)
                            .collect(Collectors.toUnmodifiableSet());
    }

    private void computeMethodToLiveRegistersInfo(ProgramIr programIr, Map<Method, List<LiveInterval>> methodToLiveIntervalsMap) {
        methodToLiveIntervalsMap.forEach(((methodBegin, liveIntervals) ->
                methodToLiveRegistersInfo.put(
                        methodBegin,
                        computeInstructionToLiveRegistersMap(programIr, liveIntervals,
                                variableToRegisterMap.get(methodBegin))
                )));
        methodToLiveIntervalsMap.forEach(((methodBegin, liveIntervals) ->
                methodToLiveValuesInfo.put(
                        methodBegin,
                        computeInstructionToLiveValuesMap(liveIntervals)
                )));
    }

    public Map<Method, Map<IrValue, X64RegisterType>> getVariableToRegisterMap() {
        return variableToRegisterMap;
    }

    public Map<Method, Map<Instruction, Set<X64RegisterType>>> getMethodToLiveRegistersInfo() {
        return methodToLiveRegistersInfo;
    }

}

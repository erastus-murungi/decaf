package decaf.regalloc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import decaf.asm.X86Register;
import decaf.codegen.codes.Instruction;
import decaf.common.ProgramIr;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrValue;

public class RegisterAllocator {
    public final Map<Method, Map<IrValue, X86Register>> variableToRegisterMap = new HashMap<>();

    public final Map<Method, Map<Instruction, Set<X86Register>>> methodToLiveRegistersInfo = new HashMap<>();

    public final Map<Method, Map<Instruction, Set<IrValue>>> methodToLiveValuesInfo = new HashMap<>();

    private final LiveIntervalsManager liveIntervalsManager;

    public RegisterAllocator(ProgramIr programIr) {
        liveIntervalsManager = new LiveIntervalsManager(programIr);
        var linearScan = new LinearScan(List.copyOf(X86Register.regsToAllocate), getLiveIntervalsManager().methodToLiveIntervalsMap);
        linearScan.allocate();
        variableToRegisterMap.putAll(linearScan.getVariableToRegisterMapping());
        computeMethodToLiveRegistersInfo(programIr, getLiveIntervalsManager().methodToLiveIntervalsMap);
    }

    private static Map<Instruction, Set<X86Register>> computeInstructionToLiveRegistersMap(ProgramIr programIr, List<LiveInterval> liveIntervals, Map<IrValue, X86Register> registerMap) {
        var instructionToLiveRegistersMap = new HashMap<Instruction, Set<X86Register>>();
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

    private static Set<X86Register> getLiveRegistersAtPoint(ProgramIr programIr, Collection<LiveInterval> liveIntervals, int index, Map<IrValue, X86Register> registerMap) {
        return liveIntervals.stream()
                .filter(liveInterval -> liveInterval.startPoint() <= index && index < liveInterval.endPoint())
                .map(LiveInterval::irSsaRegister)
                .filter(name -> !programIr.getGlobals().contains(name))
                .map(registerMap::get)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<IrValue> getLiveValuesAtPoint(Collection<LiveInterval> liveIntervals, int index) {
        return liveIntervals.stream()
                            .filter(liveInterval -> liveInterval.startPoint() <= index && index < liveInterval.endPoint())
                            .map(LiveInterval::irSsaRegister)
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

    public Map<Method, Map<IrValue, X86Register>> getVariableToRegisterMap() {
        return variableToRegisterMap;
    }

    public Map<Method, Map<Instruction, Set<X86Register>>> getMethodToLiveRegistersInfo() {
        return methodToLiveRegistersInfo;
    }

    public LiveIntervalsManager getLiveIntervalsManager() {
        return liveIntervalsManager;
    }
}

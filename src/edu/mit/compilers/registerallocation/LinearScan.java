package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.asm.X64Register;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

public class LinearScan {
    private final List<X64Register> availableRegisters = new ArrayList<>();
    private List<LiveInterval> active = new ArrayList<>();
    private final Map<MethodBegin, Map<AbstractName, X64Register>> varToRegMap = new HashMap<>();
    private final Map<MethodBegin, List<LiveInterval>> liveIntervals;

    public LinearScan(Collection<X64Register> availableRegisters, Map<MethodBegin, List<LiveInterval>> liveIntervals) {
        this.availableRegisters.addAll(availableRegisters);
        this.liveIntervals = liveIntervals;
    }

    public Map<MethodBegin, Map<AbstractName, X64Register>> getVariableToRegisterMapping() {
        return varToRegMap;
    }

    private Set<AssignableName> mapParametersToRegisters(MethodBegin methodBegin) {
        var parameters = methodBegin.getParameterNames();
        varToRegMap.put(methodBegin, new HashMap<>());
        for (int i = 0; i < Math.min(parameters.size(), X64Register.N_ARG_REGISTERS); i++) {
            varToRegMap.get(methodBegin).put(parameters.get(i), X64Register.argumentRegs[i]);
            availableRegisters.remove(X64Register.argumentRegs[i]);
        }
        return parameters.stream().collect(Collectors.toUnmodifiableSet());
    }

    public void allocate() {
        for (var entry : liveIntervals.entrySet()) {
            availableRegisters.clear();
            availableRegisters.addAll(List.of(X64Register.regsToAllocate));
            Set<AssignableName> assignableNames = mapParametersToRegisters(entry.getKey());
            List<LiveInterval> liveIntervalsList = entry.getValue();
            liveIntervalsList.sort(LiveInterval::compareStartPoint);
            Map<AbstractName, X64Register> varToReg = varToRegMap.get(entry.getKey());
            active = new ArrayList<>();
            for (LiveInterval i : liveIntervalsList) {
                if (assignableNames.contains(i.variable))
                    continue;
                expireOldIntervals(i, varToReg);
                if (active.size() == X64Register.N_AVAILABLE_REGISTERS) {
                    spillAtInterval(i, varToReg);
                } else {
                    varToReg.put(i.variable, availableRegisters.remove(0));
                    active.add(i);
                    active.sort(LiveInterval::compareEndpoint);
                }
            }
        }
//        System.out.println(varToRegMap);
    }

    public void expireOldIntervals(LiveInterval i, Map<AbstractName, X64Register> varToReg) {
        active.sort(LiveInterval::compareEndpoint);
        var toRemove = new ArrayList<LiveInterval>();
        for (LiveInterval j : active) {
            if (j.endPoint >= i.startPoint) {
                for (LiveInterval interval : toRemove) active.remove(interval);
                return;
            }
            toRemove.add(j);
            availableRegisters.add(varToReg.get(j.variable));
        }
        for (LiveInterval j : toRemove) active.remove(j);
    }

    public void spillAtInterval(LiveInterval i, Map<AbstractName, X64Register> varToReg) {
        LiveInterval spill = active.get(active.size() - 1);
        if (spill.endPoint > i.endPoint) {
            varToReg.put(i.variable, varToReg.get(spill.variable));
            varToReg.put(spill.variable, X64Register.STACK);
            active.remove(spill);
            active.add(i);
            active.sort(LiveInterval::compareEndpoint);
        } else {
            varToReg.put(i.variable, X64Register.STACK);
        }
    }
}
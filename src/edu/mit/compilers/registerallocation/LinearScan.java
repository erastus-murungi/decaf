package edu.mit.compilers.registerallocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.IrValue;

public class LinearScan {
    private final List<X64RegisterType> availableRegisters = new ArrayList<>();
    private final Map<Method, Map<IrValue, X64RegisterType>> varToRegMap = new HashMap<>();
    private final Map<Method, List<LiveInterval>> liveIntervals;
    private List<LiveInterval> active = new ArrayList<>();

    public LinearScan(Collection<X64RegisterType> availableRegisters, Map<Method, List<LiveInterval>> liveIntervals) {
        this.availableRegisters.addAll(availableRegisters);
        this.liveIntervals = liveIntervals;
    }

    public Map<Method, Map<IrValue, X64RegisterType>> getVariableToRegisterMapping() {
        return varToRegMap;
    }

    public void allocate() {
        for (var entry : liveIntervals.entrySet()) {
            varToRegMap.put(entry.getKey(), new HashMap<>());
            availableRegisters.clear();
            availableRegisters.addAll(List.copyOf(X64RegisterType.regsToAllocate));
            final var N_AVAILABLE_REGISTERS = availableRegisters.size();
            List<LiveInterval> liveIntervalsList = entry.getValue();
            liveIntervalsList.sort(LiveInterval::compareStartPoint);
            Map<IrValue, X64RegisterType> varToReg = varToRegMap.get(entry.getKey());
            active = new ArrayList<>();
            for (LiveInterval i : liveIntervalsList) {
                expireOldIntervals(i, varToReg);
                if (active.size() == N_AVAILABLE_REGISTERS) {
                    spillAtInterval(i, varToReg);
                } else {
                    varToReg.put(i.irAssignableValue(), availableRegisters.remove(0));
                    active.add(i);
                    active.sort(LiveInterval::compareEndpoint);
                }
            }
        }
    }

    public void expireOldIntervals(LiveInterval i, Map<IrValue, X64RegisterType> varToReg) {
        active.sort(LiveInterval::compareEndpoint);
        for (LiveInterval j : new ArrayList<>(active)) {
            if (j.endPoint() >= i.startPoint()) {
                return;
            }
            active.remove(j);
            availableRegisters.add(varToReg.get(j.irAssignableValue()));
        }
    }

    public void spillAtInterval(LiveInterval i, Map<IrValue, X64RegisterType> varToReg) {
        LiveInterval spill = active.get(active.size() - 1);
        if (spill.endPoint() > i.endPoint()) {
            varToReg.put(i.irAssignableValue(), varToReg.get(spill.irAssignableValue()));
            varToReg.put(spill.irAssignableValue(), X64RegisterType.STACK);
            active.remove(spill);
            active.add(i);
            active.sort(LiveInterval::compareEndpoint);
        } else {
            varToReg.put(i.irAssignableValue(), X64RegisterType.STACK);
        }
    }
}
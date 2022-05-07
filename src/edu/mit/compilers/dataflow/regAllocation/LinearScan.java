package edu.mit.compilers.dataflow.regAllocation;

import java.util.ArrayList;
import java.util.HashMap;

public class LinearScan {

    private ArrayList<String> availableRegisters;
    private ArrayList<LiveInterval> active = new ArrayList<>();
    private HashMap<String, String> varToReg;

    public LinearScan(ArrayList<String> availableRegisters) {
        this.availableRegisters = availableRegisters;
    }

    public void linearScanRegisterAllocation(ArrayList<LiveInterval> liveIntervals) {
        liveIntervals.sort((l1, l2) -> l1.compareStartpoint(l2));
        for (LiveInterval i: liveIntervals) {
            expireOldIntervals(i);
            if (active.size() == availableRegisters.size()) {
                spillAtInterval(i);
            }
            else {
                varToReg.put(i.varName, availableRegisters.remove(0));
                active.add(i);
                active.sort((l1, l2) -> l1.compareEndpoint(l2));
            }
        }
    }

    public void expireOldIntervals(LiveInterval i) {
        active.sort((l1, l2) -> l1.compareEndpoint(l2));
        for (LiveInterval j: active) {
            if (j.endpoint >= i.startpoint) {
                return;
            }
            active.remove(j);
            availableRegisters.add(varToReg.get(j.varName));
        }
    }

    public void spillAtInterval(LiveInterval i) {
        LiveInterval spill = active.get(active.size()-1);
        if (spill.endpoint > i.endpoint) {
            varToReg.put(i.varName, varToReg.get(spill));
            varToReg.put(spill.varName, "stack");
            active.remove(spill);
            active.add(i);
            active.sort((l1, l2) -> l1.compareEndpoint(l2));
        }
        else {
            varToReg.put(i.varName, "stack");
        }
    }

}

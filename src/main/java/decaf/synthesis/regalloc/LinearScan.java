package decaf.synthesis.regalloc;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import decaf.ir.codes.Method;
import decaf.ir.names.IrValue;
import decaf.synthesis.asm.X86Register;

public class LinearScan {

  private final Map<Method, Map<IrValue, X86Register>> varToRegMap = new HashMap<>();
  private final Map<Method, List<LiveInterval>> liveIntervals;
  private List<X86Register> availableRegisters = new ArrayList<>();
  private List<LiveInterval> active = new ArrayList<>();

  public LinearScan(
      Collection<X86Register> availableRegisters,
      Map<Method, List<LiveInterval>> liveIntervals
  ) {
    this.availableRegisters.addAll(availableRegisters);
    this.liveIntervals = liveIntervals;
  }

  public Map<Method, Map<IrValue, X86Register>> getVariableToRegisterMapping() {
    return varToRegMap;
  }


  private List<LiveInterval> prepare(Map.Entry<Method, List<LiveInterval>> entry) {
    varToRegMap.put(
        entry.getKey(),
        new HashMap<>()
    );
    availableRegisters = new ArrayList<>(X86Register.regsToAllocate);
    var liveIntervalsList = entry.getValue();
    liveIntervalsList.sort(LiveInterval::compareStartPoint);
    active = new ArrayList<>();
    return liveIntervalsList;
  }

  public void allocate() {
    for (var entry : liveIntervals.entrySet()) {
      var liveIntervalsList = prepare(entry);
      var nAvailableRegisters = availableRegisters.size();
      var varToReg = varToRegMap.get(entry.getKey());
      for (LiveInterval i : liveIntervalsList) {
        expireOldIntervals(
            i,
            varToReg
        );
        if (active.size() == nAvailableRegisters) {
          spillAtInterval(
              i,
              varToReg
          );
        } else {
          varToReg.put(
              i.irSsaRegister(),
              availableRegisters.remove(0)
          );
          active.add(i);
          active.sort(LiveInterval::compareEndpoint);
        }
      }
    }
  }

  public void expireOldIntervals(
      LiveInterval i,
      Map<IrValue, X86Register> varToReg
  ) {
    active.sort(LiveInterval::compareEndpoint);
    for (LiveInterval j : new ArrayList<>(active)) {
      if (j.endPoint() >= i.startPoint()) {
        return;
      }
      active.remove(j);
      availableRegisters.add(varToReg.get(j.irSsaRegister()));
    }
  }

  public void spillAtInterval(
      LiveInterval i,
      Map<IrValue, X86Register> varToReg
  ) {
    LiveInterval spill = active.get(active.size() - 1);
    if (spill.endPoint() > i.endPoint()) {
      varToReg.put(
          i.irSsaRegister(),
          varToReg.get(spill.irSsaRegister())
      );
      varToReg.put(
          spill.irSsaRegister(),
          X86Register.STACK
      );
      active.remove(spill);
      active.add(i);
      active.sort(LiveInterval::compareEndpoint);
    } else {
      varToReg.put(
          i.irSsaRegister(),
          X86Register.STACK
      );
    }
  }
}
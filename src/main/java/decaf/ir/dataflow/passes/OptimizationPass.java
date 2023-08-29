package decaf.ir.dataflow.passes;


import java.util.List;
import java.util.Set;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.names.IrValue;

public abstract class OptimizationPass {
  protected final OptimizationContext optimizationContext;
  protected BasicBlock entryBlock;
  protected Method method;

  public OptimizationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    this.optimizationContext = optimizationContext;
    this.method = method;
    this.entryBlock = method.getEntryBlock();
  }

  // return whether an instruction of the form x = x
  public static boolean isTrivialAssignment(Instruction instruction) {
    if (instruction instanceof CopyInstruction assign) {
      return assign.getDestination()
                   .equals(assign.getValue());
    }
    return false;
  }

  public Method getMethod() {
    return method;
  }

  public Set<IrValue> globals() {
    return optimizationContext.globals();
  }

  public List<BasicBlock> getBasicBlocksList() {
    return optimizationContext.getBasicBlocks(method);
  }

  // return true if changes happened
  public abstract boolean runFunctionPass();
}

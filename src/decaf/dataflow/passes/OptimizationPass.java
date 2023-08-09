package decaf.dataflow.passes;


import java.util.List;
import java.util.Set;

import decaf.cfg.BasicBlock;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrValue;
import decaf.dataflow.OptimizationContext;

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

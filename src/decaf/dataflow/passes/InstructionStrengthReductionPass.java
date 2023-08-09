package decaf.dataflow.passes;


import decaf.codegen.codes.Method;
import decaf.dataflow.OptimizationContext;

public class InstructionStrengthReductionPass extends OptimizationPass {
  public InstructionStrengthReductionPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  @Override
  public boolean runFunctionPass() {
    return false;
  }
}

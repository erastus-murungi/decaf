package decaf.ir.dataflow.passes;


import decaf.ir.dataflow.OptimizationContext;

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

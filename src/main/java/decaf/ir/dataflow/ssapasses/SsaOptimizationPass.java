package decaf.ir.dataflow.ssapasses;


import decaf.ir.codes.Method;
import decaf.ir.dataflow.OptimizationContext;
import decaf.ir.dataflow.passes.OptimizationPass;

public abstract class SsaOptimizationPass extends OptimizationPass {
  public SsaOptimizationPass(
      OptimizationContext optimizationContext,
      Method method
  ) {
    super(
        optimizationContext,
        method
    );
  }

  protected abstract void resetForPass();
}

package decaf.dataflow.ssapasses;


import decaf.codegen.codes.Method;
import decaf.dataflow.OptimizationContext;
import decaf.dataflow.passes.OptimizationPass;

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

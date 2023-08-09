package decaf.dataflow;

public enum OptimizationPassType {
  CommonSubExpression,
  CopyPropagation,
  DeadStoreElimination,
  PeepHoleOptimization,
  ConstantPropagation,
  InstructionSimplification,
  BranchSimplification,
  CommonSubExpressionSsa,
  CopyPropagationSsa,
  DeadStoreEliminationSsa,
  SccpSsa,
  RedundantPhiEliminationPass,
  LoopAnalysisPass,
  FunctionInlinePass
}
package decaf.ir.dataflow;


import java.util.ArrayList;
import java.util.List;

import decaf.ir.codes.Method;
import decaf.ir.dataflow.passes.BranchSimplificationPass;
import decaf.ir.dataflow.passes.CommonSubExpressionEliminationPass;
import decaf.ir.dataflow.passes.ConstantPropagationPass;
import decaf.ir.dataflow.passes.CopyPropagationPass;
import decaf.ir.dataflow.passes.DeadStoreEliminationPass;
import decaf.ir.dataflow.passes.FunctionInlinePass;
import decaf.ir.dataflow.passes.InstructionSimplifyPass;
import decaf.ir.dataflow.passes.OptimizationPass;
import decaf.ir.dataflow.passes.PeepHoleOptimizationPass;
import decaf.ir.dataflow.ssapasses.CommonSubExpressionEliminationSsaPass;
import decaf.ir.dataflow.ssapasses.CopyPropagationSsaPass;
import decaf.ir.dataflow.ssapasses.DeadStoreEliminationSsaPass;
import decaf.ir.dataflow.ssapasses.LoopAnalysisPass;
import decaf.ir.dataflow.ssapasses.RedundantPhiEliminationPass;
import decaf.ir.dataflow.ssapasses.SccpSsaPass;
import decaf.shared.CompilationContext;
import decaf.shared.ProgramIr;
import decaf.shared.Utils;

public class DataflowOptimizer {
  private static final int MAX_RUNS = 20;
  private final List<OptimizationPass> optimizationPassesList = new ArrayList<>();
  private final OptimizationContext optimizationContext;
  private final CompilationContext compilationContext;

  public DataflowOptimizer(ProgramIr programIr, CompilationContext compilationContext) {
    this.optimizationContext = new OptimizationContext(programIr);
    this.compilationContext = compilationContext;
  }

  public List<Method> getOptimizedMethods() {
    return optimizationContext.getMethodsToOptimize();
  }

  public void initialize() {
    addPass(OptimizationPassType.PeepHoleOptimization);
    addPass(OptimizationPassType.SccpSsa);
    addPass(OptimizationPassType.CopyPropagationSsa);
    addPass(OptimizationPassType.RedundantPhiEliminationPass);
    addPass(OptimizationPassType.DeadStoreEliminationSsa);
    addPass(OptimizationPassType.LoopAnalysisPass);
    addPass(OptimizationPassType.PeepHoleOptimization);
  }

  public void optimize() {
    for (int run = 0; run < MAX_RUNS; run++) {
      boolean changesHappened = false;
      for (var optimizationPass : optimizationPassesList) {
        var changesHappenedForOpt = optimizationPass.runFunctionPass();
        changesHappened = changesHappened | changesHappenedForOpt;
        if (compilationContext.debugModeOn()) {
          System.out.format(
              "%s<%s> run = %s :: ",
              optimizationPass.getClass()
                              .getSimpleName(),
              optimizationPass.getMethod()
                              .methodName(),
              run
          );
          System.out.println(Utils.coloredPrint(
              String.valueOf(changesHappenedForOpt),
              Utils.ANSIColorConstants.ANSI_GREEN_BOLD
          ));
          System.out.println(ProgramIr.mergeMethod(optimizationPass.getMethod()));
        }
      }
      if (!changesHappened) {
        break;
      }
    }
  }

  public void addPass(OptimizationPassType optimizationPassType) {
    final var toOptimizeMethods = optimizationContext.getMethodsToOptimize();
    switch (optimizationPassType) {
      case CopyPropagation -> toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CopyPropagationPass(
          optimizationContext,
          method
      )));
      case CommonSubExpression ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CommonSubExpressionEliminationPass(
              optimizationContext,
              method
          )));
      case DeadStoreElimination ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new DeadStoreEliminationPass(
              optimizationContext,
              method
          )));
      case PeepHoleOptimization ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new PeepHoleOptimizationPass(
              optimizationContext,
              method
          )));
      case ConstantPropagation ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new ConstantPropagationPass(
              optimizationContext,
              method
          )));
      case InstructionSimplification ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new InstructionSimplifyPass(
              optimizationContext,
              method
          )));
      case BranchSimplification ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new BranchSimplificationPass(
              optimizationContext,
              method
          )));
      case CommonSubExpressionSsa ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CommonSubExpressionEliminationSsaPass(
              optimizationContext,
              method
          )));
      case CopyPropagationSsa ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CopyPropagationSsaPass(
              optimizationContext,
              method
          )));
      case DeadStoreEliminationSsa ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new DeadStoreEliminationSsaPass(
              optimizationContext,
              method
          )));
      case SccpSsa -> toOptimizeMethods.forEach(method -> optimizationPassesList.add(new SccpSsaPass(
          optimizationContext,
          method
      )));
      case RedundantPhiEliminationPass ->
          toOptimizeMethods.forEach(method -> optimizationPassesList.add(new RedundantPhiEliminationPass(
              optimizationContext,
              method
          )));
      case LoopAnalysisPass -> toOptimizeMethods.forEach(method -> optimizationPassesList.add(new LoopAnalysisPass(
          optimizationContext,
          method
      )));
      case FunctionInlinePass -> toOptimizeMethods.forEach(method -> optimizationPassesList.add(new FunctionInlinePass(
          optimizationContext,
          method
      )));
      default -> throw new IllegalArgumentException();
    }
  }
}

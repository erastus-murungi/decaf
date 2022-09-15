package decaf.dataflow;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.codes.Method;
import decaf.dataflow.passes.BranchSimplificationPass;
import decaf.dataflow.passes.CommonSubExpressionEliminationPass;
import decaf.dataflow.passes.ConstantPropagationPass;
import decaf.dataflow.passes.CopyPropagationPass;
import decaf.dataflow.passes.DeadStoreEliminationPass;
import decaf.dataflow.passes.FunctionInlinePass;
import decaf.dataflow.passes.InstructionSimplifyPass;
import decaf.dataflow.passes.OptimizationPass;
import decaf.dataflow.passes.PeepHoleOptimizationPass;
import decaf.dataflow.ssapasses.CommonSubExpressionEliminationSsaPass;
import decaf.dataflow.ssapasses.CopyPropagationSsaPass;
import decaf.dataflow.ssapasses.DeadStoreEliminationSsaPass;
import decaf.dataflow.ssapasses.LoopAnalysisPass;
import decaf.dataflow.ssapasses.RedundantPhiEliminationPass;
import decaf.dataflow.ssapasses.SccpSsaPass;
import decaf.common.CompilationContext;
import decaf.common.ProgramIr;
import decaf.common.Utils;

public class DataflowOptimizer {
    private static final int MAX_RUNS = 20;
    private final List<OptimizationPass> optimizationPassesList = new ArrayList<>();
    private final OptimizationContext optimizationContext;

    public DataflowOptimizer(ProgramIr programIr) {
        this.optimizationContext = new OptimizationContext(programIr);
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

    private void runInterProceduralPasses() {
        var functionInlinePass = new FunctionInlinePass(optimizationContext.getMethodsToOptimize());
        optimizationContext.setMethodsToOptimize(functionInlinePass.run());
    }

    public void optimize() {
        for (int run = 0; run < MAX_RUNS; run++) {
            boolean changesHappened = false;
            for (var optimizationPass : optimizationPassesList) {
                var changesHappenedForOpt = optimizationPass.runFunctionPass();
                changesHappened = changesHappened | changesHappenedForOpt;
                if (CompilationContext.isDebugModeOn()) {
                    System.out.format("%s<%s> run = %s :: ", optimizationPass.getClass().getSimpleName(), optimizationPass.getMethod().methodName(), run);
                    System.out.println(Utils.coloredPrint(String.valueOf(changesHappenedForOpt), Utils.ANSIColorConstants.ANSI_GREEN_BOLD));
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
            case CopyPropagation ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CopyPropagationPass(optimizationContext, method)));
            case CommonSubExpression ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CommonSubExpressionEliminationPass(optimizationContext, method)));
            case DeadStoreElimination ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new DeadStoreEliminationPass(optimizationContext, method)));
            case PeepHoleOptimization ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new PeepHoleOptimizationPass(optimizationContext, method)));
            case ConstantPropagation ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new ConstantPropagationPass(optimizationContext, method)));
            case InstructionSimplification ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new InstructionSimplifyPass(optimizationContext, method)));
            case BranchSimplification ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new BranchSimplificationPass(optimizationContext, method)));
            case CommonSubExpressionSsa ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CommonSubExpressionEliminationSsaPass(optimizationContext, method)));
            case CopyPropagationSsa ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CopyPropagationSsaPass(optimizationContext, method)));
            case DeadStoreEliminationSsa ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new DeadStoreEliminationSsaPass(optimizationContext, method)));
            case SccpSsa ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new SccpSsaPass(optimizationContext, method)));
            case RedundantPhiEliminationPass ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new RedundantPhiEliminationPass(optimizationContext, method)));
            case LoopAnalysisPass ->
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new LoopAnalysisPass(optimizationContext, method)));
            default -> throw new IllegalArgumentException();
        }
    }
}

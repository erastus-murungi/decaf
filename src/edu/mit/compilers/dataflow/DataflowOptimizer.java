package edu.mit.compilers.dataflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.dataflow.passes.BranchSimplificationPass;
import edu.mit.compilers.dataflow.passes.CommonSubExpressionEliminationPass;
import edu.mit.compilers.dataflow.passes.ConstantPropagationPass;
import edu.mit.compilers.dataflow.passes.CopyPropagationPass;
import edu.mit.compilers.dataflow.passes.DeadStoreEliminationPass;
import edu.mit.compilers.dataflow.passes.FunctionInlinePass;
import edu.mit.compilers.dataflow.passes.InstructionSimplifyPass;
import edu.mit.compilers.dataflow.passes.OptimizationPass;
import edu.mit.compilers.dataflow.passes.PeepHoleOptimizationPass;
import edu.mit.compilers.dataflow.ssapasses.CommonSubExpressionEliminationSsaPass;
import edu.mit.compilers.dataflow.ssapasses.CopyPropagationSsaPass;
import edu.mit.compilers.dataflow.ssapasses.DeadStoreEliminationSsaPass;
import edu.mit.compilers.dataflow.ssapasses.LoopAnalysisPass;
import edu.mit.compilers.dataflow.ssapasses.RedundantPhiEliminationPass;
import edu.mit.compilers.dataflow.ssapasses.SccpSsaPass;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.GraphVizPrinter;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

public class DataflowOptimizer {
    private static final int MAX_RUNS = 20;
    private final List<OptimizationPass> optimizationPassesList = new ArrayList<>();
    private final OptimizationContext optimizationContext;

    public DataflowOptimizer(ProgramIr programIr, boolean[] cliOpts) {
        this.optimizationContext = new OptimizationContext(programIr);
    }

    public List<Method> getOptimizedMethods() {
        return optimizationContext.getMethodsToOptimize();
    }

    public void initialize() {
//        runInterProceduralPasses();
//        addPass(OptimizationPassType.CopyPropagation);
//        addPass(OptimizationPassType.CommonSubExpression);
//        addPass(OptimizationPassType.DeadCodeElimination);
//        addPass(OptimizationPassType.DeadStoreElimination);
//        addPass(OptimizationPassType.ConstantPropagation);
//        addPass(OptimizationPassType.InstructionSimplification);
        // addPass(OptimizationPassType.BranchSimplification);
//        addPass(OptimizationPassType.CommonSubExpressionSsa);
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
                if (CLI.debug) {
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

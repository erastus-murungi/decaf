package edu.mit.compilers.dataflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
import edu.mit.compilers.dataflow.ssapasses.SccpSsaPass;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.Utils;

public class DataflowOptimizer {
    private static final int MAX_RUNS = 20;

    Integer numberOfRuns = MAX_RUNS;
    Set<LValue> globalNames;
    private final List<Method> allMethods;
    private List<Method> toOptimizeMethods;
    List<OptimizationPass> optimizationPassList = new ArrayList<>();

    Factory optimizationPassesFactory = new Factory();

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
        SccpSsa
        }

    public class Factory {
        public List<OptimizationPass> getPass(OptimizationPassType optimizationPassType) {
            final var optimizationPassesList = new ArrayList<OptimizationPass>();
            switch (optimizationPassType) {
                case CopyPropagation: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new CopyPropagationPass(globalNames, method)));
                    break;
                }
                case CommonSubExpression: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new CommonSubExpressionEliminationPass(globalNames, method)));
                    break;
                }
                case DeadStoreElimination: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new DeadStoreEliminationPass(globalNames, method)));
                    break;
                }
                case PeepHoleOptimization: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new PeepHoleOptimizationPass(globalNames, method)));
                    break;
                }
                case ConstantPropagation: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new ConstantPropagationPass(globalNames, method)));
                    break;
                }
                case InstructionSimplification: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new InstructionSimplifyPass(globalNames, method)));
                    break;
                }
                case BranchSimplification: {
                    toOptimizeMethods.forEach(method ->
                            optimizationPassesList.add(new BranchSimplificationPass(globalNames, method)));
                    break;
                }
                case CommonSubExpressionSsa: {
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CommonSubExpressionEliminationSsaPass(globalNames, method)));
                    break;
                }
                case CopyPropagationSsa: {
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new CopyPropagationSsaPass(globalNames, method)));
                    break;
                }
                case DeadStoreEliminationSsa: {
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new DeadStoreEliminationSsaPass(globalNames, method)));
                    break;
                }
                case SccpSsa: {
                    toOptimizeMethods.forEach(method -> optimizationPassesList.add(new SccpSsaPass(globalNames, method)));
                    break;
                }
                default: {
                    throw new IllegalArgumentException();
                }
            }
            return optimizationPassesList;
        }
    }

    public void addPass(OptimizationPassType optimizationPassType) {
        optimizationPassList.addAll(optimizationPassesFactory.getPass(optimizationPassType));

    }

    public void initialize() {
//        runInterProceduralPasses();
//        addPass(OptimizationPassType.CopyPropagation);
//        addPass(OptimizationPassType.CommonSubExpression);
//        addPass(OptimizationPassType.DeadCodeElimination);
//        addPass(OptimizationPassType.PeepHoleOptimization);
//        addPass(OptimizationPassType.DeadStoreElimination);
//        addPass(OptimizationPassType.ConstantPropagation);
//        addPass(OptimizationPassType.InstructionSimplification);
        // addPass(OptimizationPassType.BranchSimplification);
//        addPass(OptimizationPassType.CommonSubExpressionSsa);
//        addPass(OptimizationPassType.CopyPropagationSsa);
//        addPass(OptimizationPassType.DeadStoreEliminationSsa);
//        addPass(OptimizationPassType.SccpSsa);
    }

    public List<Method> getOptimizedMethods() {
        if (toOptimizeMethods.isEmpty()) {
            return allMethods;
        } else {
            return toOptimizeMethods;
        }
    }

    public DataflowOptimizer(List<Method> allMethods, Set<LValue> globalNames, boolean[] cliOpts) {
        this.globalNames = globalNames;
        this.allMethods = allMethods;
        // ignore all methods with a runtime
        if (allMethods.stream()
                .anyMatch(Method::hasRuntimeException))
            this.toOptimizeMethods = Collections.emptyList();
        else
            this.toOptimizeMethods = allMethods;
    }

    private void runInterProceduralPasses() {
        FunctionInlinePass functionInlinePass = new FunctionInlinePass(toOptimizeMethods);
        toOptimizeMethods = functionInlinePass.run();
    }

    public void optimize() {
        for (int run = 0; run < numberOfRuns; run++) {
            boolean changesHappened = false;
            for (var optimizationPass : optimizationPassList) {
                if (!toOptimizeMethods.contains(optimizationPass.getMethod()))
                    continue;
                var changesHappenedForOpt = optimizationPass.runFunctionPass();
                changesHappened = changesHappened | changesHappenedForOpt;
                if (CLI.debug) {
                    System.out.format("%s<%s> run = %s\n", optimizationPass.getClass()
                            .getSimpleName(), optimizationPass.getMethod()
                            .methodName(), run);
                    System.out.println(TraceScheduler.flattenIr(optimizationPass.getMethod()));
                    System.out.println(Utils.coloredPrint(String.valueOf(changesHappenedForOpt), Utils.ANSIColorConstants.ANSI_GREEN_BOLD));
                }
                if (run % toOptimizeMethods.size() == 0)
                    runInterProceduralPasses();
            }
            if (!changesHappened) {
                break;
            }
        }
    }
}

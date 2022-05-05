package edu.mit.compilers.dataflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.passes.BranchSimplificationPass;
import edu.mit.compilers.dataflow.passes.CommonSubExpressionEliminationPass;
import edu.mit.compilers.dataflow.passes.ConstantPropagationPass;
import edu.mit.compilers.dataflow.passes.CopyPropagationPass;
import edu.mit.compilers.dataflow.passes.DeadCodeEliminationPass;
import edu.mit.compilers.dataflow.passes.DeadStoreEliminationPass;
import edu.mit.compilers.dataflow.passes.FunctionInlinePass;
import edu.mit.compilers.dataflow.passes.InstructionSimplifyPass;
import edu.mit.compilers.dataflow.passes.OptimizationPass;
import edu.mit.compilers.dataflow.passes.PeepHoleOptimizationPass;
import edu.mit.compilers.utils.Utils;

public class DataflowOptimizer {
    private static final int MAX_RUNS = 20;

    Integer numberOfRuns = MAX_RUNS;
    Set<AbstractName> globalNames;
    public List<MethodBegin> methodBeginTacLists;
    List<OptimizationPass> optimizationPassList = new ArrayList<>();

    Factory optimizationPassesFactory = new Factory();

    public enum OptimizationPassType {
        CommonSubExpression,
        CopyPropagation,
        DeadCodeElimination,
        DeadStoreElimination,
        PeepHoleOptimization,
        ConstantPropagation,
        InstructionSimplification,
        BranchSimplification
    }

    public class Factory {
        public List<OptimizationPass> getPass(OptimizationPassType optimizationPassType) {
            final var optimizationPassesList = new ArrayList<OptimizationPass>();
            switch (optimizationPassType) {
                case CopyPropagation: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new CopyPropagationPass(globalNames, methodBegin)));
                    break;
                }
                case DeadCodeElimination: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new DeadCodeEliminationPass(globalNames, methodBegin)));
                    break;
                }
                case CommonSubExpression: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new CommonSubExpressionEliminationPass(globalNames, methodBegin)));
                    break;
                }
                case DeadStoreElimination: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new DeadStoreEliminationPass(globalNames, methodBegin)));
                    break;
                }
                case PeepHoleOptimization: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new PeepHoleOptimizationPass(globalNames, methodBegin)));
                    break;
                }
                case ConstantPropagation: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new ConstantPropagationPass(globalNames, methodBegin)));
                    break;
                }
                case InstructionSimplification: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new InstructionSimplifyPass(globalNames, methodBegin)));
                    break;
                }
                case BranchSimplification: {
                    methodBeginTacLists.forEach(methodBegin ->
                            optimizationPassesList.add(new BranchSimplificationPass(globalNames, methodBegin)));
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
        runInterProceduralPasses();
        addPass(OptimizationPassType.PeepHoleOptimization);
        addPass(OptimizationPassType.CommonSubExpression);
        addPass(OptimizationPassType.CopyPropagation);
        addPass(OptimizationPassType.DeadCodeElimination);
        addPass(OptimizationPassType.DeadStoreElimination);
        addPass(OptimizationPassType.ConstantPropagation);
        addPass(OptimizationPassType.InstructionSimplification);
        addPass(OptimizationPassType.BranchSimplification);
    }

    public DataflowOptimizer(List<MethodBegin> methodBeginTacLists, Set<AbstractName> globalNames) {
        this.globalNames = globalNames;
        this.methodBeginTacLists = methodBeginTacLists;
    }

    private void runInterProceduralPasses() {
        FunctionInlinePass functionInlinePass = new FunctionInlinePass(methodBeginTacLists);
        methodBeginTacLists = functionInlinePass.run();
    }
    public void optimize() {
        for (int run = 0; run < numberOfRuns; run++) {
            boolean changesHappened = false;
            for (var optimizationPass : optimizationPassList) {
                if (!methodBeginTacLists.contains(optimizationPass.getMethod()))
                    continue;
                var changesHappenedForOpt = optimizationPass.run();
                changesHappened = changesHappened | !changesHappenedForOpt;
//                System.out.format("%s<%s> run = %s", optimizationPass.getClass().getSimpleName(), optimizationPass.getMethod().methodName(), run);
//                System.out.println(optimizationPass.getMethod().entryBlock.instructionList.flatten());
//                System.out.println(Utils.coloredPrint(String.valueOf(changesHappened), Utils.ANSIColorConstants.ANSI_RED));
                if (run % methodBeginTacLists.size() == 0)
                    runInterProceduralPasses();
            }
            if (!changesHappened) {
                break;
            }
        }
    }
}
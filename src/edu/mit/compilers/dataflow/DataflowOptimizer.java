package edu.mit.compilers.dataflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.Program;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.passes.CommonSubExpressionEliminationPass;
import edu.mit.compilers.dataflow.passes.CopyPropagationPass;
import edu.mit.compilers.dataflow.passes.DeadCodeEliminationPass;
import edu.mit.compilers.dataflow.passes.DeadStoreEliminationPass;
import edu.mit.compilers.dataflow.passes.OptimizationPass;
import edu.mit.compilers.dataflow.passes.PeepHoleOptimizationPass;

public class DataflowOptimizer {
    private static final int MAX_RUNS = 20;

    Integer numberOfRuns = MAX_RUNS;
    Set<AbstractName> globalNames;
    List<MethodBegin> methodBeginTacLists;
    List<OptimizationPass> optimizationPassList = new ArrayList<>();

    Factory optimizationPassesFactory = new Factory();

    public enum OptimizationPassType {
        CommonSubExpression,
        CopyPropagation,
        DeadCodeElimination,
        DeadStoreElimination,
        PeepHoleOptimization
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
        addPass(OptimizationPassType.PeepHoleOptimization);
        addPass(OptimizationPassType.CommonSubExpression);
        addPass(OptimizationPassType.CopyPropagation);
        addPass(OptimizationPassType.DeadCodeElimination);
        addPass(OptimizationPassType.DeadStoreElimination);
    }

    public DataflowOptimizer(List<MethodBegin> methodBeginTacLists, Set<AbstractName> globalNames) {
        this.globalNames = globalNames;
        this.methodBeginTacLists = methodBeginTacLists;
    }

    public void optimize() {
        for (int run = 0; run < numberOfRuns; run++) {
            boolean changesHappened = false;
            for (var optimizationPass : optimizationPassList) {
                changesHappened = changesHappened | !optimizationPass.run();
            }
            if (!changesHappened)
                break;
        }
    }
}

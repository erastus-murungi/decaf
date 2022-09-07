package edu.mit.compilers.dataflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.TarjanSCC;

public class OptimizationContext {
    private final Map<Method, List<BasicBlock>> methodToBlocks = new HashMap<>();

    private final ProgramIr programIr;

    private List<Method> methodsToOptimizeMethods = new ArrayList<>();

    public OptimizationContext(ProgramIr programIr) {
        this.programIr = programIr;
        for (Method method : programIr.methodList)
            methodToBlocks.put(method, List.copyOf(TarjanSCC.getReversePostOrder(method.entryBlock)));
        setMethodsToOptimize(programIr.methodList);
    }

    public Set<LValue> globals() {
        return programIr.getGlobals();
    }

    public void setBasicBlocks(Method method, Collection<BasicBlock> basicBlocks) {
        methodToBlocks.put(method, List.copyOf(basicBlocks));
    }

    public List<BasicBlock> getBasicBlocks(Method method) {
        return methodToBlocks.get(method);
    }

    public List<Method> getMethodsToOptimize() {
        return methodsToOptimizeMethods;
    }

    public void setMethodsToOptimize(Collection<Method> methodsToOptimizeMethods) {
        this.methodsToOptimizeMethods = List.copyOf(methodsToOptimizeMethods);
    }
}

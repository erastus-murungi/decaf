package decaf.dataflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import decaf.codegen.names.IrValue;
import decaf.common.ProgramIr;
import decaf.common.StronglyConnectedComponentsTarjan;
import decaf.cfg.BasicBlock;
import decaf.codegen.codes.Method;

public class OptimizationContext {
    private final Map<Method, List<BasicBlock>> methodToBlocks = new HashMap<>();

    private final ProgramIr programIr;

    private List<Method> methodsToOptimizeMethods = new ArrayList<>();

    public OptimizationContext(ProgramIr programIr) {
        this.programIr = programIr;
        for (Method method : programIr.getMethods())
            methodToBlocks.put(method, List.copyOf(StronglyConnectedComponentsTarjan.getReversePostOrder(method.getEntryBlock())));
        setMethodsToOptimize(programIr.getMethods());
    }

    public Set<IrValue> globals() {
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

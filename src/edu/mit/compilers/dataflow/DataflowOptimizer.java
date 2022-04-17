package edu.mit.compilers.dataflow;

import java.util.Map;
import java.util.Set;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.names.AbstractName;

public class DataflowOptimizer {
    Set<AbstractName> globalNames;

    public DataflowOptimizer(Set<AbstractName> globalNames) {
        this.globalNames = globalNames;
    }

    private static final int MAX_RUNS = 20;

    public void optimize(Map<String, BasicBlock> methodToEntryBlock) {
        methodToEntryBlock.forEach((methodName, entryBlock) -> optimize(entryBlock));
    }

    public void optimize(BasicBlock entryBlock) {
        for (int run = 0; run < MAX_RUNS; run++) {
            GlobalCSE.performGlobalCSE(entryBlock, globalNames);
            System.out.println("Common SubExpression done");
            System.out.println(entryBlock.threeAddressCodeList);
            CopyPropagation.performGlobalCopyPropagation(entryBlock, globalNames);
            System.out.println("Copy Propagation done");
            System.out.println(entryBlock.threeAddressCodeList);
            GlobalDCE.performGlobalDeadCodeElimination(entryBlock);
            System.out.println("Dead Code Elimination done");
            System.out.println(entryBlock.threeAddressCodeList);
        }
    }
}

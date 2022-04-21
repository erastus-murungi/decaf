package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.*;

public class MethodBegin extends ThreeAddressCode {
    public final MethodDefinition methodDefinition;
    /**
     * @implNote instead of storing the set of locals, we now store a method's tac list.
     * Because of optimizations, the set of locals could be re-computed;
     *
     * This is the unoptimized threeAddressCodeList of a method
     */
    public ThreeAddressCodeList unoptimized;

    /**
     * We use this for optimization
     */
    public BasicBlock entryBlock;

    // to be filled in later by the X64Converter
    public HashMap<String, Integer> nameToStackOffset = new HashMap<>();

    public MethodBegin(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
    }

    @Override
    public String toString() {
        return String.format("%s:\n%s%s", methodDefinition.methodName.id, DOUBLE_INDENT, "enter method");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }
}

package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class MethodBegin extends Instruction {
    public final MethodDefinition methodDefinition;
    /**
     * @implNote instead of storing the set of locals, we now store a method's tac list.
     * Because of optimizations, the set of locals could be re-computed;
     * <p>
     * This is the unoptimized threeAddressCodeList of a method
     */
    public InstructionList unoptimized;

    /**
     * We use this for optimization
     */
    public BasicBlock entryBlock;

    public boolean isMain() {
        return methodDefinition.methodName.id.equals("main");
    }

    public String methodName() {
        return methodDefinition.methodName.id;
    }

    public Set<AssignableName> getParameterNames() {
        return methodDefinition.methodDefinitionParameterList.stream()
                .map(methodDefinitionParameter -> new VariableName(methodDefinitionParameter.id.id, Utils.WORD_SIZE, methodDefinitionParameter.builtinType))
                .collect(Collectors.toUnmodifiableSet());
    }

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
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return String.format("\n%s: -> %s {%s", methodDefinition.methodName.id, methodDefinition.returnType.getSourceCode(), DOUBLE_INDENT);
    }

    @Override
    public Instruction copy() {
        return new MethodBegin(methodDefinition);
    }
}

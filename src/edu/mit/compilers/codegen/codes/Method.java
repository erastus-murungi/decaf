package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class Method extends Instruction {
    public final MethodDefinition methodDefinition;
    /**
     * @implNote instead of storing the set of locals, we now store a method's tac list.
     * Because of optimizations, the set of locals could be re-computed;
     * <p>
     * This is the unoptimized threeAddressCodeList of a method
     */
    public InstructionList unoptimizedInstructionList;

    private final List<LValue> parameterNames;

    public List<LValue> getParameterNames() {
        return parameterNames;
    }

    /**
     * We use this for optimization
     */
    public BasicBlock entryBlock;

    public BasicBlock exitBlock;

    private boolean hasRuntimeException;

    public boolean hasRuntimeException() {
        return hasRuntimeException;
    }

    public void setHasRuntimeException(boolean hasRuntimeException) {
        this.hasRuntimeException = hasRuntimeException;
    }

    public boolean isMain() {
        return methodDefinition.methodName.getLabel()
                                          .equals("main");
    }

    public String methodName() {
        return methodDefinition.methodName.getLabel();
    }

    // to be filled in later by the X64Converter
    public HashMap<String, Integer> nameToStackOffset = new HashMap<>();

    public Method(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
        parameterNames = methodDefinition.parameterList.stream()
                                                       .map(methodDefinitionParameter -> new Variable(methodDefinitionParameter.getName() + "_arg", methodDefinitionParameter.getType()))
                                                       .collect(Collectors.toList());
    }


    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }


    @Override
    public Instruction copy() {
        return new Method(methodDefinition);
    }

    @Override
    public String toString() {
        return String.format("%s @%s(%s) -> %s {%s", "define",
                methodDefinition.methodName.getLabel(),
                parameterNames.stream()
                              .map(parameterName -> parameterName.getType()
                                                                 .getSourceCode() + " " + parameterName.repr())
                              .collect(Collectors.joining(", ")),
                methodDefinition.returnType.getSourceCode(), DOUBLE_INDENT);
    }

    @Override
    public String syntaxHighlightedToString() {
        var defineString = Utils.coloredPrint("define", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s @%s(%s) -> %s {%s", defineString,
                methodDefinition.methodName.getLabel(),
                parameterNames.stream()
                              .map(parameterName -> parameterName.getType()
                                                                 .getColoredSourceCode() + " " + parameterName.repr())
                              .collect(Collectors.joining(", ")),
                methodDefinition.returnType.getSourceCode(), DOUBLE_INDENT);
    }
}

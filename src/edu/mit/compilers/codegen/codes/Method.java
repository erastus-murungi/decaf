package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.utils.Utils;

public class Method extends Instruction {
    public final MethodDefinition methodDefinition;
    private final List<IrRegister> parameterNames;
    /**
     * @implNote instead of storing the set of locals, we now store a method's tac list.
     * Because of optimizations, the set of locals could be re-computed;
     * <p>
     * This is the unoptimized threeAddressCodeList of a method
     */
    public InstructionList unoptimizedInstructionList;
    /**
     * We use this for optimization
     */
    public BasicBlock entryBlock;
    public BasicBlock exitBlock;
    private boolean hasRuntimeException;

    public Method(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
        parameterNames = methodDefinition.parameterList.stream()
                .map(methodDefinitionParameter -> new IrRegister(methodDefinitionParameter.getName(), methodDefinitionParameter.getType()))
                .collect(Collectors.toList());
    }

    public List<IrRegister> getParameterNames() {
        return List.copyOf(parameterNames);
    }

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

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
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
                                .getSourceCode() + " " + parameterName)
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
                                .getColoredSourceCode() + " " + parameterName)
                        .collect(Collectors.joining(", ")),
                methodDefinition.returnType.getSourceCode(), DOUBLE_INDENT);
    }
}

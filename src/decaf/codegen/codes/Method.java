package decaf.codegen.codes;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import decaf.asm.AsmWriter;
import decaf.ast.MethodDefinition;
import decaf.codegen.InstructionList;
import decaf.codegen.names.IrRegister;
import decaf.common.Utils;
import decaf.cfg.BasicBlock;
import decaf.codegen.names.IrValue;

public class Method extends Instruction {
  private final MethodDefinition methodDefinition;
  private final List<IrRegister> parameterNames;
  private InstructionList unoptimizedInstructionList;
  private BasicBlock entryBlock;
  private BasicBlock exitBlock;
  private boolean hasRuntimeException;

  public Method(MethodDefinition methodDefinition) {
    super(methodDefinition);
    this.methodDefinition = methodDefinition;
    parameterNames = methodDefinition.parameterList.stream()
                                                   .map(methodDefinitionParameter -> new IrRegister(
                                                       methodDefinitionParameter.getName(),
                                                       methodDefinitionParameter.getType()
                                                   ))
                                                   .collect(Collectors.toList());
  }

  public List<IrRegister> getParameterNames() {
    return List.copyOf(parameterNames);
  }

  public boolean hasRuntimeException() {
    return isHasRuntimeException();
  }

  public boolean isMain() {
    return getMethodDefinition().methodName.getLabel()
                                           .equals("main");
  }

  public String methodName() {
    return getMethodDefinition().methodName.getLabel();
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
    return new Method(getMethodDefinition());
  }

  @Override
  public String toString() {
    return String.format("%s @%s(%s) -> %s {%s",
                         "define",
                         getMethodDefinition().methodName.getLabel(),
                         getParameterNames().stream()
                                            .map(parameterName -> parameterName.getType()
                                                                               .getSourceCode() + " " + parameterName)
                                            .collect(Collectors.joining(", ")),
                         getMethodDefinition().returnType.getSourceCode(),
                         DOUBLE_INDENT
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    var defineString = Utils.coloredPrint(
        "define",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format("%s @%s(%s) -> %s {%s",
                         defineString,
                         getMethodDefinition().methodName.getLabel(),
                         getParameterNames().stream()
                                            .map(parameterName -> parameterName.getType()
                                                                               .getColoredSourceCode() + " " +
                                                parameterName)
                                            .collect(Collectors.joining(", ")),
                         getMethodDefinition().returnType.getSourceCode(),
                         DOUBLE_INDENT
    );
  }

  public MethodDefinition getMethodDefinition() {
    return methodDefinition;
  }

  /**
   * @implNote instead of storing the set of locals, we now store a method's tac list.
   * Because of optimizations, the set of locals could be re-computed;
   * <p>
   * This is the unoptimized threeAddressCodeList of a method
   */
  public InstructionList getUnoptimizedInstructionList() {
    return unoptimizedInstructionList;
  }

  public void setUnoptimizedInstructionList(InstructionList unoptimizedInstructionList) {
    this.unoptimizedInstructionList = unoptimizedInstructionList;
  }

  /**
   * We use this for optimization
   */
  public BasicBlock getEntryBlock() {
    return entryBlock;
  }

  public void setEntryBlock(BasicBlock entryBlock) {
    this.entryBlock = entryBlock;
  }

  public BasicBlock getExitBlock() {
    return exitBlock;
  }

  public void setExitBlock(BasicBlock exitBlock) {
    this.exitBlock = exitBlock;
  }

  public boolean isHasRuntimeException() {
    return hasRuntimeException;
  }

  public void setHasRuntimeException(boolean hasRuntimeException) {
    this.hasRuntimeException = hasRuntimeException;
  }
}

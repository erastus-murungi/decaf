package edu.mit.compilers.asm;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.mit.compilers.asm.X86Register.N_ARG_REGISTERS;
import static edu.mit.compilers.utils.Utils.WORD_SIZE;
import static edu.mit.compilers.utils.Utils.roundUp16;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.mit.compilers.asm.instructions.X64BinaryInstruction;
import edu.mit.compilers.asm.instructions.X64Instruction;
import edu.mit.compilers.asm.instructions.X64NoOperandInstruction;
import edu.mit.compilers.asm.instructions.X64UnaryInstruction;
import edu.mit.compilers.asm.instructions.X86MetaData;
import edu.mit.compilers.asm.operands.X64CallOperand;
import edu.mit.compilers.asm.operands.X64JumpTargetOperand;
import edu.mit.compilers.asm.operands.X86ConstantValue;
import edu.mit.compilers.asm.operands.X86MemoryAddressValue;
import edu.mit.compilers.asm.operands.X86RegisterMappedValue;
import edu.mit.compilers.asm.operands.X86StackMappedValue;
import edu.mit.compilers.asm.operands.X86Value;
import edu.mit.compilers.asm.types.X64BinaryInstructionType;
import edu.mit.compilers.asm.types.X64NopInstructionType;
import edu.mit.compilers.asm.types.X64UnaryInstructionType;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.FunctionCall;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringConstantAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalBranch;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.codegen.names.IrStackArray;
import edu.mit.compilers.codegen.names.IrStringConstant;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.registerallocation.RegisterAllocator;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.Operators;
import edu.mit.compilers.utils.ProgramIr;

public class X64AsmWriter implements AsmWriter {
  @NotNull
  private static final X86Register COPY_TEMP_REGISTER = X86Register.R10;
  @NotNull
  private final RegisterAllocator registerAllocator;
  @NotNull
  private final ProgramIr programIr;
  @NotNull
  private final X86Program x86Program = new X86Program();
  @NotNull
  private final AsmWriterContext asmWriterContext = new AsmWriterContext();
  @NotNull
  private final X86ValueResolver x86ValueResolver;
  @NotNull
  private X86Method x86Method = new X86Method();
  @Nullable
  private Instruction currentInstruction;
  @NotNull
  private Method currentMethod;


  public X64AsmWriter(
      @NotNull ProgramIr programIr,
      @NotNull RegisterAllocator registerAllocator
  ) {
    this.registerAllocator = registerAllocator;
    this.programIr = programIr;
    this.x86ValueResolver = new X86ValueResolver(programIr,
        registerAllocator);
    if (CLI.debug) {
      System.out.println(registerAllocator.getVariableToRegisterMap());
    }
    emit();
    currentMethod = programIr.getMethods()
                             .get(0);
  }

  private X86Value resolveIrValue(
      @NotNull IrValue irValue,
      boolean updateMethodX86InstructionList
  ) {
    return x86ValueResolver.resolveIrValue(irValue,
        updateMethodX86InstructionList);
  }

  private X86Value resolveIrValue(@NotNull IrValue irValue) {
    return x86ValueResolver.resolveIrValue(irValue,
        true);
  }

  private void emit() {
    emitProgramPrologue();
    emitMethods();
    emitProgramEpilogue();
  }

  private void emitProgramPrologue() {
    var prologue = new ArrayList<X86MetaData>();
    prologue.add(new X86MetaData(".data"));
    for (Instruction instruction : programIr.getPrologue()) {
      if (instruction instanceof StringConstantAllocation stringConstantAllocation) {
        prologue.add(new X86MetaData(stringConstantAllocation.getASM()));
      } else if (instruction instanceof GlobalAllocation globalAllocation) {
        prologue.add(new X86MetaData(String.format(".comm %s, %s, %s",
            globalAllocation.getValue(),
            globalAllocation.getSize(),
            64)));
      } else {
        throw new IllegalStateException();
      }
    }
    x86Program.addPrologue(prologue);
  }

  private void emitProgramEpilogue() {
    x86Program.addEpilogue(List.of(new X86MetaData(".subsections_via_symbols")));
  }

  private void emitMethods() {
    for (var method : programIr.getMethods()) {
      x86Program.addMethod(emitMethod(method));
    }
  }

  private X86Method emitMethod(@NotNull Method method) {
    currentMethod = method;
    x86Method = new X86Method();
    x86ValueResolver.prepareForMethod(currentMethod,
        x86Method);
    for (var instructionList : TraceScheduler.getInstructionTrace(method)) {
      if (!instructionList.isEntry() && !instructionList.getLabel()
                                                        .equals("UNSET")) {
        var label = instructionList.getLabel();
        x86Method.add(new X86MetaData(String.format(".%s:",
            label)));
      }
      for (var instruction : instructionList) {
        currentInstruction = instruction;
        instruction.accept(this);
      }
    }
    return x86Method;
  }

  public @NotNull X86Program getX86Program() {
    return x86Program;
  }

  public X86Value resolveNextStackLocation(@NotNull X86Value x86Value) {
    return x86ValueResolver.resolveNextStackLocation(x86Value);
  }

  private void calleeSave() {
    var toSave = X86Register.calleeSaved;
    for (var register : toSave) {
      x86Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(),
          new X64UnaryInstruction(X64UnaryInstructionType.pushq,
              X86RegisterMappedValue.unassigned(register)));
    }
  }

  private void calleeRestore() {
    var toRestore = new ArrayList<>(X86Register.calleeSaved);
    for (var register : toRestore) {
      x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.popq,
          X86RegisterMappedValue.unassigned(register)));
    }
  }

  private void saveMethodArgsToLocations(@NotNull Method method) {
    // next we save all the arguments to their corresponding locations
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames()
                                                        .size(); parameterIndex++) {
      var parameter = method.getParameterNames()
                            .get(parameterIndex);

      var dst = x86ValueResolver.resolveInitialArgumentLocation(parameter);

      if (parameterIndex < N_ARG_REGISTERS) {
        var src = new X86RegisterMappedValue(X86Register.argumentRegisters.get(parameterIndex),
            parameter);
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            src,
            dst));
      } else {
        var src = new X86StackMappedValue(X86Register.RBP,
            (parameterIndex - 5) * WORD_SIZE + 8,
            parameter);
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            src,
            new X86RegisterMappedValue(COPY_TEMP_REGISTER,
                parameter)));
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
            dst));
      }
    }
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames()
                                                        .size(); parameterIndex++) {
      var parameter = checkNotNull(method.getParameterNames()
                                         .get(parameterIndex));
      if (x86ValueResolver.parameterUsedInCurrentMethod(parameter)) {
        checkNotNull(x86ValueResolver.resolveInitialArgumentLocation(parameter));
      }
    }
  }

  private void callerRestore(@Nullable X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(currentMethod,
                                               Collections.emptyMap())
                                           .getOrDefault(currentInstruction,
                                               Collections.emptySet());
    var toRestore = registerMapping.stream()
                                   .filter(X86Register.callerSaved::contains)
                                   .toList();
    for (var x64Register : toRestore) {
      if (x64Register.equals(X86Register.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(returnAddressRegister,
          regOperand)) {
        var location = resolveNextStackLocation(regOperand);
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            location,
            regOperand));
      }
    }
  }

  private void callerSave(@Nullable X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(currentMethod,
                                               Collections.emptyMap())
                                           .getOrDefault(currentInstruction,
                                               Collections.emptySet());
    var toSave = registerMapping.stream()
                                .filter(X86Register.callerSaved::contains)
                                .toList();
    int startIndex = x86Method.size();
    for (var x64Register : toSave) {
      if (x64Register.equals(X86Register.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(returnAddressRegister,
          regOperand)) {
        var location = resolveNextStackLocation(regOperand);
        x86Method.addAtIndex(startIndex,
            new X64BinaryInstruction(X64BinaryInstructionType.movq,
                regOperand,
                location));
      }
    }
  }


  private void emitStackArgumentsInstructions(@NotNull FunctionCall functionCall) {
    var arguments = functionCall.getArguments();
    // we need to create stack space for the start arguments
    if (arguments.size() > N_ARG_REGISTERS) {
      var stackSpaceForArgs = new X86ConstantValue(new IrIntegerConstant((long) roundUp16(
          (arguments.size() - N_ARG_REGISTERS) * WORD_SIZE),
          Type.Int));
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.subq,
          stackSpaceForArgs,
          X86RegisterMappedValue.unassigned(X86Register.RSP)));
    }
    for (int stackArgumentIndex = N_ARG_REGISTERS; stackArgumentIndex < arguments.size(); stackArgumentIndex++) {
      var stackArgument = arguments.get(stackArgumentIndex);
      final var dst = new X86StackMappedValue(X86Register.RSP,
          (stackArgumentIndex - N_ARG_REGISTERS) * WORD_SIZE);
      if (stackArgument instanceof IrStackArray || x86ValueResolver.isStackMappedIrValue(stackArgument) ||
          stackArgument instanceof IrMemoryAddress) {
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            resolveIrValue(arguments.get(stackArgumentIndex)),
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)));
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
            dst));
      } else {
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            resolveIrValue(arguments.get(stackArgumentIndex)),
            dst));
      }
    }
  }

  private boolean anyResolvedValuesUseRegister(
      @NotNull Collection<X86Value> resolvedValues,
      @NotNull X86Register register
  ) {
    return resolvedValues.stream()
                         .anyMatch(x86Value -> x86Value.registersInUse()
                                                       .contains(register));
  }

  private void moveToStack(
      X86Value resolvedArgument,
      X86Value registerCache
  ) {
    if (resolvedArgument instanceof X86MemoryAddressValue) {
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          resolvedArgument,
          X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)));
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
          registerCache));
    } else {
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          resolvedArgument,
          registerCache));
    }
  }

  public void schedule(@NotNull FunctionCall functionCall) {
    emitStackArgumentsInstructions(functionCall);

    var registerArguments = new ArrayList<>(functionCall.getArguments()
                                                        .subList(0,
                                                            Math.min(N_ARG_REGISTERS,
                                                                functionCall.getNumArguments())));
    var resolvedArguments = new ArrayList<X86Value>();
    var prepInstructions = new ArrayList<List<X64Instruction>>();
    var destinationRegisters = List.copyOf(X86Register.argumentRegisters.subList(0,
        registerArguments.size()));
    for (var registerArgument : registerArguments) {
      var resolvedX86Value = resolveIrValue(registerArgument,
          false);
      resolvedArguments.add(resolvedX86Value);
      prepInstructions.add(x86ValueResolver.getPreparatoryInstructions());
    }

    var addedPrep = new HashSet<Integer>();
    var temp = new HashMap<Integer, X86StackMappedValue>();
    for (int indexOfArgument = 0; indexOfArgument < resolvedArguments.size(); indexOfArgument++) {
      var resolvedArgument = resolvedArguments.get(indexOfArgument);
      var destinationRegister = destinationRegisters.get(indexOfArgument);

      if (anyResolvedValuesUseRegister(resolvedArguments.subList(indexOfArgument,
              resolvedArguments.size()),
          destinationRegister)) {
        // we need to schedule the move
        x86Method.addLines(prepInstructions.get(indexOfArgument));
        addedPrep.add(indexOfArgument);
        var registerCache = x86ValueResolver.pushStackNoSave();
        temp.put(indexOfArgument, registerCache);
        moveToStack(resolvedArgument,
            registerCache);
        resolvedArguments.set(indexOfArgument,
            registerCache);
      }
    }
    var tempSaved = new ArrayList<X64Instruction>();
    for (int indexOfArgument = resolvedArguments.size() - 1; indexOfArgument >= 0; indexOfArgument--) {
      var resolvedArgument = resolvedArguments.get(indexOfArgument);
      var correctArgumentRegister = new X86RegisterMappedValue(destinationRegisters.get(indexOfArgument),
          resolvedArgument.getValue());
      if (temp.containsKey(indexOfArgument)) {
        if (resolvedArgument instanceof X86ConstantValue x86ConstantValue &&
            x86ConstantValue.getValue() instanceof IrStringConstant) {
          tempSaved.add(new X64BinaryInstruction(X64BinaryInstructionType.leaq,
              resolvedArgument,
              correctArgumentRegister));
        } else {
          tempSaved.add(new X64BinaryInstruction(X64BinaryInstructionType.movq,
              resolvedArgument,
              correctArgumentRegister));
        }
      } else {
        if (!addedPrep.contains(indexOfArgument))
          x86Method.addLines(prepInstructions.get(indexOfArgument));
        if (resolvedArgument instanceof X86ConstantValue x86ConstantValue &&
            x86ConstantValue.getValue() instanceof IrStringConstant) {
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.leaq,
              resolvedArgument,
              correctArgumentRegister));
        } else {
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
              resolvedArgument,
              correctArgumentRegister));
        }
      }
    }
    x86Method.addLines(tempSaved);
  }

  @Override
  public void emitInstruction(@NotNull FunctionCallWithResult functionCallWithResult) {
    callerSave(resolveIrValue(functionCallWithResult.getDestination()));
    schedule(functionCallWithResult);
    if (functionCallWithResult.isImported()) {
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl,
          X86RegisterMappedValue.unassigned(X86Register.EAX),
          X86RegisterMappedValue.unassigned(X86Register.EAX)));
      x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq,
                   new X64CallOperand(functionCallWithResult)))
               .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                   X86RegisterMappedValue.unassigned(X86Register.RAX),
                   resolveIrValue(functionCallWithResult.getDestination())));
    } else {
      x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq,
                   new X64CallOperand(functionCallWithResult)))
               .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                   X86RegisterMappedValue.unassigned(X86Register.RAX),
                   resolveIrValue(functionCallWithResult.getDestination())));
    }
    restoreStack(functionCallWithResult);
    callerRestore(resolveIrValue(functionCallWithResult.getDestination()));
    asmWriterContext.setMaxStackSpaceForArgs(functionCallWithResult);
  }

  private void restoreStack(@NotNull FunctionCall functionCall) {
    if (functionCall.getNumArguments() > N_ARG_REGISTERS) {
      long numStackArgs = functionCall.getNumArguments() - N_ARG_REGISTERS;
      x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.addq,
          new X86ConstantValue(new IrIntegerConstant((long) roundUp16((int) (numStackArgs * WORD_SIZE)),
              Type.Int)),
          X86RegisterMappedValue.unassigned(X86Register.RSP)));
    }
  }

  @Override
  public void emitInstruction(@NotNull Method method) {
    asmWriterContext.setLastComparisonOperator(null);
    currentMethod = method;

    if (!asmWriterContext.isTextLabelAdded()) {
      x86Method.addLine(new X86MetaData(".text"));
      asmWriterContext.setTextLabelAdded();
    }

    if (method.isMain()) x86Method.addLine(new X86MetaData(".global _main"))
                                  .addLine(new X86MetaData(".p2align  4, 0x90"));

    if (method.isMain()) {
      x86Method.addLine(new X86MetaData("_main:"));
    } else {
      x86Method.addLine(new X86MetaData(method.methodName() + ":"));
    }

    asmWriterContext.setLocationOfSubqInst(x86Method.size());
    saveMethodArgsToLocations(method);
  }

  @Override
  public void emitInstruction(@NotNull ConditionalBranch conditionalBranch) {
    var resolvedCondition = resolveIrValue(conditionalBranch.getCondition());
    if (asmWriterContext.getLastComparisonOperator() == null) {
      if (conditionalBranch.getCondition() instanceof IrIntegerConstant) {
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                     resolvedCondition,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq,
                     new X86ConstantValue(IrIntegerConstant.zero()),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.je,
                     new X64JumpTargetOperand(conditionalBranch.getTarget())));
      } else {
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq,
                     new X86ConstantValue(IrIntegerConstant.zero()),
                     resolvedCondition))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.je,
                     new X64JumpTargetOperand(conditionalBranch.getTarget())));
      }
      return;
    } else {
      x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.getCorrectJumpIfFalseInstruction(asmWriterContext.getLastComparisonOperator()),
          new X64JumpTargetOperand(conditionalBranch.getTarget())));
    }
    asmWriterContext.setLastComparisonOperator(null);
  }

  @Override
  public void emitInstruction(@NotNull FunctionCallNoResult functionCallNoResult) {
    callerSave(null);
    schedule(functionCallNoResult);
    if (functionCallNoResult.isImported()) x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl,
                                                        X86RegisterMappedValue.unassigned(X86Register.EAX),
                                                        X86RegisterMappedValue.unassigned(X86Register.EAX)))
                                                    .addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq,
                                                        new X64CallOperand(functionCallNoResult)));
    else x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq,
        new X64CallOperand(functionCallNoResult)));
    restoreStack(functionCallNoResult);
    callerRestore(null);
  }

  @Override
  public void emitInstruction(@NotNull MethodEnd methodEnd) {
    var space = new X86ConstantValue(new IrIntegerConstant((long) roundUp16(-x86ValueResolver.getCurrentStackOffset()),
        Type.Int));

    x86Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(),
        new X64BinaryInstruction(X64BinaryInstructionType.subq,
            space,
            X86RegisterMappedValue.unassigned(X86Register.RSP)));
    calleeSave();
    x86Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(),
        new X64BinaryInstruction(X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(X86Register.RSP),
            X86RegisterMappedValue.unassigned(X86Register.RBP)));
    x86Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(),
        new X64UnaryInstruction(X64UnaryInstructionType.pushq,
            X86RegisterMappedValue.unassigned(X86Register.RBP)));
    x86Method = (methodEnd.isMain() ? x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl,
        X86RegisterMappedValue.unassigned(X86Register.EAX),
        X86RegisterMappedValue.unassigned(X86Register.EAX))): x86Method);

    x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.addq,
        space,
        X86RegisterMappedValue.unassigned(X86Register.RSP)));

    calleeRestore();
    x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                 X86RegisterMappedValue.unassigned(X86Register.RBP),
                 X86RegisterMappedValue.unassigned(X86Register.RSP)))
             .addLine(new X64UnaryInstruction(X64UnaryInstructionType.popq,
                 X86RegisterMappedValue.unassigned(X86Register.RBP)));
    x86Method.addLine(new X64NoOperandInstruction(X64NopInstructionType.retq));
  }

  @Override
  public void emitInstruction(@NotNull ReturnInstruction returnInstruction) {
    if (returnInstruction.getReturnAddress()
                         .isPresent()) x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
        resolveIrValue(returnInstruction.getReturnAddress()
                                        .get()),
        X86RegisterMappedValue.unassigned(X86Register.RAX)));
  }

  @Override
  public void emitInstruction(@NotNull UnaryInstruction unaryInstruction) {
    switch (unaryInstruction.operator) {
      case Operators.NOT -> {
        asmWriterContext.setLastComparisonOperator(null);
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                     resolveIrValue(unaryInstruction.operand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                     resolveIrValue(unaryInstruction.getDestination())))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorq,
                     new X86ConstantValue(IrIntegerConstant.one()),
                     resolveIrValue(unaryInstruction.getDestination())));
      }
      case Operators.MINUS -> x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                                           resolveIrValue(unaryInstruction.operand),
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                                       .addLine(new X64UnaryInstruction(X64UnaryInstructionType.neg,
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                                       .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                                           resolveIrValue(unaryInstruction.getDestination())));
      default -> throw new IllegalStateException(unaryInstruction.toString());
    }
  }

  @Override
  public void emitInstruction(@NotNull UnconditionalBranch unconditionalBranch) {
    x86Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.jmp,
        new X64JumpTargetOperand(unconditionalBranch.getTarget())));
  }

  @Override
  public void emitInstruction(@NotNull ArrayBoundsCheck arrayBoundsCheck) {
  }

  @Override
  public void emitInstruction(@NotNull RuntimeException runtimeException) {
  }

  @Override
  public void emitInstruction(@NotNull CopyInstruction copyInstruction) {
    if (!resolveIrValue(copyInstruction.getValue()).equals(resolveIrValue(copyInstruction.getDestination()))) {
      if (resolveIrValue(copyInstruction.getValue()) instanceof X86RegisterMappedValue ||
          resolveIrValue(copyInstruction.getValue()) instanceof X86ConstantValue)
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            resolveIrValue(copyInstruction.getValue()),
            resolveIrValue(copyInstruction.getDestination())));
      else x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                        resolveIrValue(copyInstruction.getValue()),
                        X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                    .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                        X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                        resolveIrValue(copyInstruction.getDestination())));
    }
  }

  @Override
  public void emitInstruction(@NotNull GetAddress getAddress) {
  }

  @Override
  public void emitInstruction(@NotNull BinaryInstruction binaryInstruction) {
    switch (binaryInstruction.operator) {
      case Operators.PLUS, Operators.MINUS, Operators.MULTIPLY, Operators.CONDITIONAL_OR, Operators.CONDITIONAL_AND ->
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.getX64BinaryInstruction(binaryInstruction.operator),
                       resolveIrValue(binaryInstruction.sndOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                       resolveIrValue(binaryInstruction.getDestination())));
      case Operators.DIVIDE, Operators.MOD -> {
        // If we are planning to use RDX, we spill it first
        if (!resolveIrValue(binaryInstruction.getDestination()).toString()
                                                               .equals(X86Register.RDX.toString()))
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
              X86RegisterMappedValue.unassigned(X86Register.RDX),
              resolveNextStackLocation(X86RegisterMappedValue.unassigned(X86Register.RDX))));
        if (binaryInstruction.sndOperand instanceof IrIntegerConstant) {
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(X86Register.RAX)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.sndOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(X64UnaryInstructionType.idivq,
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)));
        } else {
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(X86Register.RAX)))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(X64UnaryInstructionType.idivq,
                       resolveIrValue(binaryInstruction.sndOperand)));
        }
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(binaryInstruction.operator.equals("%") ? X86Register.RDX: X86Register.RAX),
            resolveIrValue(binaryInstruction.getDestination())));
        // restore RDX
        if (!resolveIrValue(binaryInstruction.getDestination()).toString()
                                                               .equals(X86Register.RDX.toString()))
          x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
              resolveNextStackLocation(X86RegisterMappedValue.unassigned(X86Register.RDX)),
              X86RegisterMappedValue.unassigned(X86Register.RDX)));
      }
      // comparison operators
      case Operators.EQ, Operators.NEQ, Operators.LT, Operators.GT, Operators.LEQ, Operators.GEQ -> {
        asmWriterContext.setLastComparisonOperator(binaryInstruction.operator);
        x86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                     resolveIrValue(binaryInstruction.fstOperand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq,
                     resolveIrValue(binaryInstruction.sndOperand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.getCorrectComparisonSetInstruction(binaryInstruction.operator),
                     X86RegisterMappedValue.unassigned(X86Register.al)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movzbq,
                     X86RegisterMappedValue.unassigned(X86Register.al),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                     resolveIrValue(binaryInstruction.getDestination())));
      }
      default -> throw new IllegalStateException(binaryInstruction.toString());
    }
  }
}

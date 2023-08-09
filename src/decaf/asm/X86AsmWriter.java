package decaf.asm;

import static com.google.common.base.Preconditions.checkNotNull;
import static decaf.asm.X86Register.N_ARG_REGISTERS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import decaf.asm.instructions.X64BinaryInstruction;
import decaf.asm.instructions.X64Instruction;
import decaf.asm.instructions.X64NoOperandInstruction;
import decaf.asm.instructions.X64UnaryInstruction;
import decaf.asm.instructions.X86MetaData;
import decaf.asm.operands.X64CallOperand;
import decaf.asm.operands.X64JumpTargetOperand;
import decaf.asm.operands.X86ConstantValue;
import decaf.asm.operands.X86MemoryAddressComputation;
import decaf.asm.operands.X86MemoryAddressInRegister;
import decaf.asm.operands.X86RegisterMappedValue;
import decaf.asm.operands.X86StackMappedValue;
import decaf.asm.operands.X86Value;
import decaf.asm.types.X64BinaryInstructionType;
import decaf.asm.types.X64NopInstructionType;
import decaf.asm.types.X64UnaryInstructionType;
import decaf.ast.Type;
import decaf.codegen.TraceScheduler;
import decaf.codegen.codes.ArrayBoundsCheck;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.ConditionalBranch;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCall;
import decaf.codegen.codes.FunctionCallNoResult;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.GetAddress;
import decaf.codegen.codes.GlobalAllocation;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.codes.ReturnInstruction;
import decaf.codegen.codes.RuntimeException;
import decaf.codegen.codes.StringConstantAllocation;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.codes.UnconditionalBranch;
import decaf.codegen.names.IrIntegerConstant;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrStackArray;
import decaf.codegen.names.IrStringConstant;
import decaf.codegen.names.IrValue;
import decaf.common.CompilationContext;
import decaf.common.Operators;
import decaf.common.ProgramIr;
import decaf.common.Utils;
import decaf.regalloc.RegisterAllocator;

public class X86AsmWriter implements AsmWriter {

  private static final X86Register COPY_TEMP_REGISTER = X86Register.R10;

  private final RegisterAllocator registerAllocator;

  private final ProgramIr programIr;

  private final X86Program x86Program = new X86Program();

  private final AsmWriterContext asmWriterContext = new AsmWriterContext();

  private final X86ValueResolver x86ValueResolver;

  private X86Method x86Method = new X86Method();

  private Instruction currentInstruction;

  private Method currentMethod;


  public X86AsmWriter(
      ProgramIr programIr,
      RegisterAllocator registerAllocator
  ) {
    this.registerAllocator = registerAllocator;
    this.programIr = programIr;
    this.x86ValueResolver = new X86ValueResolver(
        programIr,
        registerAllocator
    );
    if (CompilationContext.isDebugModeOn()) {
      System.out.println(registerAllocator.getVariableToRegisterMap());
    }
    emit();
    currentMethod = programIr.getMethods()
                             .get(0);
  }

  private X86Value resolveIrValue(IrValue irValue) {
    return x86ValueResolver.resolveIrValue(
        irValue,
        true
    );
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
        prologue.add(new X86MetaData(globalAllocation.toString()));
      } else {
        throw new IllegalStateException();
      }
    }
    x86Program.addPrologue(prologue);
    x86Program.add(new X86MetaData("\n"));
  }

  private void emitProgramEpilogue() {
    x86Program.addEpilogue(List.of(new X86MetaData(".subsections_via_symbols")));
  }

  private void emitMethods() {
    for (var method : programIr.getMethods()) {
      x86Program.addMethod(emitMethod(method));
    }
  }

  private X86Method emitMethod(Method method) {
    currentMethod = method;
    x86Method = new X86Method();
    x86ValueResolver.prepareForMethod(
        currentMethod,
        x86Method
    );
    for (var instructionList : TraceScheduler.getInstructionTrace(method)) {
      if (!instructionList.isEntry() && !instructionList.getLabel()
                                                        .equals("UNSET")) {
        var label = instructionList.getLabel();
        x86Method.add(new X86MetaData(String.format(
            ".%s:",
            label
        )));
      }
      for (var instruction : instructionList) {
        currentInstruction = instruction;
        instruction.accept(this);
      }
    }
    return x86Method;
  }

  public X86Program getX86Program() {
    return x86Program;
  }

  public X86Value resolveNextStackLocation(X86Value x86Value) {
    return x86ValueResolver.resolveNextStackLocation(x86Value);
  }

  private void calleeSave() {
    var toSave = X86Register.calleeSaved;
    for (var register : toSave) {
      x86Method.addAtIndex(
          asmWriterContext.getLocationOfSubqInst(),
          new X64UnaryInstruction(
              X64UnaryInstructionType.pushq,
              X86RegisterMappedValue.unassigned(register)
          )
      );
    }
  }

  private void calleeRestore() {
    var toRestore = new ArrayList<>(X86Register.calleeSaved);
    for (var register : toRestore) {
      x86Method.addLine(new X64UnaryInstruction(
          X64UnaryInstructionType.popq,
          X86RegisterMappedValue.unassigned(register)
      ));
    }
  }

  private void saveMethodArgsToLocations(Method method) {
    // next we save all the arguments to their corresponding locations
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames()
                                                        .size(); parameterIndex++) {
      var parameter = method.getParameterNames()
                            .get(parameterIndex);

      var dst = x86ValueResolver.resolveInitialArgumentLocation(parameter);

      if (parameterIndex < N_ARG_REGISTERS) {
        var src = new X86RegisterMappedValue(
            X86Register.argumentRegisters.get(parameterIndex),
            parameter
        );
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            src,
            dst
        ));
      } else {
        var src = new X86StackMappedValue(
            X86Register.RBP,
            (parameterIndex - 5) * Utils.WORD_SIZE + 8,
            parameter
        );
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            src,
            new X86RegisterMappedValue(
                COPY_TEMP_REGISTER,
                parameter
            )
        ));
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
            dst
        ));
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

  private void callerRestore(X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(
                                               currentMethod,
                                               Collections.emptyMap()
                                           )
                                           .getOrDefault(
                                               currentInstruction,
                                               Collections.emptySet()
                                           );
    var toRestore = registerMapping.stream()
                                   .filter(X86Register.callerSaved::contains)
                                   .toList();
    for (var x64Register : toRestore) {
      if (x64Register.equals(X86Register.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(
          returnAddressRegister,
          regOperand
      )) {
        var location = resolveNextStackLocation(regOperand);
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            location,
            regOperand
        ));
      }
    }
  }

  private void callerSave(X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(
                                               currentMethod,
                                               Collections.emptyMap()
                                           )
                                           .getOrDefault(
                                               currentInstruction,
                                               Collections.emptySet()
                                           );
    var toSave = registerMapping.stream()
                                .filter(X86Register.callerSaved::contains)
                                .toList();
    int startIndex = x86Method.size();
    for (var x64Register : toSave) {
      if (x64Register.equals(X86Register.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(
          returnAddressRegister,
          regOperand
      )) {
        var location = resolveNextStackLocation(regOperand);
        x86Method.addAtIndex(
            startIndex,
            new X64BinaryInstruction(
                X64BinaryInstructionType.movq,
                regOperand,
                location
            )
        );
      }
    }
  }


  private void emitStackArgumentsInstructions(FunctionCall functionCall) {
    var arguments = functionCall.getArguments();
    // we need to create stack space for the start arguments
    if (arguments.size() > N_ARG_REGISTERS) {
      var stackSpaceForArgs = new X86ConstantValue(new IrIntegerConstant(
          (long) Utils.roundUp16((arguments.size() - N_ARG_REGISTERS) * Utils.WORD_SIZE),
          Type.Int
      ));
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.subq,
          stackSpaceForArgs,
          X86RegisterMappedValue.unassigned(X86Register.RSP)
      ));
    }
    for (int stackArgumentIndex = N_ARG_REGISTERS; stackArgumentIndex < arguments.size(); stackArgumentIndex++) {
      var stackArgument = arguments.get(stackArgumentIndex);
      final var dst = new X86StackMappedValue(
          X86Register.RSP,
          (stackArgumentIndex - N_ARG_REGISTERS) * Utils.WORD_SIZE
      );
      if (stackArgument instanceof IrStackArray || x86ValueResolver.isStackMappedIrValue(stackArgument) ||
          stackArgument instanceof IrMemoryAddress) {
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            resolveIrValue(arguments.get(stackArgumentIndex)),
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
        ));
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
            dst
        ));
      } else {
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            resolveIrValue(arguments.get(stackArgumentIndex)),
            dst
        ));
      }
    }
  }

  private boolean anyResolvedValuesUseRegister(
      Collection<X86Value> resolvedValues,
      X86Register register
  ) {
    return resolvedValues.stream()
                         .anyMatch(x86Value -> x86Value.registersInUse()
                                                       .contains(register));
  }

  private void moveToStack(
      X86Value resolvedArgument,
      X86Value registerCache
  ) {
    if (resolvedArgument instanceof X86MemoryAddressComputation) {
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.movq,
          resolvedArgument,
          X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
      ));
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.movq,
          X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
          registerCache
      ));
    } else {
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.movq,
          resolvedArgument,
          registerCache
      ));
    }
  }

  public void schedule(FunctionCall functionCall) {
    emitStackArgumentsInstructions(functionCall);

    var registerArguments = new ArrayList<>(functionCall.getArguments()
                                                        .subList(
                                                            0,
                                                            Math.min(
                                                                N_ARG_REGISTERS,
                                                                functionCall.getNumArguments()
                                                            )
                                                        ));
    var resolvedArguments = new ArrayList<X86Value>();
    var prepInstructions = new ArrayList<List<X64Instruction>>();
    var destinationRegisters = List.copyOf(X86Register.argumentRegisters.subList(
        0,
        registerArguments.size()
    ));
    for (var registerArgument : registerArguments) {
      var resolvedX86Value = x86ValueResolver.resolveIrValue(
          registerArgument,
          false
      );
      resolvedArguments.add(resolvedX86Value);
      prepInstructions.add(x86ValueResolver.getPreparatoryInstructions());
    }

    var addedPrep = new HashSet<Integer>();
    var temp = new HashMap<Integer, X86StackMappedValue>();
    for (int indexOfArgument = 0; indexOfArgument < resolvedArguments.size(); indexOfArgument++) {
      var resolvedArgument = resolvedArguments.get(indexOfArgument);
      var destinationRegister = destinationRegisters.get(indexOfArgument);

      if (anyResolvedValuesUseRegister(
          resolvedArguments.subList(
              indexOfArgument,
              resolvedArguments.size()
          ),
          destinationRegister
      )) {
        // we need to schedule the move
        x86Method.addLines(prepInstructions.get(indexOfArgument));
        addedPrep.add(indexOfArgument);
        var registerCache = x86ValueResolver.pushStackNoSave();
        temp.put(
            indexOfArgument,
            registerCache
        );
        moveToStack(
            resolvedArgument,
            registerCache
        );
        resolvedArguments.set(
            indexOfArgument,
            registerCache
        );
      }
    }
    var tempSaved = new ArrayList<X64Instruction>();
    for (int indexOfArgument = resolvedArguments.size() - 1; indexOfArgument >= 0; indexOfArgument--) {
      var resolvedArgument = resolvedArguments.get(indexOfArgument);
      var correctArgumentRegister = new X86RegisterMappedValue(
          destinationRegisters.get(indexOfArgument),
          resolvedArgument.getValue()
      );
      if (temp.containsKey(indexOfArgument)) {
        if (resolvedArgument instanceof X86ConstantValue x86ConstantValue &&
            x86ConstantValue.getValue() instanceof IrStringConstant) {
          tempSaved.add(new X64BinaryInstruction(
              X64BinaryInstructionType.leaq,
              resolvedArgument,
              correctArgumentRegister
          ));
        } else {
          tempSaved.add(new X64BinaryInstruction(
              X64BinaryInstructionType.movq,
              resolvedArgument,
              correctArgumentRegister
          ));
        }
      } else {
        if (!addedPrep.contains(indexOfArgument)) x86Method.addLines(prepInstructions.get(indexOfArgument));
        if (resolvedArgument instanceof X86ConstantValue x86ConstantValue &&
            x86ConstantValue.getValue() instanceof IrStringConstant) {
          x86Method.addLine(new X64BinaryInstruction(
              X64BinaryInstructionType.leaq,
              resolvedArgument,
              correctArgumentRegister
          ));
        } else {
          x86Method.addLine(new X64BinaryInstruction(
              X64BinaryInstructionType.movq,
              resolvedArgument,
              correctArgumentRegister
          ));
        }
      }
    }
    x86Method.addLines(tempSaved);
  }

  @Override
  public void emitInstruction(FunctionCallWithResult functionCallWithResult) {
    callerSave(resolveIrValue(functionCallWithResult.getDestination()));
    schedule(functionCallWithResult);
    if (functionCallWithResult.isImported()) {
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.xorl,
          X86RegisterMappedValue.unassigned(X86Register.EAX),
          X86RegisterMappedValue.unassigned(X86Register.EAX)
      ));
      x86Method.addLine(new X64UnaryInstruction(
                   X64UnaryInstructionType.callq,
                   new X64CallOperand(functionCallWithResult)
               ))
               .addLine(new X64BinaryInstruction(
                   X64BinaryInstructionType.movq,
                   X86RegisterMappedValue.unassigned(X86Register.RAX),
                   resolveIrValue(functionCallWithResult.getDestination())
               ));
    } else {
      x86Method.addLine(new X64UnaryInstruction(
                   X64UnaryInstructionType.callq,
                   new X64CallOperand(functionCallWithResult)
               ))
               .addLine(new X64BinaryInstruction(
                   X64BinaryInstructionType.movq,
                   X86RegisterMappedValue.unassigned(X86Register.RAX),
                   resolveIrValue(functionCallWithResult.getDestination())
               ));
    }
    restoreStack(functionCallWithResult);
    callerRestore(resolveIrValue(functionCallWithResult.getDestination()));
    asmWriterContext.setMaxStackSpaceForArgs(functionCallWithResult);
  }

  private void restoreStack(FunctionCall functionCall) {
    if (functionCall.getNumArguments() > N_ARG_REGISTERS) {
      long numStackArgs = functionCall.getNumArguments() - N_ARG_REGISTERS;
      x86Method.addLine(new X64BinaryInstruction(
          X64BinaryInstructionType.addq,
          new X86ConstantValue(new IrIntegerConstant(
              (long) Utils.roundUp16((int) (numStackArgs * Utils.WORD_SIZE)),
              Type.Int
          )),
          X86RegisterMappedValue.unassigned(X86Register.RSP)
      ));
    }
  }

  @Override
  public void emitInstruction(Method method) {
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
  public void emitInstruction(ConditionalBranch conditionalBranch) {
    var resolvedCondition = resolveIrValue(conditionalBranch.getCondition());
    if (asmWriterContext.getLastComparisonOperator() == null) {
      if (conditionalBranch.getCondition() instanceof IrIntegerConstant) {
        x86Method.addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movq,
                     resolvedCondition,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.cmpq,
                     new X86ConstantValue(IrIntegerConstant.zero()),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64UnaryInstruction(
                     X64UnaryInstructionType.je,
                     new X64JumpTargetOperand(conditionalBranch.getTarget())
                 ));
      } else {
        x86Method.addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.cmpq,
                     new X86ConstantValue(IrIntegerConstant.zero()),
                     resolvedCondition
                 ))
                 .addLine(new X64UnaryInstruction(
                     X64UnaryInstructionType.je,
                     new X64JumpTargetOperand(conditionalBranch.getTarget())
                 ));
      }
      return;
    } else {
      x86Method.addLine(new X64UnaryInstruction(
          X64UnaryInstructionType.getCorrectJumpIfFalseInstruction(asmWriterContext.getLastComparisonOperator()),
          new X64JumpTargetOperand(conditionalBranch.getTarget())
      ));
    }
    asmWriterContext.setLastComparisonOperator(null);
  }

  @Override
  public void emitInstruction(FunctionCallNoResult functionCallNoResult) {
    callerSave(null);
    schedule(functionCallNoResult);
    if (functionCallNoResult.isImported()) x86Method.addLine(new X64BinaryInstruction(
                                                        X64BinaryInstructionType.xorl,
                                                        X86RegisterMappedValue.unassigned(X86Register.EAX),
                                                        X86RegisterMappedValue.unassigned(X86Register.EAX)
                                                    ))
                                                    .addLine(new X64UnaryInstruction(
                                                        X64UnaryInstructionType.callq,
                                                        new X64CallOperand(functionCallNoResult)
                                                    ));
    else x86Method.addLine(new X64UnaryInstruction(
        X64UnaryInstructionType.callq,
        new X64CallOperand(functionCallNoResult)
    ));
    restoreStack(functionCallNoResult);
    callerRestore(null);
  }

  @Override
  public void emitInstruction(MethodEnd methodEnd) {
    var space = new X86ConstantValue(new IrIntegerConstant(
        (long) Utils.roundUp16(-x86ValueResolver.getCurrentStackOffset()),
        Type.Int
    ));

    x86Method.addAtIndex(
        asmWriterContext.getLocationOfSubqInst(),
        new X64BinaryInstruction(
            X64BinaryInstructionType.subq,
            space,
            X86RegisterMappedValue.unassigned(X86Register.RSP)
        )
    );
    calleeSave();
    x86Method.addAtIndex(
        asmWriterContext.getLocationOfSubqInst(),
        new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(X86Register.RSP),
            X86RegisterMappedValue.unassigned(X86Register.RBP)
        )
    );
    x86Method.addAtIndex(
        asmWriterContext.getLocationOfSubqInst(),
        new X64UnaryInstruction(
            X64UnaryInstructionType.pushq,
            X86RegisterMappedValue.unassigned(X86Register.RBP)
        )
    );
    x86Method = (methodEnd.isMain() ? x86Method.addLine(new X64BinaryInstruction(
        X64BinaryInstructionType.xorl,
        X86RegisterMappedValue.unassigned(X86Register.EAX),
        X86RegisterMappedValue.unassigned(X86Register.EAX)
    )): x86Method);

    x86Method.addLine(new X64BinaryInstruction(
        X64BinaryInstructionType.addq,
        space,
        X86RegisterMappedValue.unassigned(X86Register.RSP)
    ));

    calleeRestore();
    x86Method.addLine(new X64BinaryInstruction(
                 X64BinaryInstructionType.movq,
                 X86RegisterMappedValue.unassigned(X86Register.RBP),
                 X86RegisterMappedValue.unassigned(X86Register.RSP)
             ))
             .addLine(new X64UnaryInstruction(
                 X64UnaryInstructionType.popq,
                 X86RegisterMappedValue.unassigned(X86Register.RBP)
             ));
    x86Method.addLine(new X64NoOperandInstruction(X64NopInstructionType.retq));
  }

  @Override
  public void emitInstruction(ReturnInstruction returnInstruction) {
    if (returnInstruction.getReturnAddress()
                         .isPresent()) x86Method.addLine(new X64BinaryInstruction(
        X64BinaryInstructionType.movq,
        resolveIrValue(returnInstruction.getReturnAddress()
                                        .get()),
        X86RegisterMappedValue.unassigned(X86Register.RAX)
    ));
  }

  @Override
  public void emitInstruction(UnaryInstruction unaryInstruction) {
    switch (unaryInstruction.operator) {
      case Operators.NOT -> {
        asmWriterContext.setLastComparisonOperator(null);
        x86Method.addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movq,
                     resolveIrValue(unaryInstruction.operand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movq,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                     resolveIrValue(unaryInstruction.getDestination())
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.xorq,
                     new X86ConstantValue(IrIntegerConstant.one()),
                     resolveIrValue(unaryInstruction.getDestination())
                 ));
      }
      case Operators.MINUS -> x86Method.addLine(new X64BinaryInstruction(
                                           X64BinaryInstructionType.movq,
                                           resolveIrValue(unaryInstruction.operand),
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                                       ))
                                       .addLine(new X64UnaryInstruction(
                                           X64UnaryInstructionType.neg,
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                                       ))
                                       .addLine(new X64BinaryInstruction(
                                           X64BinaryInstructionType.movq,
                                           X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                                           resolveIrValue(unaryInstruction.getDestination())
                                       ));
      default -> throw new IllegalStateException(unaryInstruction.toString());
    }
  }

  @Override
  public void emitInstruction(UnconditionalBranch unconditionalBranch) {
    x86Method.addLine(new X64UnaryInstruction(
        X64UnaryInstructionType.jmp,
        new X64JumpTargetOperand(unconditionalBranch.getTarget())
    ));
  }

  @Override
  public void emitInstruction(ArrayBoundsCheck arrayBoundsCheck) {
  }

  @Override
  public void emitInstruction(RuntimeException runtimeException) {
  }

  @Override
  public void emitInstruction(CopyInstruction copyInstruction) {
    if (!resolveIrValue(copyInstruction.getValue()).equals(resolveIrValue(copyInstruction.getDestination()))) {
      if (resolveIrValue(copyInstruction.getValue()) instanceof X86RegisterMappedValue ||
          resolveIrValue(copyInstruction.getValue()) instanceof X86ConstantValue ||
          resolveIrValue(copyInstruction.getValue()) instanceof X86MemoryAddressInRegister)
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            resolveIrValue(copyInstruction.getValue()),
            resolveIrValue(copyInstruction.getDestination())
        ));
      else x86Method.addLine(new X64BinaryInstruction(
                        X64BinaryInstructionType.movq,
                        resolveIrValue(copyInstruction.getValue()),
                        X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                    ))
                    .addLine(new X64BinaryInstruction(
                        X64BinaryInstructionType.movq,
                        X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                        resolveIrValue(copyInstruction.getDestination())
                    ));
    }
  }

  @Override
  public void emitInstruction(GetAddress getAddress) {
    x86Method.addLine(X86MetaData.blockComment(String.format("&(%s)",
                                                             getAddress.getSource()
                                                                       .getSourceCode()
    )));
    x86ValueResolver.processGetAddress(getAddress);
  }

  @Override
  public void emitInstruction(BinaryInstruction binaryInstruction) {
    x86Method.addLine(X86MetaData.blockComment(binaryInstruction.getSource()
                                                                .getSourceCode()));
    switch (binaryInstruction.operator) {
      case Operators.PLUS, Operators.MINUS, Operators.MULTIPLY, Operators.CONDITIONAL_OR, Operators.CONDITIONAL_AND ->
          x86Method.addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                   ))
                   .addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.getX64BinaryInstruction(binaryInstruction.operator),
                       resolveIrValue(binaryInstruction.sndOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                   ))
                   .addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.movq,
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                       resolveIrValue(binaryInstruction.getDestination())
                   ));
      case Operators.DIVIDE, Operators.MOD -> {
        // If we are planning to use RDX, we spill it first
        if (!resolveIrValue(binaryInstruction.getDestination()).toString()
                                                               .equals(X86Register.RDX.toString()))
          x86Method.addLine(new X64BinaryInstruction(
              X64BinaryInstructionType.movq,
              X86RegisterMappedValue.unassigned(X86Register.RDX),
              resolveNextStackLocation(X86RegisterMappedValue.unassigned(X86Register.RDX))
          ));
        if (binaryInstruction.sndOperand instanceof IrIntegerConstant) {
          x86Method.addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(X86Register.RAX)
                   ))
                   .addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.sndOperand),
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                   ))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(
                       X64UnaryInstructionType.idivq,
                       X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                   ));
        } else {
          x86Method.addLine(new X64BinaryInstruction(
                       X64BinaryInstructionType.movq,
                       resolveIrValue(binaryInstruction.fstOperand),
                       X86RegisterMappedValue.unassigned(X86Register.RAX)
                   ))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(
                       X64UnaryInstructionType.idivq,
                       resolveIrValue(binaryInstruction.sndOperand)
                   ));
        }
        x86Method.addLine(new X64BinaryInstruction(
            X64BinaryInstructionType.movq,
            X86RegisterMappedValue.unassigned(binaryInstruction.operator.equals("%") ? X86Register.RDX: X86Register.RAX),
            resolveIrValue(binaryInstruction.getDestination())
        ));
        // restore RDX
        if (!resolveIrValue(binaryInstruction.getDestination()).toString()
                                                               .equals(X86Register.RDX.toString()))
          x86Method.addLine(new X64BinaryInstruction(
              X64BinaryInstructionType.movq,
              resolveNextStackLocation(X86RegisterMappedValue.unassigned(X86Register.RDX)),
              X86RegisterMappedValue.unassigned(X86Register.RDX)
          ));
      }
      // comparison operators
      case Operators.EQ, Operators.NEQ, Operators.LT, Operators.GT, Operators.LEQ, Operators.GEQ -> {
        asmWriterContext.setLastComparisonOperator(binaryInstruction.operator);
        x86Method.addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movq,
                     resolveIrValue(binaryInstruction.fstOperand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.cmpq,
                     resolveIrValue(binaryInstruction.sndOperand),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64UnaryInstruction(
                     X64UnaryInstructionType.getCorrectComparisonSetInstruction(binaryInstruction.operator),
                     X86RegisterMappedValue.unassigned(X86Register.al)
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movzbq,
                     X86RegisterMappedValue.unassigned(X86Register.al),
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)
                 ))
                 .addLine(new X64BinaryInstruction(
                     X64BinaryInstructionType.movq,
                     X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER),
                     resolveIrValue(binaryInstruction.getDestination())
                 ));
      }
      default -> throw new IllegalStateException(binaryInstruction.toString());
    }
  }
}

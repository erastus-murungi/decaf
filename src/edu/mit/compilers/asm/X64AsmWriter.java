package edu.mit.compilers.asm;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.mit.compilers.asm.X64RegisterType.N_ARG_REGISTERS;
import static edu.mit.compilers.utils.Utils.WORD_SIZE;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import edu.mit.compilers.asm.instructions.X64BinaryInstruction;
import edu.mit.compilers.asm.instructions.X64Instruction;
import edu.mit.compilers.asm.instructions.X64MetaData;
import edu.mit.compilers.asm.instructions.X64NoOperandInstruction;
import edu.mit.compilers.asm.instructions.X64UnaryInstruction;
import edu.mit.compilers.asm.operands.X64CallOperand;
import edu.mit.compilers.asm.operands.X64JumpTargetOperand;
import edu.mit.compilers.asm.operands.X86ConstantValue;
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
import edu.mit.compilers.codegen.names.IrStringConstant;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.registerallocation.RegisterAllocator;
import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.Operators;
import edu.mit.compilers.utils.ProgramIr;

public class X64AsmWriter implements AsmWriter {
  @NotNull
  private final RegisterAllocator registerAllocator;
  @NotNull
  private final ProgramIr programIr;
  @NotNull
  private final List<X64Method> emittedMethods = new ArrayList<>();
  @NotNull
  private final List<X64Instruction> epilogue = new ArrayList<>();
  @NotNull
  private final List<X64Instruction> prologue = new ArrayList<>();
  @NotNull
  private final X64RegisterType COPY_TEMP_REGISTER = X64RegisterType.R10;
  @NotNull
  private final AsmWriterContext asmWriterContext = new AsmWriterContext();
  @NotNull
  private final X64ValueResolver x64ValueResolver;
  @NotNull
  private X64Method x64Method = new X64Method();
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
    this.x64ValueResolver = new X64ValueResolver(programIr, registerAllocator);
    if (CLI.debug) {
      System.out.println(registerAllocator.getVariableToRegisterMap());
    }
    emit();
    currentMethod = programIr.getMethods().get(0);
  }

  private static int roundUp16(int n) {
    if (n == 0) return 16;
    return n >= 0 ? ((n + 16 - 1) / 16) * 16: (n / 16) * 16;
  }

  private X86Value resolveIrValue(@NotNull IrValue irValue) {
    return x64ValueResolver.resolveIrValue(irValue);
  }

  private void emit() {
    emitProgramPrologue();
    emitMethods();
    emitProgramEpilogue();
  }

  private void emitProgramPrologue() {
    prologue.add(new X64MetaData(".data"));
    for (Instruction instruction : programIr.getPrologue()) {
      if (instruction instanceof StringConstantAllocation stringConstantAllocation) {
        prologue.add(new X64MetaData(stringConstantAllocation.getASM()));
      } else if (instruction instanceof GlobalAllocation globalAllocation) {
        prologue.add(new X64MetaData(String.format(".comm %s, %s, %s", globalAllocation.getValue(), globalAllocation.getSize(), 64)));
      } else {
        throw new IllegalStateException();
      }
    }
  }

  private void emitProgramEpilogue() {
    epilogue.add(new X64MetaData(".subsections_via_symbols"));
  }

  private void emitMethods() {
    for (var method : programIr.getMethods()) {
      emittedMethods.add(emitMethod(method));
    }
  }

  private X64Method emitMethod(@NotNull Method method) {
    currentMethod = method;
    x64Method = new X64Method();
    x64ValueResolver.prepareForMethod(currentMethod, x64Method);
    for (var instructionList : TraceScheduler.getInstructionTrace(method)) {
      if (!instructionList.isEntry() && !instructionList.getLabel().equals("UNSET")) {
        var label = instructionList.getLabel();
        x64Method.add(new X64MetaData(String.format(".%s:", label)));
      }
      for (var instruction : instructionList) {
        currentInstruction = instruction;
        instruction.accept(this);
      }
    }
    return x64Method;
  }

  public X64Program convert() {
    return new X64Program(prologue, epilogue, emittedMethods);
  }

  public X86Value resolveNextStackLocation(@NotNull X64RegisterType registerType) {
    return x64ValueResolver.resolveNextStackLocation(registerType);
  }

  private void calleeSave() {
    var toSave = X64RegisterType.calleeSaved;
    for (var register : toSave) {
      x64Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(), new X64UnaryInstruction(X64UnaryInstructionType.pushq, X86RegisterMappedValue.unassigned(register)));
    }
  }

  private void calleeRestore() {
    var toRestore = new ArrayList<>(X64RegisterType.calleeSaved);
    for (var register : toRestore) {
      x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.popq, X86RegisterMappedValue.unassigned(register)));
    }
  }

  private void saveMethodArgsToLocations(@NotNull Method method) {
    // next we save all the arguments to their corresponding locations
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames().size(); parameterIndex++) {
      var parameter = method.getParameterNames().get(parameterIndex);

      var dst = x64ValueResolver.resolveInitialArgumentLocation(parameter);

      if (parameterIndex < N_ARG_REGISTERS) {
        var src = new X86RegisterMappedValue(X64RegisterType.parameterRegisters.get(parameterIndex), parameter);
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, src, dst));
      } else {
        var src = new X86StackMappedValue(X64RegisterType.RBP, (parameterIndex - 5) * WORD_SIZE + 8, parameter);
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, src, new X86RegisterMappedValue(COPY_TEMP_REGISTER, parameter)));
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), dst));
      }
    }
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames().size(); parameterIndex++) {
      var parameter = checkNotNull(method.getParameterNames().get(parameterIndex));
      if (x64ValueResolver.parameterUsedInCurrentMethod(parameter)) {
        checkNotNull(x64ValueResolver.resolveInitialArgumentLocation(parameter));
      }
    }
  }

  private void callerRestore(@Nullable X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(currentMethod, Collections.emptyMap())
                                           .getOrDefault(currentInstruction, Collections.emptySet());
    var toRestore = registerMapping.stream().filter(X64RegisterType.callerSaved::contains).toList();
    for (var x64Register : toRestore) {
      if (x64Register.equals(X64RegisterType.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(returnAddressRegister, regOperand)) {
        var location = resolveNextStackLocation(x64Register);
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, location, regOperand));
      }
    }
  }

  private void callerSave(@Nullable X86Value returnAddressRegister) {
    var registerMapping = registerAllocator.getMethodToLiveRegistersInfo()
                                           .getOrDefault(currentMethod, Collections.emptyMap())
                                           .getOrDefault(currentInstruction, Collections.emptySet());
    var toSave = registerMapping.stream().filter(X64RegisterType.callerSaved::contains).toList();
    int startIndex = x64Method.size();
    for (var x64Register : toSave) {
      if (x64Register.equals(X64RegisterType.STACK)) continue;
      var regOperand = X86RegisterMappedValue.unassigned(x64Register);
      if (!Objects.equals(returnAddressRegister, regOperand)) {
        var location = resolveNextStackLocation(x64Register);
        x64Method.addAtIndex(startIndex, new X64BinaryInstruction(X64BinaryInstructionType.movq, regOperand, location));
      }
    }
  }


  private List<X64Instruction> emitStackArgumentsInstructions(@NotNull FunctionCall functionCall) {
    var arguments = functionCall.getArguments();

    // instructions we will splice into the builder
    var instructions = new ArrayList<X64Instruction>();

    // we need to create stack space for the start arguments
    if (arguments.size() > N_ARG_REGISTERS) {
      var stackSpaceForArgs = new X86ConstantValue(new IrIntegerConstant((long) roundUp16((arguments.size() - N_ARG_REGISTERS) * WORD_SIZE), Type.Int));
      x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.subq, stackSpaceForArgs, X86RegisterMappedValue.unassigned(X64RegisterType.RSP)));
    }
    for (int stackArgumentIndex = N_ARG_REGISTERS; stackArgumentIndex < arguments.size(); stackArgumentIndex++) {
      var stackArgument = resolveIrValue(arguments.get(stackArgumentIndex));
      var dst = new X86StackMappedValue(X64RegisterType.RSP, (stackArgumentIndex - N_ARG_REGISTERS) * WORD_SIZE);
      if (stackArgument instanceof X86StackMappedValue) {
        instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, stackArgument, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)));
        instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), dst));
      } else {
        instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, stackArgument, dst));
      }
    }
    return instructions;
  }

  /**
   * Splices push arguments into the correct position in the XBuilder
   * while avoiding overwrites
   */
  private void orderFunctionCallArguments(@NotNull FunctionCall functionCall) {
    var arguments = functionCall.getArguments();

    // instructions we will splice into the builder
    var instructions = emitStackArgumentsInstructions(functionCall);
    // first let's copy the parameters we will push to the stack
    // this makes it easier to extract the most recent 6 parameters

    var copyOfPushArguments = new ArrayList<>(arguments);

    // extract the most recent pushes
    var argumentsToStoreInParameterRegisters = new ArrayList<>(copyOfPushArguments.subList(0, Math.min(N_ARG_REGISTERS, arguments.size())));

    // this is a map of a register to the argument it stores
    // note that the registers are represented as enums -> we use an EnumMap
    // note that X64Register.STACK is also included, so we need to consider it differently later
    var registerToResidentArgument = new EnumMap<X64RegisterType, Integer>(X64RegisterType.class);

    // we also have a reverse map to make looking an argument's register easier
    var residentArgumentToRegister = new HashMap<Integer, X64RegisterType>();

    // this is a map of arguments to the correct argument registers
    // we assign the arguments in the exact order, i.e RDI, RSI, RDX, RCX, R8, R9
    // this is why we use an indexOfArgument irAssignableValue to loop through the argument registers
    // in the correct order
    var pushParameterX64RegisterMap = new HashMap<Integer, X86RegisterMappedValue>();

    // the index of the parameter register
    int indexOfParameterRegister = 0;
    for (IrValue argument : argumentsToStoreInParameterRegisters) {
      // argument register
      var parameterRegister = X64RegisterType.parameterRegisters.get(indexOfParameterRegister);

      // the register which houses this argument
      // if the mapping doesn't contain a register for this argument, we default to X64Register.STACK
      var residentRegister = resolveIrValue(argument);
      if (residentRegister instanceof X86RegisterMappedValue x86RegisterMappedValue) {
        registerToResidentArgument.put(x86RegisterMappedValue.getX64RegisterType(), indexOfParameterRegister);
        residentArgumentToRegister.put(indexOfParameterRegister, x86RegisterMappedValue.getX64RegisterType());
      }

      pushParameterX64RegisterMap.put(indexOfParameterRegister, X86RegisterMappedValue.unassigned(parameterRegister));

      // march forward
      indexOfParameterRegister++;
    }

    // we create a set of parameter registers to make lookups more convenient
    var setOfParameterRegisters = Set.copyOf(X64RegisterType.parameterRegisters);

    // here we store arguments whose resident registers are the same as parameter registers
    // which could be potentially overwritten as we push arguments to parameter registers
    var potentiallyOverwrittenParameterRegisters = new HashMap<IrValue, X64RegisterType>();

    for (X64RegisterType x64RegisterType : registerToResidentArgument.keySet()) {
      if (setOfParameterRegisters.contains(x64RegisterType)) {
        potentiallyOverwrittenParameterRegisters.put(argumentsToStoreInParameterRegisters.get(registerToResidentArgument.get(x64RegisterType)), x64RegisterType);
        // this is the stack location storing the argument resident register
        var registerCache = resolveNextStackLocation(x64RegisterType);
        instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(x64RegisterType), registerCache));
      }
    }

    Collections.reverse(argumentsToStoreInParameterRegisters);
    indexOfParameterRegister = argumentsToStoreInParameterRegisters.size() - 1;
    for (IrValue argument : argumentsToStoreInParameterRegisters) {
      var argumentResidentRegister = residentArgumentToRegister.getOrDefault(indexOfParameterRegister, X64RegisterType.STACK);
      // if the argument is stored in the stack, just move it from the stack to parameter register
      if (argumentResidentRegister == X64RegisterType.STACK) {
        if (argument instanceof IrStringConstant)
          instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.leaq, resolveIrValue(argument), pushParameterX64RegisterMap.get(indexOfParameterRegister)));
        else {
          instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(argument), pushParameterX64RegisterMap.get(indexOfParameterRegister)));
        }
      } else {
        // if conflict might happen, then move from the register cache
        if (potentiallyOverwrittenParameterRegisters.containsKey(argument)) {
          var parameterRegisterCache = resolveNextStackLocation(argumentResidentRegister);
          instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, parameterRegisterCache, pushParameterX64RegisterMap.get(indexOfParameterRegister)));
        } else {
          // just resolve the location, for arguments like string constants and constants
          instructions.add(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(argumentResidentRegister), pushParameterX64RegisterMap.get(indexOfParameterRegister)));
        }
      }
      indexOfParameterRegister--;
    }
    x64Method.addAllAtIndex(x64Method.size(), instructions);

  }

  @Override
  public void emitInstruction(@NotNull FunctionCallWithResult functionCallWithResult) {
    callerSave(resolveIrValue(functionCallWithResult.getDestination()));
    orderFunctionCallArguments(functionCallWithResult);
    if (functionCallWithResult.isImported()) {
      x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl, X86RegisterMappedValue.unassigned(X64RegisterType.EAX), X86RegisterMappedValue.unassigned(X64RegisterType.EAX)));
      x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq, new X64CallOperand(functionCallWithResult)))
               .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(X64RegisterType.RAX), resolveIrValue(functionCallWithResult.getDestination())));
    } else {
      x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq, new X64CallOperand(functionCallWithResult)))
               .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(X64RegisterType.RAX), resolveIrValue(functionCallWithResult.getDestination())));
    }
    restoreStack(functionCallWithResult);
    callerRestore(resolveIrValue(functionCallWithResult.getDestination()));
    asmWriterContext.setMaxStackSpaceForArgs(functionCallWithResult);
  }

  private void restoreStack(@NotNull FunctionCall functionCall) {
    if (functionCall.getNumArguments() > N_ARG_REGISTERS) {
      long numStackArgs = functionCall.getNumArguments() - N_ARG_REGISTERS;
      x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.addq, new X86ConstantValue(new IrIntegerConstant((long) roundUp16((int) (numStackArgs * WORD_SIZE)), Type.Int)), X86RegisterMappedValue.unassigned(X64RegisterType.RSP)));
    }
  }

  @Override
  public void emitInstruction(@NotNull Method method) {
    asmWriterContext.setLastComparisonOperator(null);
    currentMethod = method;

    if (!asmWriterContext.isTextLabelAdded()) {
      x64Method.addLine(new X64MetaData(".text"));
      asmWriterContext.setTextLabelAdded();
    }

    if (method.isMain())
      x64Method.addLine(new X64MetaData(".global _main")).addLine(new X64MetaData(".p2align  4, 0x90"));

    if (method.isMain()) {
      x64Method.addLine(new X64MetaData("_main:"));
    } else {
      x64Method.addLine(new X64MetaData(method.methodName() + ":"));
    }

    asmWriterContext.setLocationOfSubqInst(x64Method.size());
    saveMethodArgsToLocations(method);
  }

  @Override
  public void emitInstruction(@NotNull ConditionalBranch conditionalBranch) {
    var resolvedCondition = resolveIrValue(conditionalBranch.getCondition());
    if (asmWriterContext.getLastComparisonOperator() == null) {
      if (conditionalBranch.getCondition() instanceof IrIntegerConstant) {
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolvedCondition, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq, new X86ConstantValue(IrIntegerConstant.zero()), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.je, new X64JumpTargetOperand(conditionalBranch.getTarget())));
      } else {
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq, new X86ConstantValue(IrIntegerConstant.zero()), resolvedCondition))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.je, new X64JumpTargetOperand(conditionalBranch.getTarget())));
      }
      return;
    } else {
      x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.getCorrectJumpIfFalseInstruction(asmWriterContext.getLastComparisonOperator()), new X64JumpTargetOperand(conditionalBranch.getTarget())));
    }
    asmWriterContext.setLastComparisonOperator(null);
  }

  @Override
  public void emitInstruction(@NotNull FunctionCallNoResult functionCallNoResult) {
    callerSave(null);
    orderFunctionCallArguments(functionCallNoResult);
    if (functionCallNoResult.isImported())
      x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl, X86RegisterMappedValue.unassigned(X64RegisterType.EAX), X86RegisterMappedValue.unassigned(X64RegisterType.EAX)))
               .addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq, new X64CallOperand(functionCallNoResult)));
    else
      x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.callq, new X64CallOperand(functionCallNoResult)));
    restoreStack(functionCallNoResult);
    callerRestore(null);
  }

  @Override
  public void emitInstruction(@NotNull MethodEnd methodEnd) {
    var space = new X86ConstantValue(new IrIntegerConstant((long) roundUp16(-x64ValueResolver.getCurrentStackOffset()), Type.Int));

    x64Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(), new X64BinaryInstruction(X64BinaryInstructionType.subq, space, X86RegisterMappedValue.unassigned(X64RegisterType.RSP)));
    calleeSave();
    x64Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(), new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(X64RegisterType.RSP), X86RegisterMappedValue.unassigned(X64RegisterType.RBP)));
    x64Method.addAtIndex(asmWriterContext.getLocationOfSubqInst(), new X64UnaryInstruction(X64UnaryInstructionType.pushq, X86RegisterMappedValue.unassigned(X64RegisterType.RBP)));
    x64Method = (methodEnd.isMain() ? x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorl, X86RegisterMappedValue.unassigned(X64RegisterType.EAX), X86RegisterMappedValue.unassigned(X64RegisterType.EAX))): x64Method);

    x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.addq, space, X86RegisterMappedValue.unassigned(X64RegisterType.RSP)));

    calleeRestore();
    x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(X64RegisterType.RBP), X86RegisterMappedValue.unassigned(X64RegisterType.RSP)))
             .addLine(new X64UnaryInstruction(X64UnaryInstructionType.popq, X86RegisterMappedValue.unassigned(X64RegisterType.RBP)));
    x64Method.addLine(new X64NoOperandInstruction(X64NopInstructionType.retq));
  }

  @Override
  public void emitInstruction(@NotNull ReturnInstruction returnInstruction) {
    if (returnInstruction.getReturnAddress().isPresent())
      x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(returnInstruction.getReturnAddress()
                                                                                                                .get()), X86RegisterMappedValue.unassigned(X64RegisterType.RAX)));
  }

  @Override
  public void emitInstruction(@NotNull UnaryInstruction unaryInstruction) {
    switch (unaryInstruction.operator) {
      case Operators.NOT -> {
        asmWriterContext.setLastComparisonOperator(null);
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(unaryInstruction.operand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), resolveIrValue(unaryInstruction.getDestination())))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.xorq, new X86ConstantValue(IrIntegerConstant.one()), resolveIrValue(unaryInstruction.getDestination())));
      }
      case Operators.MINUS ->
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(unaryInstruction.operand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64UnaryInstruction(X64UnaryInstructionType.neg, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), resolveIrValue(unaryInstruction.getDestination())));
      default -> throw new IllegalStateException(unaryInstruction.toString());
    }
  }

  @Override
  public void emitInstruction(@NotNull UnconditionalBranch unconditionalBranch) {
    x64Method.addLine(new X64UnaryInstruction(X64UnaryInstructionType.jmp, new X64JumpTargetOperand(unconditionalBranch.getTarget())));
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
      if (resolveIrValue(copyInstruction.getValue()) instanceof X86RegisterMappedValue || resolveIrValue(copyInstruction.getValue()) instanceof X86ConstantValue)
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(copyInstruction.getValue()), resolveIrValue(copyInstruction.getDestination())));
      else
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(copyInstruction.getValue()), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), resolveIrValue(copyInstruction.getDestination())));
    }
  }

  @Override
  public void emitInstruction(@NotNull GetAddress getAddress) {
  }

  @Override
  public void emitInstruction(@NotNull BinaryInstruction binaryInstruction) {
    switch (binaryInstruction.operator) {
      case Operators.PLUS, Operators.MINUS, Operators.MULTIPLY, Operators.CONDITIONAL_OR, Operators.CONDITIONAL_AND ->
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(binaryInstruction.fstOperand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.getX64BinaryInstruction(binaryInstruction.operator), resolveIrValue(binaryInstruction.sndOperand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), resolveIrValue(binaryInstruction.getDestination())));
      case Operators.DIVIDE, Operators.MOD -> {
        // If we are planning to use RDX, we spill it first
        if (!resolveIrValue(binaryInstruction.getDestination()).toString().equals(X64RegisterType.RDX.toString()))
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(X64RegisterType.RDX), resolveNextStackLocation(X64RegisterType.RDX)));
        if (binaryInstruction.sndOperand instanceof IrIntegerConstant) {
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(binaryInstruction.fstOperand), X86RegisterMappedValue.unassigned(X64RegisterType.RAX)))
                   .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(binaryInstruction.sndOperand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(X64UnaryInstructionType.idivq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)));
        } else {
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(binaryInstruction.fstOperand), X86RegisterMappedValue.unassigned(X64RegisterType.RAX)))
                   .addLine(new X64NoOperandInstruction(X64NopInstructionType.cqto))
                   .addLine(new X64UnaryInstruction(X64UnaryInstructionType.idivq, resolveIrValue(binaryInstruction.sndOperand)));
        }
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(binaryInstruction.operator.equals("%") ? X64RegisterType.RDX: X64RegisterType.RAX), resolveIrValue(binaryInstruction.getDestination())));
        // restore RDX
        if (!resolveIrValue(binaryInstruction.getDestination()).toString().equals(X64RegisterType.RDX.toString()))
          x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveNextStackLocation(X64RegisterType.RDX), X86RegisterMappedValue.unassigned(X64RegisterType.RDX)));
      }
      // comparison operators
      case Operators.EQ, Operators.NEQ, Operators.LT, Operators.GT, Operators.LEQ, Operators.GEQ -> {
        asmWriterContext.setLastComparisonOperator(binaryInstruction.operator);
        x64Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, resolveIrValue(binaryInstruction.fstOperand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.cmpq, resolveIrValue(binaryInstruction.sndOperand), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64UnaryInstruction(X64UnaryInstructionType.getCorrectComparisonSetInstruction(binaryInstruction.operator), X86RegisterMappedValue.unassigned(X64RegisterType.al)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movzbq, X86RegisterMappedValue.unassigned(X64RegisterType.al), X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER)))
                 .addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq, X86RegisterMappedValue.unassigned(COPY_TEMP_REGISTER), resolveIrValue(binaryInstruction.getDestination())));
      }
      default -> throw new IllegalStateException(binaryInstruction.toString());
    }
  }
}

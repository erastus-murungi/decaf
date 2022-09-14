package edu.mit.compilers.asm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.Math.min;
import static edu.mit.compilers.utils.Utils.WORD_SIZE;
import static edu.mit.compilers.utils.Utils.roundUp16;

import com.google.common.collect.Sets;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import edu.mit.compilers.asm.instructions.X64BinaryInstruction;
import edu.mit.compilers.asm.operands.X86MemoryAddressValue;
import edu.mit.compilers.asm.operands.X86ConstantValue;
import edu.mit.compilers.asm.operands.X86GlobalValue;
import edu.mit.compilers.asm.operands.X86RegisterMappedValue;
import edu.mit.compilers.asm.operands.X86StackMappedValue;
import edu.mit.compilers.asm.operands.X86Value;
import edu.mit.compilers.asm.types.X64BinaryInstructionType;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.StringConstantAllocation;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrConstant;
import edu.mit.compilers.codegen.names.IrGlobal;
import edu.mit.compilers.codegen.names.IrGlobalArray;
import edu.mit.compilers.codegen.names.IrGlobalScalar;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.codegen.names.IrStackArray;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.registerallocation.RegisterAllocator;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

/**
 * Resolve names
 */
public class X86ValueResolver {
  @NotNull
  private final Map<Method, Map<IrValue, X86RegisterMappedValue>> registerMappedIrValues = new HashMap<>();
  @NotNull
  private final Map<IrConstant, X86ConstantValue> constants = new HashMap<>();
  @NotNull
  private final Map<IrGlobalScalar, X86GlobalValue> globalScalars = new HashMap<>();
  @NotNull
  private final Map<Method, Map<IrValue, Integer>> stackOffsets = new HashMap<>();
  @NotNull
  private final Map<X64RegisterType, Integer> temporarySaveLocations = new HashMap<>();
  @NotNull
  private final Map<Method, Integer> largestStackOffset = new HashMap<>();
  @NotNull
  private final Map<Method, Map<IrAssignableValue, X86Value>> initialArgumentLocations = new HashMap<>();
  @NotNull
  private final RegisterAllocator registerAllocator;
  @NotNull
  private Method currentMethod;
  @NotNull
  private X86Method currentX86Method;
  private int currentStackOffset = 0;


  public X86ValueResolver(
      @NotNull ProgramIr programIr,
      @NotNull RegisterAllocator registerAllocator
  ) {
    currentMethod = programIr.getMethods()
                             .get(0);
    currentX86Method = new X86Method();
    this.registerAllocator = registerAllocator;
    mapGlobalsToX64Operands(programIr);
    mapLocalsToX64Operand(programIr,
        registerAllocator);
  }

  public boolean parameterUsedInCurrentMethod(@NotNull IrValue irValue) {
    return registerAllocator.getVariableToRegisterMap()
                            .get(currentMethod)
                            .containsKey(irValue);
  }

  public void setCurrentMethod(@NotNull Method currentMethod) {
    this.currentMethod = currentMethod;
  }

  public void setCurrentX64Method(@NotNull X86Method currentX86Method) {
    this.currentX86Method = currentX86Method;
  }

  private void mapLocalsToX64Operand(
      ProgramIr programIr,
      RegisterAllocator registerAllocator
  ) {
    for (var method : programIr.getMethods()) {
      prepareForMethod(method,
          currentX86Method);
      mapParametersToLocations(method,
          registerAllocator);
      mapLocalToX64OperandsForMethod(method,
          registerAllocator);
    }
  }

  private void mapLocalToX64OperandsForMethod(
      @NotNull Method method,
      @NotNull RegisterAllocator registerAllocator
  ) {
    var methodRegisterMapping = registerAllocator.getVariableToRegisterMap()
                                                 .get(method);
    var liveIntervals = registerAllocator.liveIntervalsUtil.getLiveIntervals(method);
    liveIntervals.forEach(liveInterval -> {
      var irAssignableValue = liveInterval.irAssignableValue();
      if (irAssignableValue instanceof IrRegister || irAssignableValue instanceof IrGlobalArray) {
        var dest = methodRegisterMapping.get(irAssignableValue);
        if (dest != null && !dest.equals(X64RegisterType.STACK)) {
          registerMappedIrValues.get(method)
                                .put(irAssignableValue,
                                    new X86RegisterMappedValue(dest,
                                        irAssignableValue));
        } else {
          var alreadyInDestination = initialArgumentLocations.get(method)
                                                             .get(irAssignableValue);
          if (alreadyInDestination instanceof X86StackMappedValue x86StackMappedValue) {
            stackOffsets.get(method)
                        .put(irAssignableValue,
                            x86StackMappedValue.getOffset());
          } else {
            stackOffsets.get(method)
                        .put(irAssignableValue,
                            pushStack());
          }
        }
      } else if (irAssignableValue instanceof IrMemoryAddress || irAssignableValue instanceof IrGlobalScalar) {
      } else {
        throw new IllegalStateException(irAssignableValue.getClass()
                                                         .getName() + " " + irAssignableValue);
      }
    });
    mapIrStackArraysToStack(method);
    largestStackOffset.put(method,
        currentStackOffset);
  }

  private void mapIrStackArraysToStack(@NotNull Method method) {
    ProgramIr.getIrStackArrays(method)
             .forEach(irStackArray -> {
               int offset = -roundUp16(Math.abs(pushStack((int) irStackArray.getNumElements())));
               stackOffsets.get(method)
                           .put(irStackArray,
                               offset);
               currentStackOffset = offset;
             });
  }

  @NotNull
  private Map<IrAssignableValue, X86Value> getInitialArgumentLocations() {
    return initialArgumentLocations.get(currentMethod);
  }

  @NotNull
  private X86ConstantValue resolveIrConstantValue(@NotNull IrConstant irConstant) {
    return checkNotNull(constants.computeIfAbsent(irConstant,
        k -> new X86ConstantValue(irConstant)));
  }

  boolean isUnResolvedArgumentIrValue(@NotNull IrValue irValue) {
    return irValue instanceof IrAssignableValue argumentIrRegister &&
        getInitialArgumentLocations().containsKey(argumentIrRegister);
  }

  @NotNull
  private X86Value localizeArgument(@NotNull IrAssignableValue argument) {
    var initialArgumentLocation = getInitialArgumentLocations().remove(argument);
    var localizedArgumentX86Value = resolveIrValue(argument);
    currentX86Method.add(new X64BinaryInstruction(X64BinaryInstructionType.movq,
        initialArgumentLocation,
        localizedArgumentX86Value));
    return localizedArgumentX86Value;
  }

  private void spillValuesToStack(
      @NotNull X64RegisterType x64RegisterType,
      @NotNull Collection<IrAssignableValue> valuesToSpill
  ) {
    valuesToSpill.forEach(irAssignableValue -> {
      var spillLocation = new X86StackMappedValue(X64RegisterType.RBP,
          pushStack());
      currentX86Method.add(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          resolveIrValue(irAssignableValue),
          spillLocation));
      checkState(registerMappedIrValues.get(currentMethod)
                                       .remove(irAssignableValue)
                                       .getX64RegisterType()
                                       .equals(x64RegisterType));
      checkState(!stackOffsets.get(currentMethod)
                              .containsKey(irAssignableValue));
      stackOffsets.get(currentMethod)
                  .put(irAssignableValue,
                      spillLocation.getOffset());
    });
  }


  private void updateIrValueMappingTo(
      @NotNull IrValue irValue,
      @NotNull X64RegisterType x64RegisterType
  ) {
    var oldLocation = resolveIrValue(irValue);
    var newLocation = new X86RegisterMappedValue(x64RegisterType,
        irValue);
    currentX86Method.add(new X64BinaryInstruction(X64BinaryInstructionType.movq,
        oldLocation,
        newLocation));
    registerMappedIrValues.get(currentMethod)
                          .remove(irValue);
    stackOffsets.get(currentMethod)
                .remove(irValue);
    registerMappedIrValues.get(currentMethod)
                          .put(irValue,
                              newLocation);
  }


  private X64RegisterType genRegisterToSpill(
      @NotNull IrValue irValue,
      ArrayList<X64RegisterType> toAvoid
  ) {
    final var liveValuesInInterval = irValue instanceof IrIntegerConstant ? Collections.<IrAssignableValue>emptySet(): Utils.getLAllRegisterAllocatableValuesInInstructionList(registerAllocator.liveIntervalsUtil.getAllInstructionsInLiveIntervalOf(irValue,
        currentMethod));
    final var m = new HashMap<X64RegisterType, Set<IrAssignableValue>>();
    for (IrAssignableValue irAssignableValue : liveValuesInInterval) {
      var location = registerMappedIrValues.get(currentMethod)
                                           .get(irAssignableValue);
      if (location != null) m.computeIfAbsent(location.getX64RegisterType(),
                                 k -> new HashSet<>())
                             .add(irAssignableValue);
    }
    for (var register : X64RegisterType.regsToAllocate) {
      if (!m.containsKey(register)) {
        m.put(register,
            new HashSet<>());
      }
    }
    toAvoid.add(X64RegisterType.STACK);
    // get the one with the
    var fewestReferences = 10000;
    var reg = X64RegisterType.STACK;
    for (var register : m.keySet()) {
      if (toAvoid.contains(register)) continue;
      if (m.get(register)
           .size() < fewestReferences) {
        fewestReferences = m.get(register)
                            .size();
        reg = register;
      }
    }
    checkState(reg != X64RegisterType.STACK);
    spillValuesToStack(reg,
        m.get(reg));
    updateIrValueMappingTo(irValue,
        reg);
    return reg;
  }


  private X86MemoryAddressValue resolveIrMemoryAddress(@NotNull IrMemoryAddress irMemoryAddress) {
    X86RegisterMappedValue baseRegister, indexRegister = null;
    // get base index
    var indexValue = resolveIrValue(irMemoryAddress.getIndex());
    if (indexValue instanceof X86StackMappedValue || indexValue instanceof X86MemoryAddressValue || irMemoryAddress.getBaseAddress() instanceof IrGlobal) {
      // we should spill
      indexRegister = new X86RegisterMappedValue(genRegisterToSpill(irMemoryAddress.getIndex(),
          new ArrayList<>()),
          irMemoryAddress.getIndex());
      currentX86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          indexValue,
          indexRegister));
      indexValue = indexRegister;
    }
    if (irMemoryAddress.getBaseAddress() instanceof IrStackArray irStackArray) {
      var baseValue = resolveStackMappedIrValue(irStackArray);
      return new X86MemoryAddressValue(baseValue,
          indexValue);
    }
    var baseValue = resolveIrValue(irMemoryAddress.getBaseAddress());
    if (baseValue instanceof X86StackMappedValue) {
      // we should spill and avoid the index register
      baseRegister = new X86RegisterMappedValue(genRegisterToSpill(irMemoryAddress.getBaseAddress(),
          new ArrayList<>(indexRegister == null ? Collections.emptyList() : List.of(indexRegister.getX64RegisterType()))),
          irMemoryAddress.getBaseAddress());
    } else {
      baseRegister = (X86RegisterMappedValue) baseValue;
    }
    if (irMemoryAddress.getBaseAddress() instanceof IrGlobalArray irGlobalArray) {
      currentX86Method.addLine(new X64BinaryInstruction(X64BinaryInstructionType.movq,
          new X86GlobalValue(irGlobalArray),
          baseRegister));
    }
    return new X86MemoryAddressValue(baseRegister,
        indexValue);
  }

  private boolean isStackMappedIrValue(@NotNull IrValue irValue) {
    return irValue instanceof IrAssignableValue && stackOffsets.get(currentMethod)
                                                               .containsKey(irValue);
  }

  @NotNull
  private X86StackMappedValue resolveStackMappedIrValue(@NotNull IrValue irValue) {
    return new X86StackMappedValue(X64RegisterType.RBP,
        checkNotNull(stackOffsets.get(currentMethod)
                                 .get(irValue)),
        irValue);
  }

  @NotNull
  public X86Value resolveIrValue(@NotNull IrValue irValue) {
    if (isUnResolvedArgumentIrValue(irValue)) {
      return localizeArgument((IrAssignableValue) irValue);
    }
    if (irValue instanceof IrConstant irConstant) {
      return resolveIrConstantValue(irConstant);
    }
    if (irValue instanceof IrMemoryAddress irMemoryAddress) {
      return resolveIrMemoryAddress(irMemoryAddress);
    }
    if (irValue instanceof IrGlobalScalar irGlobalScalar) {
      return resolveIrGlobalScalar(irGlobalScalar);
    }
    if (isStackMappedIrValue(irValue)) {
      return resolveStackMappedIrValue(irValue);
    } else {
      var irAssignableValue = (IrAssignableValue) irValue;
      return resolveRegisterMappedIrValue(irAssignableValue);
    }
  }

  @NotNull
  private X86RegisterMappedValue resolveRegisterMappedIrValue(@NotNull IrAssignableValue irAssignableValue) {
    return registerMappedIrValues.get(currentMethod)
                                 .get(irAssignableValue);
  }

  @NotNull
  private X86Value resolveIrGlobalScalar(@NotNull IrGlobalScalar irGlobalScalar) {
    return globalScalars.get(irGlobalScalar);
  }

  private void mapGlobalsToX64Operands(@NotNull ProgramIr programIr) {
    for (Instruction instruction : programIr.getPrologue()) {
      if (instruction instanceof GlobalAllocation globalAllocation) {
        if (globalAllocation.getValue() instanceof IrGlobalScalar irGlobalScalar) {
          globalScalars.put(irGlobalScalar,
              new X86GlobalValue(globalAllocation.getValue()));
        }
      } else if (instruction instanceof StringConstantAllocation stringConstantAllocation) {
        var stringConstant = stringConstantAllocation.getStringConstant();
        constants.put(stringConstant,
            new X86ConstantValue(stringConstant));
      } else {
        throw new IllegalStateException(instruction.toString());
      }
    }
  }

  private int pushStack() {
    currentStackOffset = currentStackOffset - WORD_SIZE;
    return currentStackOffset;
  }

  private int pushStack(int n) {
    currentStackOffset = currentStackOffset - (WORD_SIZE * n);
    return currentStackOffset;
  }

  private void mapParametersToLocations(
      @NotNull Method method,
      @NotNull RegisterAllocator registerAllocator
  ) {
    // store all method args in unused registers and stack space if needed
    // the only registers we will ignore are the ones used to store the method args

    var unusedRegisters = getUnusedRegisters(currentMethod,
        registerAllocator);

    var destinations = new HashMap<IrAssignableValue, X86Value>();

    // next we save all the arguments to their corresponding locations
    for (int parameterIndex = 0; parameterIndex < method.getParameterNames()
                                                        .size(); parameterIndex++) {
      var parameter = method.getParameterNames()
                            .get(parameterIndex);

      if (parameterIndex < unusedRegisters.size()) {
        destinations.put(parameter,
            new X86RegisterMappedValue(unusedRegisters.get(parameterIndex),
                parameter));
      } else {
        destinations.put(parameter,
            new X86StackMappedValue(X64RegisterType.RBP,
                pushStack(),
                parameter));
      }
    }
    largestStackOffset.put(method,
        currentStackOffset);
    initialArgumentLocations.put(method,
        destinations);
  }

  private List<X64RegisterType> getUnusedRegisters(
      @NotNull Method method,
      @NotNull RegisterAllocator registerAllocator
  ) {
    var valueMappedRegisters = Set.copyOf(registerAllocator.getVariableToRegisterMap()
                                                           .get(method)
                                                           .values());
    var nUsedArgRegisters = min(6,
        method.getParameterNames()
              .size());
    var usedArgRegisters = Set.copyOf(X64RegisterType.parameterRegisters.subList(0,
        nUsedArgRegisters));
    return List.copyOf(Sets.difference(Set.copyOf(X64RegisterType.regsToAllocate),
        Sets.union(usedArgRegisters,
            valueMappedRegisters)));
  }

  public void prepareForMethod(
      @NotNull Method method,
      @NotNull X86Method x86Method
  ) {
    setCurrentMethod(method);
    setCurrentX64Method(x86Method);
    currentStackOffset = largestStackOffset.getOrDefault(method,
        0);
    stackOffsets.computeIfAbsent(method,
        k -> new HashMap<>());
    registerMappedIrValues.computeIfAbsent(method,
        k -> new HashMap<>());
  }

  public X86Value resolveNextStackLocation(@NotNull X64RegisterType registerType) {
    if (temporarySaveLocations.containsKey(registerType)) return new X86StackMappedValue(X64RegisterType.RBP,
        temporarySaveLocations.get(registerType));
    else {
      var newLocation = pushStack();
      temporarySaveLocations.put(registerType,
          newLocation);
      return new X86StackMappedValue(X64RegisterType.RBP,
          newLocation);
    }
  }

  @NotNull
  public X86Value resolveInitialArgumentLocation(@NotNull IrRegister irRegister) {
    return initialArgumentLocations.get(currentMethod)
                                   .get(irRegister);
  }

  public int getCurrentStackOffset() {
    return currentStackOffset;
  }
}

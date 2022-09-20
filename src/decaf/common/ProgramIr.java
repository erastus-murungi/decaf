package decaf.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.codegen.names.IrStackArray;
import decaf.codegen.names.IrMemoryAddress;
import decaf.ast.MethodDefinition;
import decaf.ast.MethodDefinitionParameter;
import decaf.codegen.InstructionList;
import decaf.codegen.IndexManager;
import decaf.codegen.TraceScheduler;
import decaf.codegen.codes.GlobalAllocation;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.names.IrSsaRegister;
import decaf.codegen.names.IrValue;

public class ProgramIr {
  @NotNull
  private final InstructionList prologue;
  @NotNull
  private List<Method> methodList;
  @NotNull
  private Set<IrValue> globals = new HashSet<>();

  public ProgramIr(
      @NotNull InstructionList prologue,
      @NotNull List<Method> methodList
  ) {
    this.prologue = prologue;
    this.methodList = methodList;
  }

  public static List<IrSsaRegister> getNonParamLocals(
      Method method,
      Set<IrValue> globals
  ) {
    return getLocals(
        method,
        globals
    ).stream()
     .filter(virtualRegister -> !method.getParameterNames()
                                       .contains(virtualRegister))
     .toList();
  }

  public static List<IrSsaRegister> getLocals(
      Method method,
      Set<IrValue> globals
  ) {
    Set<IrValue> uniqueNames = new HashSet<>();
    var flattened = TraceScheduler.flattenIr(method);

    for (Instruction instruction : flattened) {
      for (IrValue name : instruction.genIrValues()) {
        if (name instanceof IrMemoryAddress && !globals.contains(name)) {
          uniqueNames.add(name);
        }
      }
    }

    for (Instruction instruction : flattened) {
      for (var name : instruction.genIrValues()) {
        if (!(name instanceof IrMemoryAddress) && !globals.contains(name)) {
          uniqueNames.add(name);
        }
      }
    }
    var locals = uniqueNames.stream()
                            .filter((name -> ((name instanceof IrSsaRegister))))
                            .map(name -> (IrSsaRegister) name)
                            .distinct()
                            .sorted(Comparator.comparing(Object::toString))
                            .collect(Collectors.toList());
    reorderLocals(
        locals,
        method.getMethodDefinition()
    );
    return locals;
  }

  private static void reorderLocals(
      List<IrSsaRegister> locals,
      MethodDefinition methodDefinition
  ) {
    List<IrSsaRegister> methodParametersNames = new ArrayList<>();

    Set<String> methodParameters = methodDefinition.parameterList.stream()
                                                                 .map(MethodDefinitionParameter::getName)
                                                                 .collect(Collectors.toSet());

    List<IrSsaRegister> methodParamNamesList = new ArrayList<>();
    for (var name : locals) {
      if (methodParameters.contains(name.toString())) {
        methodParamNamesList.add(name);
      }
    }
    for (var local : locals) {
      if (methodParameters.contains(local.toString())) {
        methodParametersNames.add(local);
      }
    }
    locals.removeAll(methodParametersNames);
    locals.addAll(
        0,
        methodParamNamesList.stream()
                            .sorted(Comparator.comparing(IrValue::toString))
                            .toList()
    );
  }

  public static String mergeMethod(Method method) {
    List<String> output = new ArrayList<>();
    for (InstructionList instructionList : TraceScheduler.getInstructionTrace(method)) {
      if (!instructionList.getLabel()
                          .equals("UNSET")) output.add("    " + instructionList.getLabel() + ":");
      instructionList.forEach(instruction -> output.add(instruction.syntaxHighlightedToString()));
    }
    return String.join(
        "\n",
        output
    );
  }

  public static List<IrStackArray> getIrStackArrays(Method method) {
    return TraceScheduler.flattenIr(method)
                         .stream()
                         .flatMap(instruction -> instruction.genIrValuesFiltered(IrStackArray.class)
                                                            .stream())
                         .distinct()
                         .toList();
  }

  public List<Method> getMethods() {
    return methodList;
  }

  public void setMethods(List<Method> methodList) {
    this.methodList = methodList;
  }

  public @NotNull InstructionList getPrologue() {
    return prologue;
  }

  public List<Instruction> toSingleInstructionList() {
    var programHeader = prologue.copy();
    var instructions = programHeader.copy();
    for (Method method : methodList) {
      for (InstructionList instructionList : TraceScheduler.getInstructionTrace(method))
        instructions.addAll(instructionList);
    }
    return instructions;
  }

  public String mergeProgram() {
    List<String> output = new ArrayList<>();
    for (var instruction : prologue) {
      output.add(instruction.syntaxHighlightedToString());
    }
    for (Method method : methodList) {
      for (InstructionList instructionList : TraceScheduler.getInstructionTrace(method)) {
        if (!instructionList.getLabel()
                            .equals("UNSET")) output.add("    " + instructionList.getLabel() + ":");
        instructionList.forEach(instruction -> output.add(instruction.syntaxHighlightedToString()));
      }
    }
    return String.join(
        "\n",
        output
    );
  }

  public void findGlobals() {
    globals = prologue.stream()
                      .filter(instruction -> instruction instanceof GlobalAllocation)
                      .map(instruction -> (GlobalAllocation) instruction)
                      .map(GlobalAllocation::getValue)
                      .collect(Collectors.toUnmodifiableSet());
  }

  public void renumberLabels() {
    IndexManager.resetLabels();
    methodList.forEach(method -> TraceScheduler.getInstructionTrace(method)
                                               .forEach(instructionList -> {
                                                 if (!instructionList.isEntry())
                                                   instructionList.setLabel(IndexManager.genLabelIndex());
                                               }));
  }

  public Set<IrValue> getGlobals() {
    return Set.copyOf(globals);
  }

  public void setGlobals(Set<IrValue> globals) {
    this.globals = Set.copyOf(globals);
  }

  public List<IrSsaRegister> getLocals(Method method) {
    return getLocals(
        method,
        globals
    );
  }
}

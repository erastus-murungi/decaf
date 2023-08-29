package decaf.shared;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.FormalArgument;
import decaf.ir.IndexManager;
import decaf.ir.InstructionList;
import decaf.ir.TraceScheduler;
import decaf.ir.names.IrRegister;
import decaf.ir.names.IrStackArray;
import decaf.ir.names.IrValue;

public class ProgramIr {

  private final InstructionList prologue;

  private List<Method> methods;

  private Set<IrValue> globals = new HashSet<>();

  public ProgramIr(
      InstructionList prologue,
      List<Method> methods
  ) {
    this.prologue = prologue;
    this.methods = methods;
  }


  public static List<IrRegister> getNonParamLocals(
      Method method
  ) {
    return getLocals(method).stream()
                            .filter(virtualRegister -> !method.getParameterNames()
                                                              .contains(virtualRegister))
                            .toList();
  }


  public static List<IrRegister> getLocals(
      Method method
  ) {
    var locals = TraceScheduler.flattenIr(method)
                               .stream()
                               .flatMap(instruction -> instruction.genIrValues()
                                                                  .stream())
                               .filter(irValue -> irValue instanceof IrRegister)
                               .map(irValue -> (IrRegister) irValue)
                               .distinct()
                               .toList();
    reorderLocals(
        locals,
        method.getMethodDefinition()
    );
    return locals;
  }

  private static void reorderLocals(
      List<IrRegister> locals,
      MethodDefinition methodDefinition
  ) {
    var parameters = new ArrayList<IrRegister>();

    var parametersFromAst = methodDefinition.getFormalArguments()
                                            .stream()
                                            .map(FormalArgument::getName)
                                            .collect(Collectors.toSet());

    var methodParamNamesList = new ArrayList<IrRegister>();
    for (var name : locals) {
      if (parametersFromAst.contains(name.toString())) {
        methodParamNamesList.add(name);
      }
    }
    for (var local : locals) {
      if (parametersFromAst.contains(local.toString())) {
        parameters.add(local);
      }
    }
    locals = new ArrayList<>(locals);
    locals.removeAll(parameters);
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
    return methods;
  }

  public void setMethods(List<Method> methods) {
    this.methods = methods;
  }

  public InstructionList getPrologue() {
    return prologue;
  }

  public List<Instruction> toSingleInstructionList() {
    var programHeader = prologue.copy();
    var instructions = programHeader.copy();
    for (Method method : methods) {
      for (InstructionList instructionList : TraceScheduler.getInstructionTrace(method))
        instructions.addAll(instructionList);
    }
    return instructions;
  }

  public String mergeProgram() {
    var output = new ArrayList<String>();
    for (var instruction : prologue) {
      output.add(instruction.syntaxHighlightedToString());
    }
    for (Method method : methods) {
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
    methods.forEach(method -> TraceScheduler.getInstructionTrace(method)
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
}

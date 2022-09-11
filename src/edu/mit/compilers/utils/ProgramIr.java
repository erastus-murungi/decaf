package edu.mit.compilers.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.Value;

public class ProgramIr {
    private final InstructionList prologue;
    private List<Method> methodList;
    private Set<LValue> globals = new HashSet<>();

    public ProgramIr(InstructionList prologue, List<Method> methodList) {
        this.prologue = prologue;
        this.methodList = methodList;
    }

    public List<Method> getMethods() {
        return methodList;
    }

    public void setMethods(List<Method> methodList) {
        this.methodList = methodList;
    }

    public static List<LValue> getLocals(Method method, Set<LValue> globals) {
        Set<Value> uniqueNames = new HashSet<>();
        var flattened = TraceScheduler.flattenIr(method);

        for (Instruction instruction : flattened) {
            for (Value name : instruction.getAllValues()) {
                if (name instanceof MemoryAddress && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }

        for (Instruction instruction : flattened) {
            for (var name : instruction.getAllValues()) {
                if (!(name instanceof MemoryAddress) && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }
        var locals = uniqueNames
                .stream()
                .filter((name -> ((name instanceof LValue))))
                .map(name -> (LValue) name)
                .distinct()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());
        reorderLocals(locals, method.methodDefinition);
        return locals;
    }

    private static void reorderLocals(List<LValue> locals, MethodDefinition methodDefinition) {
        List<LValue> methodParametersNames = new ArrayList<>();

        Set<String> methodParameters = methodDefinition.parameterList
                .stream()
                .map(MethodDefinitionParameter::getName)
                .collect(Collectors.toSet());

        List<LValue> methodParamNamesList = new ArrayList<>();
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
        locals.addAll(0, methodParamNamesList
                .stream()
                .sorted(Comparator.comparing(Value::toString))
                .toList());
    }

    public static String mergeMethod(Method method) {
        List<String> output = new ArrayList<>();
        for (InstructionList instructionList : TraceScheduler.getInstructionTrace(method)) {
            if (!instructionList.getLabel()
                    .equals("UNSET"))
                output.add("    " + instructionList.getLabel() + ":");
            instructionList.forEach(instruction -> output.add(instruction.syntaxHighlightedToString()));
        }
        return String.join("\n", output);
    }

    public InstructionList getPrologue() {
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
                        .equals("UNSET"))
                    output.add("    " + instructionList.getLabel() + ":");
                instructionList.forEach(instruction -> output.add(instruction.syntaxHighlightedToString()));
            }
        }
        return String.join("\n", output);
    }

    public void findGlobals() {
        globals = prologue
                .stream()
                .filter(instruction -> instruction instanceof GlobalAllocation)
                .map(instruction -> (GlobalAllocation) instruction)
                .map(GlobalAllocation::getValue)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void renumberLabels() {
        TemporaryNameIndexGenerator.resetLabels();
        methodList.forEach(
                method ->
                        TraceScheduler.getInstructionTrace(method)
                                .forEach(
                                        instructionList -> {
                                            if (!instructionList.isEntry())
                                                instructionList.setLabel(TemporaryNameIndexGenerator.getNextLabel());
                                        }
                                )
        );
    }

    public Set<LValue> getGlobals() {
        return Set.copyOf(globals);
    }

    public void setGlobals(Set<LValue> globals) {
        this.globals = Set.copyOf(globals);
    }

    public List<LValue> getLocals(Method method) {
        return getLocals(method, globals);
    }
}

package edu.mit.compilers.utils;

import org.jetbrains.annotations.NotNull;

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
import edu.mit.compilers.codegen.names.GlobalAddress;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.Value;

public class ProgramIr {
    @NotNull
    private final InstructionList prologue;
    @NotNull
    private List<Method> methodList;
    @NotNull
    private Set<GlobalAddress> globals = new HashSet<>();

    public ProgramIr(@NotNull InstructionList prologue, @NotNull List<Method> methodList) {
        this.prologue = prologue;
        this.methodList = methodList;
    }

    public List<Method> getMethods() {
        return methodList;
    }

    public void setMethods(List<Method> methodList) {
        this.methodList = methodList;
    }

    public static List<VirtualRegister> getNonParamLocals(Method method, Set<GlobalAddress> globals) {
        return getLocals(method, globals)
                .stream()
                .filter(virtualRegister -> !method.getParameterNames().contains(virtualRegister))
                .toList();
    }

    public static List<VirtualRegister> getLocals(Method method, Set<GlobalAddress> globals) {
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
                .filter((name -> ((name instanceof VirtualRegister))))
                .map(name -> (VirtualRegister) name)
                .distinct()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());
        reorderLocals(locals, method.methodDefinition);
        return locals;
    }

    private static void reorderLocals(List<VirtualRegister> locals, MethodDefinition methodDefinition) {
        List<VirtualRegister> methodParametersNames = new ArrayList<>();

        Set<String> methodParameters = methodDefinition.parameterList
                .stream()
                .map(MethodDefinitionParameter::getName)
                .collect(Collectors.toSet());

        List<VirtualRegister> methodParamNamesList = new ArrayList<>();
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

    public Set<GlobalAddress> getGlobals() {
        return Set.copyOf(globals);
    }

    public void setGlobals(Set<GlobalAddress> globals) {
        this.globals = Set.copyOf(globals);
    }

    public List<VirtualRegister> getLocals(Method method) {
        return getLocals(method, globals);
    }
}

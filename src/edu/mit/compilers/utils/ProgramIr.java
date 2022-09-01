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
import edu.mit.compilers.codegen.TraceScheduler;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;

public class ProgramIr {
    public InstructionList headerInstructions;
    public List<Method> methodList;
    Set<LValue> globals = new HashSet<>();

    public ProgramIr(InstructionList headerInstructions, List<Method> methodList) {
        this.headerInstructions = headerInstructions;
        this.methodList = methodList;
    }

    public int getSizeOfHeaderInstructions() {
        return headerInstructions.size();
    }

    public InstructionList mergeProgram() {
        var programHeader = headerInstructions.copy();
        var tacList = programHeader.copy();
        for (Method method : methodList) {
            for (InstructionList instructionList : new TraceScheduler(method).getInstructionTrace())
                tacList.addAll(instructionList);
        }
        return tacList;
    }

    public void findGlobals() {
        globals = headerInstructions
                .stream()
                .filter(instruction -> instruction instanceof GlobalAllocation)
                .map(instruction -> (GlobalAllocation) instruction)
                .map(globalAllocation -> globalAllocation.variableName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<LValue> getGlobals() {
        return globals;
    }

    public List<LValue> getLocals(Method method) {
        return getLocals(method, globals);
    }

    public static List<LValue> getLocals(Method method, Set<LValue> globals) {
        Set<Value> uniqueNames = new HashSet<>();
        var flattened = TraceScheduler.flattenIr(method);

        for (Instruction instruction : flattened) {
            for (Value name : instruction.getAllNames()) {
                if (name instanceof MemoryAddress && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }

        for (Instruction instruction : flattened) {
            for (var name : instruction.getAllNames()) {
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
}

package edu.mit.compilers.asm;

import static edu.mit.compilers.asm.X64Register.N_ARG_REGISTERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.MethodReturn;
import edu.mit.compilers.codegen.codes.PopParameter;
import edu.mit.compilers.codegen.codes.PushArgument;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringLiteralStackAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.utils.Operators;

public class X64CodeConverter implements InstructionVisitor<X64Builder, X64Builder> {
    /**
     * keeps track of whether the {@code .text} label has been added or not
     */
    private boolean textAdded = false;
    /**
     * Set of global variables
     */
    private final Set<AbstractName> globals = new HashSet<>();
    /**
     * We need to keep track of the current method we are processing
     */
    private MethodBegin currentMethod;
    /**
     * Convenient constants to keep track of
     */
    private final ConstantName ZERO = new ConstantName(0L, BuiltinType.Int);
    private final ConstantName ONE = new ConstantName(1L, BuiltinType.Int);
    /**
     * We need a minimum of 2 registers to keep track of array indices,
     * because of situations like this:
     *
     * <pre> {@code
     * 	    mov    a(,%r11,8), %r10
     * 	    subq	a(,%r12,8), %r10
     * }</pre>
     */
    private final Stack<X64Register> freeIndexRegisters = new Stack<>();

    /**
     * Provides a mapping of an array to its index register
     */
    private final Map<MemoryAddressName, X64Register> memoryAddressToIndexRegister = new HashMap<>();

    /**
     * Provides a mapping of an index to its register
     */
    private final Map<AbstractName, X64Register> indexToIndexRegister = new HashMap<>();

    /**
     * The amount of stack space we are using for local variables
     */
    private long stackSpace;
    /**
     * Keeps track of the last comparison operator used
     * This is useful for evaluating conditionals
     * {@code lastComparisonOperator} will always have a value if a conditional jump evaluates
     * a variable;
     */
    public String lastComparisonOperator = null;
    /**
     * mapping of variables to registers
     * filled in the constructor
     */
    private Map<MethodBegin, Map<AbstractName, X64Register>> registerMapping = new HashMap<>();
    private Map<AbstractName, X64Register> currentMapping;
    private final Stack<Integer> subqLocs = new Stack<>();
    /**
     * A stack of push parameters
     * Useful when trying to place a caller saved register store
     */
    private final Stack<PushArgument> pushArguments = new Stack<>();

    /**
     * @implNote We choose %r10 as our temporary copy register
     * if we are trying to move a constant to a stack location, for instance
     * <pre>{@code a = 0}</pre> where {@code a} is allocated the stack location <pre>{@code -8(%rbp)}</pre>
     * We use
     * <pre> {@code
     * 	 movq $0, %r10
     *   movq %r10, -8(%rbp)
     *   }
     *  </pre>
     * <p>
     * If instead {@code a} is allocated some register, for instance register <pre>{@code %rcx}</pre>
     * We use
     * <pre>{@code
     *      movq $0, %rcx
     *   </pre>
     */
    private final X64Register COPY_TEMP_REGISTER = X64Register.R10;

    private Map<MethodBegin, Map<Instruction, Set<X64Register>>> methodToLiveRegistersInfo = new HashMap<>();

    private Map<MemoryAddressName, AbstractName> memoryAddressToArrayMap = new HashMap<>();

    private InstructionList instructionList;

    private Integer currentInstructionIndex;

    public X64CodeConverter(InstructionList instructionList, Map<MethodBegin,
            Map<AbstractName, X64Register>> abstractNameToX64RegisterMap,
                            Map<MethodBegin, Map<Instruction, Set<X64Register>>> methodToLiveRegistersInfo
    ) {
        this.instructionList = instructionList;
        this.registerMapping = abstractNameToX64RegisterMap;
        this.methodToLiveRegistersInfo = methodToLiveRegistersInfo;
        freeIndexRegisters.add(X64Register.R11);
        freeIndexRegisters.add(X64Register.R12);
    }

    public X64CodeConverter(InstructionList instructionList) {
        this(instructionList, Collections.emptyMap(), Collections.emptyMap());
    }

    private X64CodeConverter() {
    }

    private List<AbstractName> getLocals(InstructionList instructionList) {
        Set<AbstractName> uniqueNames = new HashSet<>();
        var flattened = instructionList.flatten();

        for (Instruction instruction : flattened) {
            for (AbstractName name : instruction.getAllNames()) {
                if (name instanceof MemoryAddressName && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }

        for (Instruction instruction : flattened) {
            for (AbstractName name : instruction.getAllNames()) {
                if (!(name instanceof MemoryAddressName) && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }
        return uniqueNames
                .stream()
                .filter((name -> ((name instanceof AssignableName))))
                .distinct()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());
    }


    private void reorderLocals(List<AbstractName> locals, MethodDefinition methodDefinition) {
        List<AbstractName> methodParametersNames = new ArrayList<>();

        Set<String> methodParameters = methodDefinition.methodDefinitionParameterList
                .stream()
                .map(methodDefinitionParameter -> methodDefinitionParameter.id.id)
                .collect(Collectors.toSet());

        List<AbstractName> methodParamNamesList = new ArrayList<>();
        for (AbstractName name : locals) {
            if (methodParameters.contains(name.toString())) {
                methodParamNamesList.add(name);
            }
        }
        for (AbstractName local : locals) {
            if (methodParameters.contains(local.toString())) {
                methodParametersNames.add(local);
            }
        }
        locals.removeAll(methodParametersNames);
        locals.addAll(0, methodParamNamesList
                .stream()
                .sorted(Comparator.comparing(AbstractName::toString))
                .collect(Collectors.toList()));
    }


    public X64Program convert() {
        X64Builder x64Builder = initializeDataSection();
        final X64Builder root = x64Builder;
        currentInstructionIndex = -1;
        for (Instruction instruction : instructionList) {
            currentInstructionIndex += 1;
            x64Builder = instruction.accept(this, x64Builder);
            if (x64Builder == null)
                return null;
        }
        return root.build();
    }

    private X64Builder initializeDataSection() {
        X64Builder x64Builder = new X64Builder();
        x64Builder.addLine(new X64Code(".data"));
        return x64Builder;
    }

    public X64Builder visit(ConditionalJump jumpIfFalse, X64Builder x64builder) {
        if (lastComparisonOperator == null) {
            if (jumpIfFalse.condition instanceof ConstantName) {
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, jumpIfFalse.condition, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
            }
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, resolveLoadLocation(jumpIfFalse.condition)))
                    .addLine(x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
        } else {
            x64builder.addLine(
                    x64InstructionLineWithComment("jump if false " + jumpIfFalse.condition.repr(), X64Instruction.getCorrectJumpIfFalseInstruction(lastComparisonOperator), x64Label(jumpIfFalse.trueLabel)));
        }
        lastComparisonOperator = null;
        return x64builder;
    }

    @Override
    public X64Builder visit(ArrayBoundsCheck arrayBoundsCheck, X64Builder x64builder) {
        final var getAddress = arrayBoundsCheck.getAddress;
        final var index = getAddress.getIndex();
        final var arrayLength = getAddress.getLength().orElseThrow();
        final var indexRegister = indexToIndexRegister.get(index);
        indexToIndexRegister.remove(index);
        // the index is stored in a register
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, indexRegister))
                .addLine(x64InstructionLine(X64Instruction.jge, x64Label(arrayBoundsCheck.getIndexIsNonNegativeLabel())))
                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.getIndexIsNonNegativeLabel() + ":"))

                .addLine(x64InstructionLine(X64Instruction.cmp, arrayLength, indexRegister))
                .addLine(x64InstructionLine(X64Instruction.jl, x64Label(arrayBoundsCheck.getIndexIsLessThanArraySizeLabel())))

                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.getIndexIsLessThanArraySizeLabel() + ":"));
    }

    @Override
    public X64Builder visit(RuntimeException runtimeException, X64Builder x64Builder) {
        return x64Builder
                .addLine(x64InstructionLine(X64Instruction.mov, new ConstantName((long) runtimeException.errorCode, BuiltinType.Int), "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"));
    }

    private boolean isRegister(String location) {
        return location.startsWith("%");
    }

    private boolean isConstant(String location) {
        return location.startsWith("$");
    }

    @Override
    public X64Builder visit(Assign assign, X64Builder x64Builder) {
        String sourceStackLocation = resolveLoadLocation(assign.operand);
        String destStackLocation = resolveLoadLocation(assign.store);

        switch (assign.assignmentOperator) {
            case "--":
            case "++":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectIncrementInstruction(assign.assignmentOperator), COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.movq, COPY_TEMP_REGISTER, destStackLocation));
            case "+=":
            case "-=":
            case "*=":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLineWithComment(String.format("%s += %s", assign.store, assign.operand.repr()), X64Instruction.getX64OperatorCode(assign.assignmentOperator.substring(0, 1)), COPY_TEMP_REGISTER, destStackLocation));
            case "=":
                if (sourceStackLocation.equals(destStackLocation))
                    return x64Builder;
                else if (isRegister(sourceStackLocation) || isConstant(sourceStackLocation))
                    return x64Builder.addLine(x64InstructionLineWithComment(
                            String.format("%s = %s", assign.store.repr(), assign.operand.repr()), X64Instruction.movq, sourceStackLocation, destStackLocation));
                else
                    return x64Builder.addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, COPY_TEMP_REGISTER))
                            .addLine(x64InstructionLineWithComment(
                                    String.format("%s = %s", assign.store.repr(), assign.operand.repr()), X64Instruction.movq, COPY_TEMP_REGISTER, destStackLocation));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(GetAddress getAddress, X64Builder x64Builder) {
        memoryAddressToArrayMap.put(getAddress.getStore(), getAddress.getBaseAddress());
        final AbstractName index = getAddress.getIndex();
        final String resolvedIndex = resolveLoadLocation(index);
        final X64Register indexRegister = freeIndexRegisters.pop();
        memoryAddressToIndexRegister.put(getAddress.getStore(), indexRegister);
        indexToIndexRegister.put(index, indexRegister);
        return x64Builder.addLine(x64InstructionLine(X64Instruction.movq, resolvedIndex, indexRegister));
    }

    @Override
    public X64Builder visit(Label label, X64Builder x64builder) {
        return x64builder.addLine(new X64Code("." + label.label + ":"));
    }

    public static String tabSpaced(Object s) {
        return "\t" + s + "\t";
    }

    public static String commaSeparated(Object... s) {
        return Arrays
                .stream(s)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    public static X64Code x64InstructionLine(Object instruction, Object... args) {
        return new X64Code(tabSpaced(instruction) + commaSeparated(args));
    }

    public X64Code x64Label(Label label) {
        return new X64Code("." + label.label);
    }

    public X64Code x64Label(String label) {
        return new X64Code("." + label);
    }

    public X64Code x64InstructionLineWithComment(String comment, Object instruction, Object... args) {
        return new X64Code(tabSpaced(instruction) + commaSeparated(args), comment);
    }

    @Override
    public X64Builder visit(MethodEnd methodEnd, X64Builder x64builder) {
        // apparently we need a return code of zero
//        calleeRestore(x64builder,);
        int loc = subqLocs.pop();
        if (stackSpace != 0) {
            stackSpace = roundUp16(stackSpace);
            x64builder.addAtIndex(loc, x64InstructionLine(X64Instruction.subq, "$" + stackSpace, X64Register.RSP));
        }
        x64builder.addAtIndex(loc,
                x64InstructionLine(X64Instruction.movq, X64Register.RSP, X64Register.RBP));
        x64builder.addAtIndex(loc,
                x64InstructionLine(X64Instruction.pushq, X64Register.RBP));

        x64builder = (methodEnd.isMain() ? x64builder
                .addLine(x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX)) : x64builder);
        if (stackSpace != 0) {
            x64builder.addLine(x64InstructionLine(X64Instruction.addq, "$" + stackSpace, X64Register.RSP));
        }
        x64builder.addLine(x64InstructionLine(X64Instruction.movq, X64Register.RBP, X64Register.RSP))
                .addLine(x64InstructionLine(X64Instruction.popq, X64Register.RBP))
                .addLine(x64InstructionLine(X64Instruction.ret));
        return x64builder;
    }

    @Override
    public X64Builder visit(MethodBegin methodBegin, X64Builder x64builder) {
        lastComparisonOperator = null;
        currentMethod = methodBegin;
        currentMapping = registerMapping.getOrDefault(methodBegin, new HashMap<>());

        if (!textAdded) {
            x64builder = x64builder.addLine(new X64Code(".text"));
            textAdded = true;
        }

        if (methodBegin.isMain())
            x64builder = x64builder.addLine(new X64Code(".globl main"));

        x64builder.addLine(new X64Code(methodBegin.methodName() + ":"));

        int stackOffsetIndex = 0;
        List<X64Code> codes = new ArrayList<>();

        List<AbstractName> locals = getLocals(methodBegin.entryBlock.instructionList);
        reorderLocals(locals, methodBegin.methodDefinition);

        for (var variableName : locals) {
            if (!globals.contains(variableName)) {
                if (variableName.size > 8) {
                    // we have an array
                    stackOffsetIndex += variableName.size + 8;
                    methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
                    long offset = methodBegin.nameToStackOffset.get(variableName.toString());
                    long localSize = 0;
                    for (int i = 0; i < variableName.size; i += 8) {
                        localSize += 8;
                        final String arrayLocation = String.format("-%s(%s)", offset - localSize, X64Register.RBP);
                        codes.add(
                                x64InstructionLineWithComment(
                                        String.format("%s[%s] = 0",
                                                variableName,
                                                i >> 3),
                                        X64Instruction.movq, ZERO, arrayLocation));
                    }
                    final String arrayLocation = String.format("-%s(%s)", offset, X64Register.RBP);
                    codes.add(
                            x64InstructionLineWithComment(
                                    String.format("%s[] = 0",
                                            variableName),
                                    X64Instruction.movq, ZERO, arrayLocation));
                } else {
                    if (currentMapping.get(variableName) == null || currentMapping.get(variableName)
                            .equals(X64Register.STACK)) {
                        stackOffsetIndex += variableName.size;
                        methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
                    }
                }
            }
        }

        stackSpace = stackOffsetIndex;
        subqLocs.push(x64builder.currentIndex());

        x64builder.addLines(codes);

        return x64builder;
    }

    private void callerSave(X64Builder x64Builder, X64Register returnAddressRegister) {
        var instruction = instructionList.get(currentInstructionIndex);
        var x64Registers = methodToLiveRegistersInfo.getOrDefault(currentMethod, Collections.emptyMap())
                .getOrDefault(instruction, Collections.emptySet());
        int startIndex = x64Builder.currentIndex();
        for (var x64Register : x64Registers) {
            if (x64Register.equals(X64Register.STACK))
                continue;
            if (x64Register != returnAddressRegister) {
                String location = getNextStackLocation(x64Register.toString());
                x64Builder.addAtIndex(startIndex, x64InstructionLine(X64Instruction.movq, x64Register, location));
            }
        }
    }

    private void callerRestore(X64Builder x64Builder, X64Register returnAddressRegister) {
        var instruction = instructionList.get(currentInstructionIndex);
        Set<X64Register> x64Registers = methodToLiveRegistersInfo.getOrDefault(currentMethod, Collections.emptyMap())
                .getOrDefault(instruction, Collections.emptySet());
        for (var x64Register : x64Registers) {
            if (x64Register.equals(X64Register.STACK))
                continue;
            if (x64Register != returnAddressRegister) {
                String location = getNextStackLocation(x64Register.toString());
                x64Builder.addLine(x64InstructionLine(X64Instruction.movq, location, x64Register));
            }
        }
    }

    /**
     * round n up to the nearest multiple of m
     */
    private static long roundUp16(long n) {
        return n >= 0 ? ((n + (long) 16 - 1) / (long) 16) * (long) 16 : (n / (long) 16) * (long) 16;
    }

    private final HashMap<X64Register, Long> argRegisterToStackOffset = new HashMap<>();

    private String getStackLocationOfParameterRegisterFromCache(X64Register argumentRegister) {
        if (!argRegisterToStackOffset.containsKey(argumentRegister)) {
            stackSpace += 8;
            argRegisterToStackOffset.put(argumentRegister, stackSpace);
        }
        return String.format("-%s(%s)", argRegisterToStackOffset.get(argumentRegister), X64Register.RBP);
    }


    /**
     * Splices push arguments into the correct position in the XBuilder
     * while avoiding overwrites
     * @param x64Builder the X64Builder to modify
     */

    private void orderFunctionCallArguments(X64Builder x64Builder) {
        // first let's copy the parameters we will push to the stack
        // this makes it easier to extract the most recent 6 parameters
        var copyOfPushArguments = new ArrayList<>(pushArguments);

        // we reverse the push parameters because parameters are pushed to the stack
        // in LIFO order
        Collections.reverse(copyOfPushArguments);

        // extract the most recent pushes
        var argumentsToStoreInParameterRegisters = new ArrayList<>(copyOfPushArguments.subList(0, Math.min(6, pushArguments.size())));

        // this is a map of a register to the argument it stores
        // note that the registers are represented as enums -> we use an EnumMap
        // note that X64Register.STACK is also included, so we need to consider it differently later
        var registerToResidentArgument = new EnumMap<X64Register, PushArgument>(X64Register.class);

        // we also have a reverse map to make looking an argument's register easier
        var residentArgumentToRegister = new HashMap<PushArgument, X64Register>();

        // this is a map of arguments to the correct argument registers
        // we assign the arguments in the exact order, i.e RDI, RSI, RDX, RCX, R8, R9
        // this is why we use an indexOfArgument variable to loop through the argument registers
        // in the correct order
        var pushParameterX64RegisterMap = new HashMap<PushArgument, X64Register>();

        // the index of the parameter register
        int indexOfParameterRegister = 0;
        for (PushArgument argument : argumentsToStoreInParameterRegisters) {
            // argument register
            var parameterRegister = X64Register.parameterRegisters[indexOfParameterRegister];

            // the register which houses this argument
            // if the mapping doesn't contain a register for this argument, we default to X64Register.STACK
            var residentRegister = currentMapping.getOrDefault(argument.parameterName, X64Register.STACK);

            registerToResidentArgument.put(residentRegister, argument);
            residentArgumentToRegister.put(argument, residentRegister);

            pushParameterX64RegisterMap.put(argument, parameterRegister);

            // march forward
            indexOfParameterRegister++;
        }

        // we create a set of parameter registers to make lookups more convenient
        var setOfParameterRegisters = Set.of(X64Register.parameterRegisters);

        // here we store arguments whose resident registers are the same as parameter registers
        // which could be potentially overwritten as we push arguments to parameter registers
        var potentiallyOverwrittenParameterRegisters = new HashMap<PushArgument, X64Register>();

        // instructions we will splice into the builder
        var instructions = new ArrayList<X64Code>();

        for (X64Register x64Register: registerToResidentArgument.keySet()) {
            if (setOfParameterRegisters.contains(x64Register)) {
                potentiallyOverwrittenParameterRegisters.put(registerToResidentArgument.get(x64Register), x64Register);
                // this is the stack location storing the argument resident register
                var registerCache = getStackLocationOfParameterRegisterFromCache(x64Register);
                instructions.add(x64InstructionLine(X64Instruction.movq, x64Register, registerCache));
            }
        }

        for (PushArgument argument: argumentsToStoreInParameterRegisters) {
            var argumentResidentRegister = residentArgumentToRegister.getOrDefault(argument, null);
            // if the argument is stored in the stack, just move it from the stack to parameter register
            if (argumentResidentRegister == X64Register.STACK) {
                instructions.add(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(argument.parameterName), pushParameterX64RegisterMap.get(argument)));
            } else {
                // if conflict might happen, then move from the register cache
                if (potentiallyOverwrittenParameterRegisters.containsKey(argument)) {
                    var parameterRegisterCache = getStackLocationOfParameterRegisterFromCache(argumentResidentRegister);
                    instructions.add(x64InstructionLine(X64Instruction.movq, parameterRegisterCache, pushParameterX64RegisterMap.get(argument)));
                } else {
                    // just resolve the location, for arguments like string constants and constants
                    instructions.add(x64InstructionLine(X64Instruction.movq, argumentResidentRegister.toString(), pushParameterX64RegisterMap.get(argument)));
                }
            }
        }
       x64Builder.addAllAtIndex(x64Builder.currentIndex(), instructions);
    }



    @Override
    public X64Builder visit(FunctionCallWithResult methodCall, X64Builder x64builder) {
        callerSave(x64builder, currentMapping.get(methodCall.store));
        orderFunctionCallArguments(x64builder);
        (methodCall.isImported() ? x64builder.addLine((x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX))) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.callq, methodCall.getMethodName()))
                .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, resolveLoadLocation(methodCall
                        .getStore())));
        callerRestore(x64builder, currentMapping.get(methodCall.store));
        pushArguments.clear();
        return x64builder;
    }

    @Override
    public X64Builder visit(FunctionCallNoResult methodCall, X64Builder x64builder) {
        callerSave(x64builder, null);
        orderFunctionCallArguments(x64builder);
        (methodCall.isImported() ? x64builder.addLine((x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX))) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.callq,
                        methodCall.getMethodName()));
         callerRestore(x64builder, null);
        pushArguments.clear();
        return x64builder;

    }

    @Override
    public X64Builder visit(MethodReturn methodReturn, X64Builder x64builder) {
        if (methodReturn
                .getReturnAddress()
                .isPresent())
            x64builder = x64builder.addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(methodReturn
                    .getReturnAddress()
                    .get()), X64Register.RAX));
        return x64builder;
    }

    @Override
    public X64Builder visit(UnaryInstruction oneOperandAssign, X64Builder x64Builder) {
        String sourceStackLocation = resolveLoadLocation(oneOperandAssign.operand);
        String destStackLocation = resolveLoadLocation(oneOperandAssign.store);

        switch (oneOperandAssign.operator) {
            case "!":
                lastComparisonOperator = null;
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.movq, COPY_TEMP_REGISTER, destStackLocation))
                        .addLine(x64InstructionLine(X64Instruction.xor, ONE, destStackLocation));
            case "-":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.neg, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.movq, COPY_TEMP_REGISTER, destStackLocation));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(PushArgument pushArgument, X64Builder x64builder) {
        pushArguments.push(pushArgument);
        if (pushArgument.parameterIndex < N_ARG_REGISTERS) {
            // we do not want to overwrite registers, so we just save this push so that
            // we find a correct ordering that does not cause overwrites later
            // just before the function call
            return x64builder;
        } else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, resolveLoadLocation(pushArgument.parameterName)));
    }

    @Override
    public X64Builder visit(PopParameter popParameter, X64Builder x64builder) {
        if (popParameter.parameterIndex < N_ARG_REGISTERS)
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.movq,
                            X64Register.parameterRegisters[popParameter.parameterIndex],
                            resolveLoadLocation(popParameter.parameterName)));
        else
            return x64builder
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            getRbpCalledArgument((popParameter.parameterIndex + 1 - N_ARG_REGISTERS) * 8 + 8),
                            COPY_TEMP_REGISTER))
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            COPY_TEMP_REGISTER,
                            resolveLoadLocation(popParameter.parameterName)));
    }

    public String getRbpCalledArgument(int index) {
        return String.format("%s(%s)", index, X64Register.RBP);
    }


    public String resolveLoadLocation(AbstractName name) {
        if (name instanceof MemoryAddressName) {
            var base = memoryAddressToArrayMap.get(name);
            var indexRegister = Objects.requireNonNull(memoryAddressToIndexRegister.get(name));
            String address;
            if (globals.contains(base)) {
                address = String.format("%s(,%s,%s)", base, indexRegister, 8);
            } else {
                var offset = currentMethod.nameToStackOffset.get(base.toString());
                address = String.format("-%s(%s,%s,%s)", offset, X64Register.RBP, indexRegister, 8);
            }
            freeIndexRegisters.push(indexRegister);
            memoryAddressToIndexRegister.remove(name);
            return address;
        }
        X64Register register = currentMapping.get(name);
        if (register != null && register != X64Register.STACK) {
                return currentMapping.get(name)
                        .toString();
        } else if (register == X64Register.STACK) {
            return getNextStackLocation(name.toString());
        }
        if (globals.contains(name)) {
            return String.format("%s(%s)", name.toString(), "%rip");
        } else if (name instanceof StringConstantName || name instanceof ConstantName)
            return name.toString();
        return String.format("-%s(%s)", currentMethod.nameToStackOffset.get(name.toString()), X64Register.RBP);
    }

    @Override
    public X64Builder visit(StringLiteralStackAllocation stringLiteralStackAllocation, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(stringLiteralStackAllocation.getASM()));
    }

    private String getNextStackLocation(String loc) {
        if (loc.contains("stack") || !currentMethod.nameToStackOffset.containsKey(loc)) {
            stackSpace += 8;
            currentMethod.nameToStackOffset.put(loc, (int) stackSpace);
        }
        return String.format("-%s(%s)", currentMethod.nameToStackOffset.get(loc), X64Register.RBP);
    }

    @Override
    public X64Builder visit(BinaryInstruction binaryInstruction, X64Builder x64builder) {
        switch (binaryInstruction.operator) {
            case Operators.PLUS:
            case Operators.MINUS:
            case Operators.MULTIPLY:
            case Operators.CONDITIONAL_OR:
            case Operators.CONDITIONAL_AND:
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.getX64OperatorCode(binaryInstruction.operator), resolveLoadLocation(binaryInstruction.sndOperand), COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store.repr(), binaryInstruction.fstOperand.repr(), binaryInstruction.operator, binaryInstruction.sndOperand.repr()), X64Instruction.movq, COPY_TEMP_REGISTER, resolveLoadLocation(binaryInstruction.store)));
            case Operators.DIVIDE:
            case Operators.MOD: {
                // If we are planning to use RDX, we spill it first
                if (!resolveLoadLocation(binaryInstruction.store).equals(X64Register.RDX.toString()))
                    x64builder.addLine(x64InstructionLine(X64Instruction.movq, X64Register.RDX, getNextStackLocation(X64Register.RDX.toString())));
                if (binaryInstruction.sndOperand instanceof ConstantName) {
                    x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.sndOperand), COPY_TEMP_REGISTER))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.idivq, COPY_TEMP_REGISTER));
                } else {
                    x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(binaryInstruction.sndOperand)));
                }
                x64builder.addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store.repr(), binaryInstruction.fstOperand.repr(), binaryInstruction.operator, binaryInstruction.sndOperand.repr()), X64Instruction.movq, (binaryInstruction.operator.equals("%") ? X64Register.RDX : X64Register.RAX), resolveLoadLocation(binaryInstruction.store)));
                // restore RDX
                if (!resolveLoadLocation(binaryInstruction.store).equals(X64Register.RDX.toString()))
                    x64builder.addLine(x64InstructionLine(X64Instruction.movq, getNextStackLocation(X64Register.RDX.toString()), X64Register.RDX));
                return x64builder;
            }
            // comparison operators
            case Operators.EQ:
            case Operators.NEQ:
            case Operators.LT:
            case Operators.GT:
            case Operators.LEQ:
            case Operators.GEQ:
                lastComparisonOperator = binaryInstruction.operator;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.cmp, resolveLoadLocation(binaryInstruction.sndOperand), COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(binaryInstruction.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, COPY_TEMP_REGISTER))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store.repr(), binaryInstruction.fstOperand.repr(), binaryInstruction.operator, binaryInstruction.sndOperand.repr()), X64Instruction.movq, COPY_TEMP_REGISTER, resolveLoadLocation(binaryInstruction.store)));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(X64Instruction.jmp, x64Label(unconditionalJump.goToLabel)));
    }

    @Override
    public X64Builder visit(GlobalAllocation globalAllocation, X64Builder x64Builder) {
        globals.add(globalAllocation.variableName);
        return x64Builder.addLine(x64InstructionLine(String.format(".comm %s, %s, %s", globalAllocation.variableName, globalAllocation.variableName.size, 64)));
    }
}


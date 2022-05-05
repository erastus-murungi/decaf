package edu.mit.compilers.asm;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static edu.mit.compilers.asm.X64Register.N_ARG_REGISTERS;

public class X64CodeConverter implements InstructionVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    private final Set<AbstractName> globals = new HashSet<>();
    private final Stack<MethodBegin> callStack = new Stack<>();
    private final ConstantName ZERO = new ConstantName(0L, BuiltinType.Int);
    private final ConstantName ONE = new ConstantName(1L, BuiltinType.Int);
    private final X64Register[] arrayIndexRegisters = {X64Register.RBX, X64Register.RCX};
    private final Stack<Pair<ArrayName, X64Register>> stackArrays = new Stack<>();
    private long stackSpace;
    public String lastComparisonOperator = null;
    private int arrayAccessCount = 0;


    private List<AbstractName> getLocals(InstructionList instructionList) {
        Set<AbstractName> uniqueNames = new HashSet<>();
        var flattened = instructionList.flatten();

        for (Instruction instruction : flattened) {
            for (AbstractName name : instruction.getAllNames()) {
                if (name instanceof ArrayName && !globals.contains(name)) {
                    uniqueNames.add(name);
                }
            }
        }

        for (Instruction instruction : flattened) {
            for (AbstractName name : instruction.getAllNames()) {
                if (!(name instanceof ArrayName) && !globals.contains(name)) {
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
        for (AbstractName name : methodParametersNames) {
            locals.remove(name);
        }
        locals.addAll(0, methodParamNamesList
                .stream()
                .sorted(Comparator.comparing(AbstractName::toString))
                .collect(Collectors.toList()));
    }


    public X64Program convert(InstructionList instructionList) {
        X64Builder x64Builder = initializeDataSection();
        final X64Builder root = x64Builder;
        for (Instruction instruction : instructionList) {
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
                        .addLine(x64InstructionLine(X64Instruction.movq, jumpIfFalse.condition, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
            }
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, resolveLoadLocation(jumpIfFalse.condition)))
                    .addLine(x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
        } else {
            x64builder.addLine(
                    x64InstructionLineWithComment("jump ifFalse " + jumpIfFalse.condition.toString(), X64Instruction.getCorrectJumpIfFalseInstruction(lastComparisonOperator), x64Label(jumpIfFalse.trueLabel)));
        }
        lastComparisonOperator = null;
        return x64builder;
    }

    @Override
    public X64Builder visit(ArrayBoundsCheck arrayBoundsCheck, X64Builder x64builder) {
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(arrayBoundsCheck.arrayAccess.accessIndex), X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.jge, x64Label(arrayBoundsCheck.indexIsGTEZero)))
                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.indexIsGTEZero.label + ":"))

                .addLine(x64InstructionLine(X64Instruction.cmp, arrayBoundsCheck.arrayAccess.arrayLength, X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.jl, x64Label(arrayBoundsCheck.indexIsLessThanArraySize)))

                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.indexIsLessThanArraySize.label + ":"));
    }

    @Override
    public X64Builder visit(ArrayAccess arrayAccess, X64Builder x64Builder) {
        final X64Register register = arrayIndexRegisters[arrayAccessCount & 1];
        arrayAccessCount += 1;
        stackArrays.push(new Pair<>(arrayAccess.arrayName, register));
        return x64Builder.addLine(
                x64InstructionLine(X64Instruction.movq,
                        X64Register.R13,
                        register));
    }

    @Override
    public X64Builder visit(RuntimeException runtimeException, X64Builder x64Builder) {
        return x64Builder
                .addLine(x64InstructionLine(X64Instruction.mov, new ConstantName((long) runtimeException.errorCode, BuiltinType.Int), "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"));
    }

    @Override
    public X64Builder visit(Assign assign, X64Builder x64Builder) {
        String sourceStackLocation = resolveLoadLocation(assign.operand);
        String destStackLocation = resolveLoadLocation(assign.store);

        switch (assign.assignmentOperator) {
            case "--":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.dec, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            case "++":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.inc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            case "+=":
                return x64Builder
                        .addLine(x64InstructionLineWithComment("move " + assign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s += %s", assign.store, assign.operand), X64Instruction.addq, X64Register.RAX, destStackLocation));
            case "-=":
                return x64Builder
                        .addLine(x64InstructionLineWithComment("move " + assign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s -= %s", assign.store, assign.operand), X64Instruction.subq, X64Register.RAX, destStackLocation));

            case "*=":
                return x64Builder
                        .addLine(x64InstructionLineWithComment("move " + assign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s *= %s", assign.store, assign.operand), X64Instruction.imulq, X64Register.RAX, destStackLocation));
            case "=":
                return (assign.operand instanceof ConstantName) ? (
                        x64Builder
                                .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, destStackLocation)))
                        : x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s", assign.store, assign.operand), X64Instruction.movq, X64Register.RAX, destStackLocation));
            default:
                return null;
        }
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

    public X64Code x64InstructionLineWithComment(String comment, Object instruction, Object... args) {
        return new X64Code(tabSpaced(instruction) + commaSeparated(args), comment);
    }

    private void saveBaseAndStackPointer(X64Builder x64Builder) {
        x64Builder
                .addLine(
                        x64InstructionLine(X64Instruction.pushq, X64Register.RBP))
                .addLine(
                        x64InstructionLine(X64Instruction.movq, X64Register.RSP, X64Register.RBP));
    }

    @Override
    public X64Builder visit(MethodEnd methodEnd, X64Builder x64builder) {
        // apparently we need a return code of zero
        x64builder = (methodEnd
                .methodName()
                .equals("main") ?
                x64builder
                        .addLine(x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX)) : x64builder);
        ((stackSpace == 0) ? x64builder :
                x64builder.addLine(x64InstructionLine(X64Instruction.addq, "$" + stackSpace, X64Register.RSP))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RBP, X64Register.RSP))
                        .addLine(x64InstructionLine(X64Instruction.popq, X64Register.RBP)))
                .addLine(x64InstructionLine(X64Instruction.ret));
        return x64builder;
    }

    @Override
    public X64Builder visit(MethodBegin methodBegin, X64Builder x64builder) {
        lastComparisonOperator = null;
        callStack.push(methodBegin);

        if (!textAdded) {
            x64builder = x64builder.addLine(new X64Code(".text"));
            textAdded = true;
        }

        final String methodName = methodBegin.methodDefinition.methodName.id;

        if (methodName.equals("main")) {
            x64builder = x64builder.addLine(new X64Code(".globl main"));
            for (AbstractName name : globals)
                for (int i = 0; i < name.size; i += 8)
                    x64builder.addLine(x64InstructionLine(X64Instruction.movq, ZERO, i + " + " + resolveLoadLocation(name)));
        }
        x64builder.addLine(new X64Code(methodName + ":"));

        int stackOffsetIndex = 0;
        List<X64Code> codes = new ArrayList<>();

        List<AbstractName> locals = getLocals(methodBegin.entryBlock.instructionList);
        reorderLocals(locals, methodBegin.methodDefinition);
        if (!locals.isEmpty())
            saveBaseAndStackPointer(x64builder);

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
                    stackOffsetIndex += variableName.size;
                    methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
                }
            }
        }

        stackSpace = roundUp16(stackOffsetIndex);
        if (stackSpace != 0)
            x64builder.addLine(x64InstructionLine(X64Instruction.subq, "$" + stackSpace, X64Register.RSP));

        x64builder.addLines(codes);

        return x64builder;
    }

    /**
     * round n up to nearest multiple of m
     */
    private static long roundUp16(long n) {
        return n >= 0 ? ((n + (long) 16 - 1) / (long) 16) * (long) 16 : (n / (long) 16) * (long) 16;
    }

    @Override
    public X64Builder visit(FunctionCallWithResult methodCall, X64Builder x64builder) {
        return (methodCall.isImported() ? x64builder.addLine((x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX))) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()))
                .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, resolveLoadLocation(methodCall
                        .getStore())));
    }

    @Override
    public X64Builder visit(FunctionCallNoResult methodCall, X64Builder x64builder) {
        return (methodCall.isImported() ? x64builder.addLine((x64InstructionLine(X64Instruction.xorl, X64Register.EAX, X64Register.EAX))) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.call,
                        methodCall.getMethodName()));
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
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation))
                        .addLine(x64InstructionLine(X64Instruction.xorb, ONE, destStackLocation));
            case "-":
                return x64Builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.neg, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(PushParameter pushParameter, X64Builder x64builder) {
        if (pushParameter.parameterIndex < N_ARG_REGISTERS)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq,
                    resolveLoadLocation(pushParameter.parameterName),
                    X64Register.argumentRegs[pushParameter.parameterIndex]));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, resolveLoadLocation(pushParameter.parameterName)));
    }

    @Override
    public X64Builder visit(PopParameter popParameter, X64Builder x64builder) {
        if (popParameter.parameterIndex < N_ARG_REGISTERS)
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.movq,
                            X64Register.argumentRegs[popParameter.parameterIndex],
                            resolveLoadLocation(popParameter.parameterName)));
        else
            return x64builder
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            getRbpCalledArgument((popParameter.parameterIndex + 1 - N_ARG_REGISTERS) * 8 + 8),
                            X64Register.R12))
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            X64Register.R12,
                            resolveLoadLocation(popParameter.parameterName)));
    }

    public String getRbpCalledArgument(int index) {
        return String.format("%s(%s)", index, X64Register.RBP);
    }

    private X64Register findLatestOccurrence(AbstractName name) {
        for (Pair<ArrayName, X64Register> nameX64RegisterPair : stackArrays) {
            if (nameX64RegisterPair.first() == name) {
                return nameX64RegisterPair.second();
            }
        }
        return null;
    }

    private void removeStackOccurrence(AbstractName name) {
        Pair<ArrayName, X64Register> nameX64RegisterPairToRemove = null;
        for (Pair<ArrayName, X64Register> nameX64RegisterPair : stackArrays) {
            if (nameX64RegisterPair.first() == name) {
                nameX64RegisterPairToRemove = nameX64RegisterPair;
            }
        }
        stackArrays.remove(nameX64RegisterPairToRemove);
    }

    public String resolveLoadLocation(AbstractName name) {
        X64Register reg = findLatestOccurrence(name);
        if (reg != null) {
            if (globals.contains(name)) {
                String toReturn = String.format("%s(,%s,%s)", name, reg, 8);
                removeStackOccurrence(name);
                return toReturn;
            } else {
                Integer offset = callStack.peek().nameToStackOffset.get(name.toString());
                String toReturn = String.format("-%s(%s,%s,%s)", offset, X64Register.RBP, reg, 8);
                removeStackOccurrence(name);
                return toReturn;
            }
        }
        if (globals.contains(name)) {
            return String.format("%s(%s)", name.toString(), "%rip");
        } else if (name instanceof StringConstantName || name instanceof ConstantName)
            return name.toString();
        return String.format("-%s(%s)", callStack.peek().nameToStackOffset.get(name.toString()), X64Register.RBP);
    }

    @Override
    public X64Builder visit(StringLiteralStackAllocation stringLiteralStackAllocation, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(stringLiteralStackAllocation.getASM()));
    }

    @Override
    public X64Builder visit(BinaryInstruction binaryInstruction, X64Builder x64builder) {
        switch (binaryInstruction.operator) {
            case "+":
            case "-":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getX64OperatorCode(binaryInstruction.operator), resolveLoadLocation(binaryInstruction.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
            case "*":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.imulq, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "/":
                if (binaryInstruction.sndOperand instanceof ConstantName) {
                    return x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.R9))
                            .addLine(x64InstructionLine(X64Instruction.idivq, X64Register.R9))
                            .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
                }
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(binaryInstruction.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "%":
                if (binaryInstruction.sndOperand instanceof ConstantName) {
                    return x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.R9))
                            .addLine(x64InstructionLine(X64Instruction.idivq, X64Register.R9))
                            .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RDX, resolveLoadLocation(binaryInstruction.store)));
                }
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(binaryInstruction.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RDX, resolveLoadLocation(binaryInstruction.store)));
            case "||":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.or, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
            case "&&":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.and, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                lastComparisonOperator = binaryInstruction.operator;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(binaryInstruction.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmp, resolveLoadLocation(binaryInstruction.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(binaryInstruction.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", binaryInstruction.store, binaryInstruction.fstOperand, binaryInstruction.operator, binaryInstruction.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(binaryInstruction.store)));
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
        return x64Builder.addLine(x64InstructionLine(String.format("%s:\n\t\t.zero %s",
                globalAllocation.variableName,
                globalAllocation.variableName.size)));
    }
}


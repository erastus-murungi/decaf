package edu.mit.compilers.asm;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.utils.Pair;

import java.util.*;
import java.util.stream.Collectors;

import static edu.mit.compilers.asm.X64Register.N_ARG_REGISTERS;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    private final Set<AbstractName> globals = new HashSet<>();
    private final Stack<MethodBegin> callStack = new Stack<>();
    private final ConstantName ZERO = new ConstantName(0L);
    private final ConstantName ONE = new ConstantName(1L);
    private final X64Register[] arrayIndexRegisters = {X64Register.RBX, X64Register.RCX};
    private final Stack<Pair<ArrayName, X64Register>> stackArrays = new Stack<>();
    private long stackSpace;
    public String lastComparisonOperator = null;
    private int arrayAccessCount = 0;


    public X64Program convert(ThreeAddressCodeList threeAddressCodeList) {
        X64Builder x64Builder = initializeDataSection();
        final X64Builder root = x64Builder;
        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            x64Builder = threeAddressCode.accept(this, x64Builder);
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
                .addLine(x64InstructionLine(X64Instruction.mov, new ConstantName((long) runtimeException.errorCode), "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"));
    }

    @Override
    public X64Builder visit(Label label, X64Builder x64builder) {
        return x64builder.addLine(new X64Code("." + label.label + ":"));
    }

    public String tabSpaced(Object s) {
        return "\t" + s + "\t";
    }

    public String commaSeparated(Object... s) {
        return Arrays
                .stream(s)
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

    public X64Code x64InstructionLine(Object instruction, Object... args) {
        return new X64Code(tabSpaced(instruction) + commaSeparated(args));
    }

    public X64Code x64Label(Label label) {
        return new X64Code("." + label.label);
    }

    public X64Code x64BlankLine() {
        return new X64Code("\n");
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
        return (methodEnd
                .methodName()
                .equals("main") ?
                x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, ZERO, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.xor, "%edi", "%edi")) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.addq, "$" + stackSpace, X64Register.RSP))
                .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RBP, X64Register.RSP))
                .addLine(x64InstructionLine(X64Instruction.popq, X64Register.RBP))
                .addLine(x64InstructionLine(X64Instruction.ret))
                .addLine(x64BlankLine());
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
        saveBaseAndStackPointer(x64builder);


        int stackOffsetIndex = 8;
        List<X64Code> codes = new ArrayList<>();
        for (final AbstractName variableName : methodBegin.getLocals()) {
            if (!globals.contains(variableName)) {
                if (variableName.size > 8) {
                    // we have an array
                    stackOffsetIndex += variableName.size;
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
                    final String location = resolveLoadLocation(variableName);
                    codes.add(x64InstructionLineWithComment(String.format("%s = 0", variableName), X64Instruction.movq, ZERO, location));
                }
            }
        }

        if (roundUp16(stackOffsetIndex - 8) == stackOffsetIndex - 8) {
            stackOffsetIndex -= 8;
        }
        stackSpace = roundUp16(stackOffsetIndex);
        x64builder
                .addLine(x64InstructionLine(X64Instruction.subq, "$" + stackOffsetIndex, X64Register.RSP))
                .addLines(codes);

        return x64builder;
    }

    /**
     * round n up to nearest multiple of m
     */
    private static long roundUp16(long n) {
        return n >= 0 ? ((n + (long) 16 - 1) / (long) 16) * (long) 16 : (n / (long) 16) * (long) 16;
    }

    @Override
    public X64Builder visit(MethodCallSetResult methodCall, X64Builder x64builder) {
        if (methodCall
                .getResultLocation()
                .isPresent())
            return x64builder
                    .addLine((x64InstructionLine(X64Instruction.xor, X64Register.RAX, X64Register.RAX)))
                    .addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()))
                    .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, resolveLoadLocation(methodCall
                            .getResultLocation()
                            .get())));
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.xor, X64Register.RAX, X64Register.RAX))
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
    public X64Builder visit(Triple oneOperandAssign, X64Builder x64builder) {
        String sourceStackLocation = resolveLoadLocation(oneOperandAssign.operand);
        String destStackLocation = resolveLoadLocation(oneOperandAssign.dst);

        switch (oneOperandAssign.operator) {
            case "!":
                lastComparisonOperator = null;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation))
                        .addLine(x64InstructionLine(X64Instruction.xorb, ONE, destStackLocation));
            case "-":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.neg, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            case "--":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.dec, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            case "++":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.inc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation));
            case "+=":
                return x64builder
                        .addLine(x64InstructionLineWithComment("move " + oneOperandAssign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s += %s", oneOperandAssign.dst, oneOperandAssign.operand), X64Instruction.addq, X64Register.RAX, destStackLocation));
            case "-=":
                return x64builder
                        .addLine(x64InstructionLineWithComment("move " + oneOperandAssign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s -= %s", oneOperandAssign.dst, oneOperandAssign.operand), X64Instruction.subq, X64Register.RAX, destStackLocation));

            case "*=":
                return x64builder
                        .addLine(x64InstructionLineWithComment("move " + oneOperandAssign.operand + " to temp register",
                                X64Instruction.movq, sourceStackLocation, X64Register.RAX)
                        )
                        .addLine(x64InstructionLineWithComment(String.format("%s *= %s", oneOperandAssign.dst, oneOperandAssign.operand), X64Instruction.imulq, X64Register.RAX, destStackLocation));
            case "=":
                return (oneOperandAssign.operand instanceof ConstantName) ? (
                        x64builder
                                .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, destStackLocation)))
                        : x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s", oneOperandAssign.dst, oneOperandAssign.operand), X64Instruction.movq, X64Register.RAX, destStackLocation));


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
        return x64builder.addLine(x64InstructionLine(stringLiteralStackAllocation.getASM()));
    }

    @Override
    public X64Builder visit(Quadruple quadruple, X64Builder x64builder) {
        switch (quadruple.operator) {
            case "+":
            case "-":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getX64OperatorCode(quadruple.operator), resolveLoadLocation(quadruple.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
            case "*":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.imulq, resolveLoadLocation(quadruple.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "/":
                if (quadruple.sndOperand instanceof ConstantName) {
                    return x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.sndOperand), X64Register.R9))
                            .addLine(x64InstructionLine(X64Instruction.idivq, X64Register.R9))
                            .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
                }
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(quadruple.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "%":
                if (quadruple.sndOperand instanceof ConstantName) {
                    return x64builder
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                            .addLine(x64InstructionLine(X64Instruction.cqto))
                            .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.sndOperand), X64Register.R9))
                            .addLine(x64InstructionLine(X64Instruction.idivq, X64Register.R9))
                            .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RDX, resolveLoadLocation(quadruple.dst)));
                }
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(quadruple.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RDX, resolveLoadLocation(quadruple.dst)));
            case "||":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.or, resolveLoadLocation(quadruple.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
            case "&&":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.and, resolveLoadLocation(quadruple.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                lastComparisonOperator = quadruple.operator;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(quadruple.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmp, resolveLoadLocation(quadruple.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(quadruple.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s %s %s", quadruple.dst, quadruple.fstOperand, quadruple.operator, quadruple.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(quadruple.dst)));
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


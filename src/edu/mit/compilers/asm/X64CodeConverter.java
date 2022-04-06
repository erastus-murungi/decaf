package edu.mit.compilers.asm;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.stream.Collectors;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    private final Set<AbstractName> globals = new HashSet<>();
    private final Stack<MethodBegin> callStack = new Stack<>();
    public final int N_ARG_REGISTERS = 6;
    public String lastComparisonOperator = null;
    private final ConstantName ZERO = new ConstantName(0L, 8);
    private final ConstantName ONE = new ConstantName(1L, 8);
    private int arrayAccessCount = 0;
    private final X64Register[] x64Registers = {X64Register.RBX, X64Register.RCX};
    private VariableName lastArrayName = null;

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

    @Override
    public X64Builder visit(CopyInstruction copyInstruction, X64Builder x64builder) {
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(copyInstruction.src), X64Register.RAX))
                .addLine(x64InstructionLineWithComment(String.format("%s = %s", copyInstruction.dst, copyInstruction.src), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(copyInstruction.dst)));
    }

    public X64Builder visit(JumpIfFalse jumpIfFalse, X64Builder x64builder) {
        if (lastComparisonOperator == null) {
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, resolveLoadLocation(jumpIfFalse.condition)))
                    .addLine(x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
        } else {
            x64builder.addLine(
                    x64InstructionLineWithComment(jumpIfFalse.condition.toString(), X64Instruction.getCorrectJumpIfFalseInstruction(lastComparisonOperator), x64Label(jumpIfFalse.trueLabel)));
        }
        lastComparisonOperator = null;
        return x64builder;
    }

    @Override
    public X64Builder visit(ArrayBoundsCheck arrayBoundsCheck, X64Builder x64builder) {
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(arrayBoundsCheck.arrayIndex), X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.jge, x64Label(arrayBoundsCheck.indexIsGTEZero)))
                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.indexIsGTEZero.label + ":\n"))

                .addLine(x64InstructionLine(X64Instruction.cmp, arrayBoundsCheck.arraySize, X64Register.R13))
                .addLine(x64InstructionLine(X64Instruction.jl, x64Label(arrayBoundsCheck.indexIsLessThanArraySize)))

                .addLine(x64InstructionLine(X64Instruction.movl, ONE, "%edi"))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))

                .addLine(new X64Code("." + arrayBoundsCheck.indexIsLessThanArraySize.label + ":\n"));
    }

    @Override
    public X64Builder visit(ArrayAccess arrayAccess, X64Builder x64Builder) {
        lastArrayName = arrayAccess.arrayName;
        return x64Builder.addLine(x64InstructionLineWithComment(
                String.format("%s", arrayAccess), X64Instruction.movq,
                X64Register.R13,
                x64Registers[arrayAccessCount & 1]));
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

    private X64Builder saveBaseAndStackPointer(X64Builder x64Builder) {
        return x64Builder
                .addLine(
                        x64InstructionLine(X64Instruction.pushq, X64Register.RBP))
                .addLine(
                        x64InstructionLine(X64Instruction.movq, X64Register.RSP, X64Register.RBP));
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
        int stackOffsetIndex = 8;

        if (methodName.equals("main"))
            x64builder = x64builder.addLine(new X64Code(".globl main"));

        x64builder.addLine(new X64Code(methodName + ":"));
        saveBaseAndStackPointer(x64builder);

        for (final AbstractName variableName : methodBegin.getLocals()) {
            methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
            if (!globals.contains(variableName)) {
                for (int i = 0; i < variableName.size; i += 8) {
                    stackOffsetIndex += 8;
                    x64builder.addLine(x64InstructionLineWithComment(String.format("%s[%s..%s] = 0    (%s)", variableName, i, i + 7, resolveLoadLocation(variableName)), X64Instruction.pushq, ZERO));
                }
            }
        }
        if (roundUp16(methodBegin.sizeOfLocals) != methodBegin.sizeOfLocals) {
            x64builder.addLine(x64InstructionLineWithComment("padding", X64Instruction.pushq, ZERO));
        }
        return x64builder;
    }

    /**
     * round n up to nearest multiple of m
     */
    private static long roundUp16(long n) {
        return n >= 0 ? ((n + (long) 16 - 1) / (long) 16) * (long) 16 : (n / (long) 16) * (long) 16;
    }

    @Override
    public X64Builder visit(MethodCall methodCall, X64Builder x64builder) {
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
    public X64Builder visit(MethodEnd methodEnd, X64Builder x64builder) {
        // apparently we need a return code of zero
        return (methodEnd
                .methodName()
                .equals("main") ?
                x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, ZERO, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.xor, "%edi", "%edi")) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.leave))
                .addLine(x64InstructionLine(X64Instruction.ret))
                .addLine(x64BlankLine());
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
    public X64Builder visit(OneOperandAssign oneOperandAssign, X64Builder x64builder) {
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
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s = %s", destStackLocation, sourceStackLocation), X64Instruction.movq, X64Register.RAX, destStackLocation));


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

    public String resolveLoadLocation(AbstractName name) {
        if (name.equals(lastArrayName) && globals.contains(name)) {
            String toReturn = String.format("%s(,%s,%s)", lastArrayName, x64Registers[arrayAccessCount & 1], 8);
            arrayAccessCount++;
            lastArrayName = null;
            return toReturn;
        } else if (name.equals(lastArrayName) && !globals.contains(name)) {
            Integer offset = callStack.peek().nameToStackOffset.get(name.toString());
            String toReturn = String.format("-%s(%s,%s,%s)", offset, X64Register.RBP, x64Registers[arrayAccessCount & 1], 8);
            arrayAccessCount++;
            lastArrayName = null;
            return toReturn;
        }
        if (globals.contains(name) || name instanceof StringConstantName || name instanceof ConstantName)
            return name.toString();
        return String.format("-%s(%s)", callStack.peek().nameToStackOffset.get(name.toString()), X64Register.RBP);
    }

    @Override
    public X64Builder visit(StringLiteralStackAllocation stringLiteralStackAllocation, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(stringLiteralStackAllocation.getASM()));
    }

    @Override
    public X64Builder visit(TwoOperandAssign twoOperandAssign, X64Builder x64builder) {
        switch (twoOperandAssign.operator) {
            case "+":
            case "-":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getX64OperatorCode(twoOperandAssign.operator), resolveLoadLocation(twoOperandAssign.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            case "*":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.imulq, resolveLoadLocation(twoOperandAssign.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "/":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(twoOperandAssign.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "%":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, resolveLoadLocation(twoOperandAssign.sndOperand)))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RDX, resolveLoadLocation(twoOperandAssign.dst)));
            case "||":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.or, resolveLoadLocation(twoOperandAssign.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            case "&&":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.and, resolveLoadLocation(twoOperandAssign.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                lastComparisonOperator = twoOperandAssign.operator;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, resolveLoadLocation(twoOperandAssign.fstOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmp, resolveLoadLocation(twoOperandAssign.sndOperand), X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(twoOperandAssign.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, X64Register.RAX))
                        .addLine(x64InstructionLineWithComment(String.format("%s=%s %s %s", twoOperandAssign.dst, twoOperandAssign.fstOperand, twoOperandAssign.operator, twoOperandAssign.sndOperand), X64Instruction.movq, X64Register.RAX, resolveLoadLocation(twoOperandAssign.dst)));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(X64Instruction.jmp, x64Label(unconditionalJump.goToLabel)));
    }

    @Override
    public X64Builder visit(DataSectionAllocation dataSectionAllocation, X64Builder x64Builder) {
        globals.add(dataSectionAllocation.variableName);
        return x64Builder.addLine(x64InstructionLine(String.format("%s:\n\t\t.zero %s",
                dataSectionAllocation.variableName,
                dataSectionAllocation.variableName.size)));
    }
}


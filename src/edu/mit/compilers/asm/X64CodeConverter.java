package edu.mit.compilers.asm;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.stream.Collectors;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    private final Set<DataSectionAllocation> globalsDataSection = new HashSet<>();
    private final Set<AbstractName> globals = new HashSet<>();
    private final Stack<MethodBegin> callStack = new Stack<>();
    public final int N_ARG_REGISTERS = 6;
    public String lastComparisonOperator = null;
    private ConstantName ZERO = new ConstantName(0L, 8);
    private ConstantName ONE = new ConstantName(1L, 8);

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
                .addLine(x64InstructionLine(X64Instruction.movq, getLocalVariableStackOffset(copyInstruction.src), X64Register.RAX))
                .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, getLocalVariableStackOffset(copyInstruction.dst)));
    }

    @Override
    public X64Builder visit(JumpIfFalse jumpIfFalse, X64Builder x64builder) {
        if (lastComparisonOperator == null) {
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, getLocalVariableStackOffset(jumpIfFalse.condition)))
                    .addLine(x64InstructionLine(X64Instruction.je, x64Label(jumpIfFalse.trueLabel)));
        }
        return x64builder.addLine(
                x64InstructionLine(X64Instruction.getCorrectJumpIfFalseInstruction(lastComparisonOperator), x64Label(jumpIfFalse.trueLabel)));
    }

    @Override
    public X64Builder visit(ArrayBoundsCheck arrayBoundsCheck, X64Builder x64builder) {
        return x64builder
                .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO, getLocalVariableStackOffset(arrayBoundsCheck.location)))
                .addLine(x64InstructionLine(X64Instruction.jl, x64Label(arrayBoundsCheck.boundsBad)))
                // diff btwn mov and movq?
                .addLine(x64InstructionLine(X64Instruction.mov, getLocalVariableStackOffset(arrayBoundsCheck.location), X64Register.RAX))
                .addLine(x64InstructionLine(X64Instruction.cmp, "$"+arrayBoundsCheck.arraySize, X64Register.RAX))
                .addLine(x64InstructionLine(X64Instruction.jge, x64Label(arrayBoundsCheck.boundsBad)))
                .addLine(x64InstructionLine(X64Instruction.jmp, x64Label(arrayBoundsCheck.boundsGood)))
                // TODO: handle bound error code
                .addLine(x64InstructionLine(X64Instruction.mov, "$1", X64Register.RAX))
                .addLine(x64InstructionLine(X64Instruction.call, "exit"))
                .addLine(x64InstructionLine(x64Label(arrayBoundsCheck.boundsGood)));
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


    private X64Builder saveBaseAndStackPointer(X64Builder x64Builder) {
        return x64Builder
                .addLine(
                        x64InstructionLine(X64Instruction.pushq, X64Register.RBP))
                .addLine(
                        x64InstructionLine(X64Instruction.movq, X64Register.RSP, X64Register.RBP));
    }

    private X64Builder pushScope(long size, boolean areMethodParams, X64Builder x64builder) {
        saveBaseAndStackPointer(x64builder);

        if (areMethodParams) //don't modify params
            x64builder.addLine(x64InstructionLine(X64Instruction.subq, "$" + size, X64Instruction.subq));
        else
            for (long i = 0; i < size; i += 8)
                x64builder.addLine(x64InstructionLine(X64Instruction.pushq, "$0")); //fields should be initialized to 0
        return x64builder;
    }

    @Override
    public X64Builder visit(MethodBegin methodBegin, X64Builder x64builder) {
        lastComparisonOperator = null;
        if (!textAdded) {
            x64builder = x64builder.addLine(new X64Code(".text"));
            textAdded = true;
        }

        final String methodName = methodBegin.methodDefinition.methodName.id;
        int stackOffsetIndex = 8;

        for (final AbstractName variableName : methodBegin.getLocals()) {
            methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
            stackOffsetIndex += 8;
        }

        if (methodName.equals("main"))
            x64builder = x64builder.addLine(new X64Code(".globl main"));

        for (DataSectionAllocation globalVariable : globalsDataSection)
            for (int i = 0; i < globalVariable.size; i += 8)
                x64builder.addLine(x64InstructionLine(X64Instruction.movq, ZERO, i + " + " + globalVariable.variableName));

        for (AbstractName name : globals)
            for (int i = 0; i < name.size; i += 8)
                x64builder.addLine(x64InstructionLine(X64Instruction.movq, ZERO, i + " + " + getLocalVariableStackOffset(name)));

        callStack.push(methodBegin);
        return saveBaseAndStackPointer(x64builder.addLine(new X64Code(methodName + ":")))
                .addLine(x64InstructionLine(X64Instruction.subq, new ConstantName(roundUp(methodBegin.sizeOfLocals, 16), 8), X64Register.RSP));
    }

    /**
     * round n up to nearest multiple of m
     */
    private static long roundUp(long n, long m) {
        return n >= 0 ? ((n + m - 1) / m) * m : (n / m) * m;
    }

    @Override
    public X64Builder visit(MethodCall methodCall, X64Builder x64builder) {
        if (methodCall
                .getResultLocation()
                .isPresent())
            return x64builder
                    .addLine((x64InstructionLine(X64Instruction.xor, X64Register.RAX, X64Register.RAX)))
                    .addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()))
                    .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, getLocalVariableStackOffset(methodCall
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
                .equals("main") ? x64builder.addLine(x64InstructionLine(X64Instruction.movq, "$0", X64Register.RAX)) : x64builder)
                .addLine(x64InstructionLine(X64Instruction.leave))
                .addLine(x64InstructionLine(X64Instruction.ret))
                .addLine(x64BlankLine());
    }

    @Override
    public X64Builder visit(MethodReturn methodReturn, X64Builder x64builder) {
        if (methodReturn
                .getReturnAddress()
                .isPresent())
            x64builder = x64builder.addLine(x64InstructionLine(X64Instruction.movq, getLocalVariableStackOffset(methodReturn
                    .getReturnAddress()
                    .get()), X64Register.RAX));
        return x64builder;
    }

    @Override
    public X64Builder visit(OneOperandAssign oneOperandAssign, X64Builder x64builder) {
        String sourceStackLocation = getLocalVariableStackOffset(oneOperandAssign.operand);
        String destStackLocation = getLocalVariableStackOffset(oneOperandAssign.dst);

        switch (oneOperandAssign.operator) {
            case "!":
            lastComparisonOperator = null;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, sourceStackLocation, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, destStackLocation))
                        .addLine(x64InstructionLine(X64Instruction.xorb, ONE, destStackLocation))
                        .addLine(x64InstructionLine(X64Instruction.cmpq, ZERO,destStackLocation));
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

            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(PushParameter pushParameter, X64Builder x64builder) {
        if (pushParameter.parameterIndex < N_ARG_REGISTERS)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq,
                    getLocalVariableStackOffset(pushParameter.parameterName),
                    X64Register.argumentRegs[pushParameter.parameterIndex]));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, getLocalVariableStackOffset(pushParameter.parameterName)));
    }

    @Override
    public X64Builder visit(PopParameter popParameter, X64Builder x64builder) {
        if (popParameter.parameterIndex < N_ARG_REGISTERS)
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.movq,
                            X64Register.argumentRegs[popParameter.parameterIndex],
                            getLocalVariableStackOffset(popParameter.parameterName)));
        else
            return x64builder
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            getRbpCalledArgument((popParameter.parameterIndex + 1 - N_ARG_REGISTERS) * 8 + 8),
                            X64Register.R12))
                    .addLine(x64InstructionLine(
                            X64Instruction.movq,
                            X64Register.R12,
                            getLocalVariableStackOffset(popParameter.parameterName)));
    }

    public String getRbpCalledArgument(int index) {
        return String.format("%s(%s)", index, X64Register.RBP);
    }

    public String getRbpCallingArgument(int index) {
        return String.format("%s(%s)", index, X64Register.RBP);
    }

    public String getLocalVariableStackOffset(AbstractName name) {
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
        String fstOperandStackLoc = getLocalVariableStackOffset(twoOperandAssign.fstOperand);
        String sndOperandStackLoc = getLocalVariableStackOffset(twoOperandAssign.sndOperand);
        String dstStackLoc = getLocalVariableStackOffset(twoOperandAssign.dst);
        switch (twoOperandAssign.operator) {
            case "+":
            case "-":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getX64OperatorCode(twoOperandAssign.operator), sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            case "*":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.imulq, sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "/":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, sndOperandStackLoc))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "%":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.movq, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cqto))
                        .addLine(x64InstructionLine(X64Instruction.idivq, sndOperandStackLoc))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RDX, dstStackLoc));
            case "||":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.or, sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            case "&&":
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.and, sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            case "==":
            case "!=":
            case "<":
            case ">":
            case "<=":
            case ">=":
                lastComparisonOperator = twoOperandAssign.operator;
                return x64builder
                        .addLine(x64InstructionLine(X64Instruction.mov, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmp, sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(twoOperandAssign.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(X64Instruction.jmp, x64Label(unconditionalJump.goToLabel)));
    }

    @Override
    public X64Builder visit(DataSectionAllocation dataSectionAllocation, X64Builder x64builder) {
        globalsDataSection.add(dataSectionAllocation);
        globals.add(dataSectionAllocation.variableName);
        return x64builder.addLine(new X64Code(dataSectionAllocation.toString()));
    }
}


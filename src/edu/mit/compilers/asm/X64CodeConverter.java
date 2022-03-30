package edu.mit.compilers.asm;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.stream.Collectors;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    final Set<AbstractName> globals = new HashSet<>();
    final Stack<MethodBegin> callStack = new Stack<>();
    public final int WORD_SIZE = 8;
    public final int MAXIMUM_NUMBER_OF_REG_ARGUMENTS = 6;
    public String lastComparisonOperator = null;

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
        return x64builder.addLine(
                x64InstructionLine("j" + lastComparisonOperator.substring(2), "." + jumpIfFalse.trueLabel.label));
    }

    @Override
    public X64Builder visit(Label label, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(label.label + ":\n"));
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
        return new X64Code("." + label.label + ":");
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

    @Override
    public X64Builder visit(MethodBegin methodBegin, X64Builder x64builder) {
        if (!textAdded) {
            x64builder = x64builder.addLine(new X64Code(".text"));
            textAdded = true;
        }

        final String methodName = methodBegin.methodDefinition.methodName.id;
        int stackOffsetIndex = 16;

        for (final AbstractName variableName : methodBegin.getLocals()) {
            methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
            stackOffsetIndex += 16;
        }

        if (methodName.equals("main"))
            x64builder = x64builder.addLine(new X64Code(".globl main"));
        callStack.push(methodBegin);
        return saveBaseAndStackPointer(x64builder.addLine(new X64Code(methodName + ":")))
                .addLine(x64InstructionLine(X64Instruction.subq, methodBegin.sizeOfLocals, X64Register.RSP));
    }

    @Override
    public X64Builder visit(MethodCall methodCall, X64Builder x64builder) {
        if (methodCall
                .getResultLocation()
                .isPresent())
            return x64builder
                    .addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()))
                    .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, "8(%rbp)"));
        return x64builder.addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()));
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
                    return x64builder.addLine(x64InstructionLine(X64Instruction.mov, sourceStackLocation, destStackLocation))
                                    .addLine(x64InstructionLine(X64Instruction.not, destStackLocation));

            case "-":
                    return x64builder.addLine(x64InstructionLine(X64Instruction.mov, sourceStackLocation, destStackLocation))
                                .addLine(x64InstructionLine(X64Instruction.not, destStackLocation));
            case "--":
                    return x64builder.addLine(x64InstructionLine(X64Instruction.mov, sourceStackLocation, destStackLocation))
                                .addLine(x64InstructionLine(X64Instruction.dec, destStackLocation));
            case "++":
                    return x64builder.addLine(x64InstructionLine(X64Instruction.mov, sourceStackLocation, destStackLocation))
                                .addLine(x64InstructionLine(X64Instruction.inc, destStackLocation));
                    
            default: return null;}
    }

    @Override
    public X64Builder visit(PopParameter popParameter, X64Builder x64builder) {
        if (popParameter.parameterIndex < MAXIMUM_NUMBER_OF_REG_ARGUMENTS)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, X64Register.argumentRegs[popParameter.parameterIndex], getLocalVariableStackOffset(popParameter.parameterName)));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, getRbpArgumentIndex(16 + WORD_SIZE * (MAXIMUM_NUMBER_OF_REG_ARGUMENTS - popParameter.parameterIndex)), getLocalVariableStackOffset(popParameter.parameterName)));
    }

    public String getRbpArgumentIndex(int index) {
        return String.format("$%s(%s)", index, X64Register.RBP);
    }

    public String getLocalVariableStackOffset(AbstractName name) {
        if (globals.contains(name) || name instanceof StringConstantName || name instanceof ConstantName)
            return name.toString();
        return String.format("-%s(%s)", callStack.peek().nameToStackOffset.get(name.toString()), X64Register.RBP);
    }

    @Override
    public X64Builder visit(PushParameter pushParameter, X64Builder x64builder) {
        if (pushParameter.parameterIndex < MAXIMUM_NUMBER_OF_REG_ARGUMENTS)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, getLocalVariableStackOffset(pushParameter.parameterName), X64Register.argumentRegs[pushParameter.parameterIndex]));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, getLocalVariableStackOffset(pushParameter.parameterName), getRbpArgumentIndex(16 + WORD_SIZE * (MAXIMUM_NUMBER_OF_REG_ARGUMENTS - pushParameter.parameterIndex))));
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
                        .addLine(x64InstructionLine(X64Instruction.mov, sndOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.cmp, fstOperandStackLoc, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.getCorrectComparisonSetInstruction(twoOperandAssign.operator), X64Register.al))
                        .addLine(x64InstructionLine(X64Instruction.movzbq, X64Register.al, X64Register.RAX))
                        .addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, dstStackLoc));
            default:
                return null;
        }
    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, X64Builder x64builder) {
        return x64builder.addLine(x64Label(unconditionalJump.goToLabel));
    }

    @Override
    public X64Builder visit(DataSectionAllocation dataSectionAllocation, X64Builder x64builder) {
        globals.add(dataSectionAllocation.variableName);
        return x64builder.addLine(new X64Code(dataSectionAllocation.toString()));
    }
}


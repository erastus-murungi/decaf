package edu.mit.compilers.asm;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.*;
import java.util.stream.Collectors;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {

    private boolean textAdded = false;
    final Set<AbstractName> globals = new HashSet<>();
    final Stack<MethodBegin> callStack = new Stack<>();

    public X64Program convert(ThreeAddressCodeList threeAddressCodeList) {
        X64Builder x64Builder = initializeDataSection();
        final X64Builder root = x64Builder;
        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            x64Builder = threeAddressCode.accept(this, x64Builder);
            if (x64Builder == null)
                break;
        }
        System.out.println(root.build());
        return null;
    }

    private X64Builder initializeDataSection() {
        X64Builder x64Builder = new X64Builder();
        x64Builder.addLine(new X64Code(".data"));
        return x64Builder;
    }

    private String stackOffset(AbstractName name) {
        if (globals.contains(name))
            return name.toString();
        return getLocalVariableStackOffset(name);
    }

    @Override
    public X64Builder visit(CopyInstruction copyInstruction, X64Builder x64builder) {
        if (copyInstruction.dst instanceof TemporaryName && copyInstruction.src instanceof ConstantName)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, copyInstruction.src, stackOffset(copyInstruction.dst)));
        return x64builder;
    }

    @Override
    public X64Builder visit(JumpIfFalse jumpIfFalse, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(Label label, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(label.label + ":\n"));
    }

    public String tabSpaced(Object s) {
        return "\t" + s + "\t";
    }

    public String tabIndented(Object s) {
        return "\t" + s;
    }

    public String commaSeparated(Object... s) {
        return Arrays.stream(s).map(Object::toString).collect(Collectors.joining(", "));
    }

    public X64Code x64InstructionLine(Object instruction, Object... args) {
        return new X64Code(tabSpaced(instruction) + commaSeparated(args));
    }


    private X64Builder saveBaseAndStackPointer(X64Builder x64Builder) {
        return x64Builder
                .addLine(
                        x64InstructionLine(X64Instruction.pushq, X64Register.RBP))
                .addLine(
                        x64InstructionLine(X64Instruction.movq, X64Register.RSP, X64Register.RBP));
    }

    private X64Builder callerSave(X64Builder x64Builder) {
//        for (X64Register calleeSavedRegister: used) {
//            x64Builder.addLine(new X64Code(tabSpaced(X64Instruction.pushq) + calleeSavedRegister));
//        }
        return x64Builder;
    }
    @Override
    public X64Builder visit(MethodBegin methodBegin, X64Builder x64builder) {
        if (!textAdded) {
            x64builder = x64builder.addLine(new X64Code(".text"));
            textAdded = true;
        }
        String methodName = methodBegin.methodDefinition.methodName.id;
        int stackOffsetIndex = 0;
        for (AbstractName variableName: methodBegin.getLocals()) {
            methodBegin.nameToStackOffset.put(variableName.toString(), stackOffsetIndex);
            stackOffsetIndex += variableName.size;
        }
        if (methodName.equals("main"))
            x64builder = x64builder.addLine(new X64Code(".globl main"));
        callStack.push(methodBegin);
        return saveBaseAndStackPointer(x64builder.addLine(new X64Code(methodName + ":")))
                .addLine(x64InstructionLine(X64Instruction.subq, methodBegin.sizeOfLocals, X64Register.RSP));
    }

    @Override
    public X64Builder visit(MethodCall methodCall, X64Builder x64builder) {
        if (methodCall.getResultLocation().isPresent())
            return x64builder.addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName())).addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, "8(%rbp)"));
        return x64builder.addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()));
    }

    @Override
    public X64Builder visit(MethodEnd methodEnd, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine("leave")).addLine(new X64Code(tabIndented("ret")));
    }

    @Override
    public X64Builder visit(MethodReturn methodReturn, X64Builder x64builder) {
        if (methodReturn.getReturnAddress().isPresent())
            x64builder = x64builder.addLine(x64InstructionLine(X64Instruction.movq, methodReturn.getReturnAddress(), X64Register.RAX));
        return x64builder.addLine(x64InstructionLine(x64InstructionLine("leave"))).addLine(x64InstructionLine("ret")).addLine(x64InstructionLine("\n"));
    }

    @Override
    public X64Builder visit(OneOperandAssign oneOperandAssign, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(PopParameter popParameter, X64Builder x64builder) {
        return x64builder;
    }

    public String getRbpArgumentIndex(int index) {
        return String.format("$%s(%s)", index, X64Register.RBP);
    }

    public String getLocalVariableStackOffset(AbstractName name) {
        return String.format("%s(%s)", callStack.peek().nameToStackOffset.get(name.toString()), X64Register.RSP);
    }

    public String getArgumentStackOffset(int parameterIndex) {
        return String.format("-%s(%s)", (parameterIndex + 1) * 8, X64Register.RBP);
    }

    public String getParameterStackOffset(int parameterIndex) {
        return String.format("-%s(%s)", (parameterIndex + 1) * 8, X64Register.RSP);
    }

    @Override
    public X64Builder visit(PushParameter pushParameter, X64Builder x64builder) {
        if (pushParameter.parameterIndex < 6)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, getLocalVariableStackOffset(pushParameter.parameterName), X64Register.argumentRegs[pushParameter.parameterIndex]));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, getLocalVariableStackOffset(pushParameter.parameterName), getRbpArgumentIndex(16 + 8*(6 - pushParameter.parameterIndex))));

    }

    @Override
    public X64Builder visit(StringLiteralStackAllocation stringLiteralStackAllocation, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(stringLiteralStackAllocation.getASM()));
    }

    @Override
    public X64Builder visit(TwoOperandAssign twoOperandAssign, X64Builder x64builder) {
        String fstOperandStackLoc = getRbpArgumentIndex(callStack.peek().nameToStackOffset.get(twoOperandAssign.fstOperand));
        String sndOperandStackLoc = getRbpArgumentIndex(callStack.peek().nameToStackOffset.get(twoOperandAssign.sndOperand));
        String dstStackLoc = getRbpArgumentIndex(callStack.peek().nameToStackOffset.get(twoOperandAssign.dst));
        switch (twoOperandAssign.operator) {
            case "+": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("add", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            case "-": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("sub", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            case "*": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("imul", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "/": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("idivq", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            // TODO: cheatsheet says that %rdx:%rax is divided by S (source) but we are going to assume just %rax for now
            case "%": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("idivq", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RDX, dstStackLoc));
            case "||": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("or", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            case "&&": return x64builder.addLine(x64InstructionLine("mov", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("and", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("mov", X64Register.RAX, dstStackLoc));
            case "==": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("sete", X64Register.RAX, dstStackLoc));
            case "!=": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("setne", X64Register.RAX, dstStackLoc));
            case "<": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("setl", X64Register.RAX, dstStackLoc));
            case ">": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("setg", X64Register.RAX, dstStackLoc));
            case "<=": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("setle", X64Register.RAX, dstStackLoc));
            case ">=": return x64builder.addLine(x64InstructionLine("mov", sndOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("cmp", fstOperandStackLoc, X64Register.RAX))
                    .addLine(x64InstructionLine("setge", X64Register.RAX, dstStackLoc));
            default: return null;
        }

    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, X64Builder x64builder) {
        return x64builder.addLine(x64InstructionLine(X64Instruction.jmp, "." + unconditionalJump.goToLabel));
    }

    @Override
    public X64Builder visit(DataSectionAllocation dataSectionAllocation, X64Builder x64builder) {
        globals.add(dataSectionAllocation.variableName);
        return x64builder.addLine(new X64Code(dataSectionAllocation.toString()));
    }
}

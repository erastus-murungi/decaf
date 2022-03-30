package edu.mit.compilers.asm;

import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.*;
import java.util.stream.Collectors;

public class X64CodeConverter implements ThreeAddressCodeVisitor<X64Builder, X64Builder> {
    boolean textAdded = false;

    HashMap<String, Set<X64Register>> usedRegisters = new HashMap<>();
    final Set<AbstractName> globals = new HashSet<>();
    final Stack<MethodBegin> callStack = new Stack<>();

    public X64Register findUnUsedRegister(String method) {
        Set<X64Register> registers = usedRegisters.get(method);
        for (X64Register x64Register: X64Register.availableRegs) {
            if (!registers.contains(x64Register))
                return x64Register;
        }
        return null;
    }

    public X64Program convert(ThreeAddressCodeList threeAddressCodeList, SymbolTable symbolTable) {
        X64Builder x64Builder = initializeDataSection();
        final X64Builder root = x64Builder;
        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            x64Builder = threeAddressCode.accept(this, symbolTable, x64Builder);
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
    public X64Builder visit(CopyInstruction copyInstruction, SymbolTable symbolTable, X64Builder x64builder) {
        if (copyInstruction.dst instanceof TemporaryName && copyInstruction.src instanceof ConstantName)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, copyInstruction.src, stackOffset(copyInstruction.dst)));
        return x64builder;
    }

    @Override
    public X64Builder visit(JumpIfFalse jumpIfFalse, SymbolTable symbolTable, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(Label label, SymbolTable symbolTable, X64Builder x64builder) {
        return null;
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
    public X64Builder visit(MethodBegin methodBegin, SymbolTable symbolTable, X64Builder x64builder) {
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
    public X64Builder visit(MethodCall methodCall, SymbolTable symbolTable, X64Builder x64builder) {
        if (methodCall.getResultLocation().isPresent())
            return x64builder.addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName())).addLine(x64InstructionLine(X64Instruction.movq, X64Register.RAX, "8(%rbp)"));
        return x64builder.addLine(x64InstructionLine(X64Instruction.call, methodCall.getMethodName()));
    }

    @Override
    public X64Builder visit(MethodEnd methodEnd, SymbolTable symbolTable, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(tabIndented("leave"))).addLine(new X64Code(tabIndented("ret")));
    }

    @Override
    public X64Builder visit(MethodReturn methodReturn, SymbolTable symbolTable, X64Builder x64builder) {
        if (methodReturn.getReturnAddress().isPresent())
            x64builder = x64builder.addLine(x64InstructionLine(X64Instruction.movq, methodReturn.getReturnAddress(), X64Register.RAX));
        return x64builder.addLine(x64InstructionLine(x64InstructionLine("leave"))).addLine(x64InstructionLine("ret")).addLine(new X64Code("\n"));
    }

    @Override
    public X64Builder visit(OneOperandAssign oneOperandAssign, SymbolTable symbolTable, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(PopParameter popParameter, SymbolTable symbolTable, X64Builder x64builder) {
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
    public X64Builder visit(PushParameter pushParameter, SymbolTable symbolTable, X64Builder x64builder) {
        if (pushParameter.parameterIndex < 6)
            return x64builder.addLine(x64InstructionLine(X64Instruction.movq, getLocalVariableStackOffset(pushParameter.parameterName), X64Register.argumentRegs[pushParameter.parameterIndex]));
        else
            return x64builder.addLine(x64InstructionLine(X64Instruction.pushq, getLocalVariableStackOffset(pushParameter.parameterName), getRbpArgumentIndex(16 + 8*(6 - pushParameter.parameterIndex))));

    }

    @Override
    public X64Builder visit(StringLiteralStackAllocation stringLiteralStackAllocation, SymbolTable symbolTable, X64Builder x64builder) {
        return x64builder.addLine(new X64Code(stringLiteralStackAllocation.getASM()));
    }

    @Override
    public X64Builder visit(TwoOperandAssign twoOperandAssign, SymbolTable symbolTable, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(UnconditionalJump unconditionalJump, SymbolTable symbolTable, X64Builder x64builder) {
        return null;
    }

    @Override
    public X64Builder visit(DataSectionAllocation dataSectionAllocation, SymbolTable currentSymbolTable, X64Builder x64builder) {
        globals.add(dataSectionAllocation.variableName);
        return x64builder.addLine(new X64Code(dataSectionAllocation.toString()));
    }
}

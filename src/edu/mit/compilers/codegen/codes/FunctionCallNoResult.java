package edu.mit.compilers.codegen.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.dataflow.operand.MethodCallOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class FunctionCallNoResult extends HasOperand implements FunctionCall {
    private final Stack<IrValue> arguments;

    public FunctionCallNoResult(MethodCall methodCall, Stack<IrValue> arguments, String comment) {
        super(methodCall, comment);
        this.arguments = arguments;
    }

    public Stack<IrValue> getArguments() {
        return arguments;
    }

    @Override
    public MethodCall getMethod() {
        return (MethodCall) source;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return new ArrayList<>(arguments);
    }

    @Override
    public Operand getOperand() {
        return new MethodCallOperand(this);
    }

    @Override
    public List<IrValue> getOperandValues() {
        return new ArrayList<>(arguments);
    }

    public boolean replaceValue(IrValue oldName, IrValue newName) {
        var replaced = false;
        int i = 0;
        for (IrValue irValue : arguments) {
            if (irValue.equals(oldName)) {
                arguments.set(i, newName);
                replaced = true;
            }
            i += 1;
        }
        return replaced;
    }

    @Override
    public String toString() {
        var args = arguments.stream().map(IrValue::repr).collect(Collectors.joining(", "));
        return String.format("%s%s %s @%s(%s) %s%s", DOUBLE_INDENT, "call", getMethodReturnType(), getMethodName(), args, DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public String syntaxHighlightedToString() {
        var callString = Utils.coloredPrint("call", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        var args = arguments.stream().map(IrValue::repr).collect(Collectors.joining(", "));
        return String.format("%s%s %s @%s(%s) %s%s", DOUBLE_INDENT, callString, getMethodReturnType(), getMethodName(), args, DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new FunctionCallNoResult((MethodCall) source, arguments, getComment().orElse(null));
    }
}
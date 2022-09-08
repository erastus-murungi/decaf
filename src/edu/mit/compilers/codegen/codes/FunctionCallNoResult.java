package edu.mit.compilers.codegen.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.MethodCallOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class FunctionCallNoResult extends HasOperand implements FunctionCall {
    private final Stack<Value> arguments;

    public FunctionCallNoResult(MethodCall methodCall, Stack<Value> arguments, String comment) {
        super(methodCall, comment);
        this.arguments = arguments;
    }

    public Stack<Value> getArguments() {
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
    public List<Value> getAllValues() {
        return new ArrayList<>(arguments);
    }

    @Override
    public Operand getOperand() {
        return new MethodCallOperand(this);
    }

    @Override
    public List<Value> getOperandValues() {
        return new ArrayList<>(arguments);
    }

    public boolean replaceValue(Value oldName, Value newName) {
        var replaced = false;
        int i = 0;
        for (Value value : arguments) {
            if (value.equals(oldName)) {
                arguments.set(i, newName);
                replaced = true;
            }
            i += 1;
        }
        return replaced;
    }

    @Override
    public String toString() {
        var args = arguments.stream().map(Value::repr).collect(Collectors.joining(", "));
        return String.format("%s%s %s @%s(%s) %s%s", DOUBLE_INDENT, "call", getMethodReturnType(), getMethodName(), args, DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public String syntaxHighlightedToString() {
        var callString = Utils.coloredPrint("call", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        var args = arguments.stream().map(Value::repr).collect(Collectors.joining(", "));
        return String.format("%s%s %s @%s(%s) %s%s", DOUBLE_INDENT, callString, getMethodReturnType(), getMethodName(), args, DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new FunctionCallNoResult((MethodCall) source, arguments, getComment().orElse(null));
    }
}
package edu.mit.compilers.codegen.codes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.dataflow.operand.MethodCallOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class FunctionCallWithResult extends StoreInstruction implements FunctionCall {
    private final Stack<AbstractName> arguments;

    public FunctionCallWithResult(MethodCall methodCall, AssignableName resultLocation, Stack<AbstractName> arguments, String comment) {
        super(resultLocation, methodCall, comment);
        this.arguments = arguments;
    }

    public Stack<AbstractName> getArguments() {
        return arguments;
    }
    @Override
    public MethodCall getMethod() {
        return (MethodCall) source;
    }

    @Override
    public String toString() {
        return String.format("%s%s = %s %s %s %s%s", DOUBLE_INDENT, getStore(), "call", getMethodReturnType(), getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        var args = new ArrayList<>(getArguments());
        args.add(getStore());
        return args;
    }

    @Override
    public String repr() {
        var callString =  Utils.coloredPrint("call", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        var args = arguments.stream().map(AbstractName::repr).collect(Collectors.joining(", "));
        return String.format("%s%s: %s = %s @%s(%s) %s%s", DOUBLE_INDENT, getStore().repr(), getMethodReturnType(), callString, getMethodName(), args, DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new FunctionCallWithResult((MethodCall) source, getStore(), arguments, getComment().orElse(null));
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (getStore() instanceof MemoryAddressName)
            return Optional.empty();
        return Optional.of(new MethodCallOperand(this));
    }

    @Override
    public Operand getOperand() {
        return new MethodCallOperand(this);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return new ArrayList<>(arguments);
    }

    @Override
    public boolean replace(AbstractName oldName, AbstractName newName) {
        var replaced = false;
        int i = 0;
        for (AbstractName abstractName: arguments) {
            if (abstractName.equals(oldName)) {
                arguments.set(i, newName);
                replaced = true;
            }
            i += 1;
        }
        return replaced;
    }
}
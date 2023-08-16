package decaf.ir.codes;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.analysis.syntax.ast.MethodCall;
import decaf.ir.dataflow.operand.MethodCallOperand;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.names.IrAssignable;
import decaf.ir.names.IrMemoryAddress;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;
import decaf.synthesis.asm.AsmWriter;

public class FunctionCallWithResult extends StoreInstruction implements FunctionCall {
  private final Stack<IrValue> arguments;

  public FunctionCallWithResult(
      MethodCall methodCall,
      IrAssignable resultLocation,
      Stack<IrValue> arguments,
      String comment
  ) {
    super(
        resultLocation,
        methodCall,
        comment
    );
    this.arguments = arguments;
  }

  public Stack<IrValue> getArguments() {
    return arguments;
  }

  @Override
  public MethodCall getMethod() {
    return (MethodCall) getSource();
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    var args = new ArrayList<>(getArguments());
    args.add(getDestination());
    return args;
  }

  @Override
  public String syntaxHighlightedToString() {
    var callString = Utils.coloredPrint(
        "call",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    var args = arguments.stream()
                        .map(IrValue::toString)
                        .collect(Collectors.joining(", "));
    return String.format(
        "%s%s: %s = %s @%s(%s) %s%s",
        DOUBLE_INDENT,
        getDestination(),
        getMethodReturnType(),
        callString,
        getMethodName(),
        args,
        DOUBLE_INDENT,
        getComment().isPresent() ? " # " + getComment().get(): ""
    );
  }

  @Override
  public String toString() {
    var args = arguments.stream()
                        .map(IrValue::toString)
                        .collect(Collectors.joining(", "));
    return String.format(
        "%s%s: %s = %s @%s(%s) %s%s",
        DOUBLE_INDENT,
        getDestination(),
        getMethodReturnType(),
        "call",
        getMethodName(),
        args,
        DOUBLE_INDENT,
        getComment().isPresent() ? " # " + getComment().get(): ""
    );
  }

  @Override
  public Instruction copy() {
    return new FunctionCallWithResult(
        (MethodCall) getSource(),
        getDestination(),
        arguments,
        getComment().orElse(null)
    );
  }

  @Override
  public Optional<Operand> getOperandNoArray() {
    if (getDestination() instanceof IrMemoryAddress)
      return Optional.empty();
    return Optional.of(new MethodCallOperand(this));
  }

  @Override
  public Operand getOperand() {
    return new MethodCallOperand(this);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    return new ArrayList<>(arguments);
  }

  @Override
  public boolean replaceValue(
      IrValue oldVariable,
      IrValue replacer
  ) {
    var replaced = false;
    int i = 0;
    for (IrValue irValue : arguments) {
      if (irValue.equals(oldVariable)) {
        arguments.set(
            i,
            replacer
        );
        replaced = true;
      }
      i += 1;
    }
    return replaced;
  }
}
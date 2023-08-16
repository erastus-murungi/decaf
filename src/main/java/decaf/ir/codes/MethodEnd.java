package decaf.ir.codes;


import java.util.Collections;
import java.util.List;

import decaf.synthesis.asm.AsmWriter;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.ir.names.IrValue;

public class MethodEnd extends Instruction {
  public MethodEnd(MethodDefinition methodDefinition) {
    super(methodDefinition);
  }

  public boolean isMain() {
    return methodName().equals("main");
  }

  public String methodName() {
    return ((MethodDefinition) getSource()).getMethodName()
                                           .getLabel();
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return Collections.emptyList();
  }

  @Override
  public Instruction copy() {
    return new MethodEnd((MethodDefinition) getSource());
  }

  @Override
  public String toString() {
    return "}\n";
  }

  @Override
  public String syntaxHighlightedToString() {
    return toString();
  }

}

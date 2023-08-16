package decaf.synthesis.asm;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import decaf.synthesis.asm.instructions.X64Instruction;
import decaf.synthesis.asm.instructions.X86MetaData;

public class X86Program extends ArrayList<X64Instruction> {
  public X86Program() {
  }

  public void addPrologue(List<X86MetaData> prologue) {
    addAll(prologue);
  }

  public void addEpilogue(List<X86MetaData> epilogue) {
    addAll(epilogue);
  }

  public void addMethod(X86Method x86Method) {
    addAll(x86Method);
  }

  @Override
  public String toString() {
    return stream().map(X64Instruction::toString)
                   .collect(Collectors.joining("\n")) + "\n";
  }
}

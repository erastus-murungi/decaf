package decaf.asm.operands;


import java.util.Collections;
import java.util.List;

import decaf.asm.X86Register;
import decaf.cfg.BasicBlock;

public class X64JumpTargetOperand extends X86Value {
  private final String target;

  public X64JumpTargetOperand(BasicBlock target) {
    super(null);
    this.target = target.getInstructionList()
                        .getLabelForAsm();
  }

  @Override
  public String toString() {
    return String.format(
        ".%s",
        target
    );
  }

  @Override
  public List<X86Register> registersInUse() {
    return Collections.emptyList();
  }
}

package decaf.ir.dataflow.operand;


import java.util.List;
import java.util.Objects;

import decaf.ir.codes.StoreInstruction;
import decaf.ir.names.IrValue;
import decaf.ir.ssa.Phi;

public class PhiOperand extends Operand {
  private final Phi phi;

  public PhiOperand(Phi phi) {
    this.phi = phi;
  }

  public Phi getPhi() {
    return phi;
  }

  @Override
  public boolean contains(IrValue comp) {
    return phi.genOperandIrValuesSurface()
              .contains(comp);
  }

  @Override
  public List<IrValue> getNames() {
    return phi.genOperandIrValuesSurface();
  }

  @Override
  public boolean isContainedIn(StoreInstruction storeInstruction) {
    if (storeInstruction instanceof Phi) {
      return new PhiOperand((Phi) storeInstruction).equals(this);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PhiOperand that = (PhiOperand) o;
    return Objects.equals(getPhi().genOperandIrValuesSurface(),
                          that.getPhi()
                              .genOperandIrValuesSurface()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getPhi().genOperandIrValuesSurface());
  }
}

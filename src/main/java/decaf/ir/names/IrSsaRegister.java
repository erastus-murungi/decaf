package decaf.ir.names;


import java.util.List;
import java.util.Objects;

import decaf.analysis.syntax.ast.Type;
import decaf.ir.IndexManager;

public class IrSsaRegister extends IrRegister implements IrRegisterAllocatable {
  protected Integer versionNumber;

  IrSsaRegister(
      String label,
      Type type,
      Integer versionNumber
  ) {
    super(
        type,
        label
    );
    this.versionNumber = versionNumber;
  }

  public IrSsaRegister(
      String label,
      Type type
  ) {
    this(
        label,
        type,
        null
    );
  }

  protected IrSsaRegister(
      long index,
      Type type
  ) {
    this(
        String.format(
            "%%%d",
            index
        ),
        type,
        null
    );
  }

  public static IrSsaRegister gen(Type type) {
    return new IrSsaRegister(
        IndexManager.genRegisterIndex(),
        type
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel());
  }

  public void renameForSsa(int versionNumber) {
    this.versionNumber = versionNumber;
  }

  public void renameForSsa(IrSsaRegister irSsaRegister) {
    if (getType() != irSsaRegister.getType())
      throw new IllegalArgumentException("type: " + getType() + "\nrename type: " + irSsaRegister.getType());
    this.label = irSsaRegister.label;
    this.versionNumber = irSsaRegister.versionNumber;
  }

  public Integer getVersionNumber() {
    return versionNumber;
  }

  public String getLabel() {
    if (versionNumber != null)
      return String.format(
          "%s.%d",
          label,
          versionNumber
      );
    return label;
  }

  @Override
  public IrSsaRegister copy() {
    return new IrSsaRegister(
        label,
        type,
        versionNumber
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IrSsaRegister irSsaRegister)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(
        getLabel(),
        irSsaRegister.getLabel()
    );
  }

  @Override
  public List<IrValue> get() {
    return List.of(this);
  }
}

package decaf.shared.descriptors;

import decaf.shared.types.Type;

public class ValueDescriptor extends Descriptor {
  private final boolean isFormalArgument;
  public ValueDescriptor(
      Type type,
      boolean isFormalArgument
  ) {
    super(type);
    this.isFormalArgument = isFormalArgument;
  }

  public boolean isFormalArgument() {
    return isFormalArgument;
  }
}

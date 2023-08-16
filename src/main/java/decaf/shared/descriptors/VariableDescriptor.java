package decaf.shared.descriptors;

import decaf.analysis.syntax.ast.Type;

public class VariableDescriptor extends Descriptor {
  public VariableDescriptor(
      String id,
      Type type
  ) {
    super(
        type,
        id
    );
  }
}

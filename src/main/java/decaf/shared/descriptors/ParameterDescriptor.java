package decaf.shared.descriptors;

import decaf.analysis.syntax.ast.Type;

public class ParameterDescriptor extends Descriptor {
  public ParameterDescriptor(
      String id,
      Type type
  ) {
    super(
        type,
        id
    );
  }
}


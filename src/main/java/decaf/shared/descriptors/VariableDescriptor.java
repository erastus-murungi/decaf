package decaf.shared.descriptors;

import decaf.analysis.syntax.ast.Name;
import decaf.analysis.syntax.ast.Type;

public class VariableDescriptor extends Descriptor {
  public final Name name;

  public VariableDescriptor(
      String id,
      Type type,
      Name name
  ) {
    super(
        type,
        id
    );
    this.name = name;
  }
}

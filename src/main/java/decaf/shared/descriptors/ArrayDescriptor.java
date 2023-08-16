package decaf.shared.descriptors;

import decaf.analysis.syntax.ast.Type;

/**
 * An Array Descriptor is just a tuple of (Array size, String id, Array Element Type)
 */
public class ArrayDescriptor extends Descriptor {
  public final Long size;
  public ArrayDescriptor(
      Long size,
      Type type
  ) {
    super(
        type
    );
    this.size = size;
  }
}
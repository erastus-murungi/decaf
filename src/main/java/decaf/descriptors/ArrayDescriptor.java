package decaf.descriptors;

import decaf.ast.Array;
import decaf.ast.Type;

/**
 * An Array Descriptor is just a tuple of (Array size, String id, Array Element Type)
 */
public class ArrayDescriptor extends Descriptor {
  public final Long size;
  public final Array array;

  public ArrayDescriptor(
      String id,
      Long size,
      Type type,
      Array array
  ) {
    super(
        type,
        id
    );
    this.size = size;
    this.array = array;
  }
}

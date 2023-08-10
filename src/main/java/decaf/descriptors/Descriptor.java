package decaf.descriptors;

import decaf.ast.Type;

/**
 * Every descriptor should at least have a type and an identifier
 */
public abstract class Descriptor {
  public Type type;
  public String id;

  public Descriptor(
      Type type,
      String id
  ) {
    this.type = type;
    this.id = id;
  }
}

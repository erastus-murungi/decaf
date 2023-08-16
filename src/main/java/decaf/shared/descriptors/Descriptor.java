package decaf.shared.descriptors;

import decaf.analysis.syntax.ast.Type;

/**
 * Every descriptor should at least have a type and an identifier
 */
public abstract class Descriptor {
  public Type type;

  public Descriptor(
      Type type
  ) {
    this.type = type;
  }
}

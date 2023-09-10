package decaf.shared.descriptors;

import static com.google.common.base.Preconditions.checkNotNull;

import org.jetbrains.annotations.NotNull;

import decaf.shared.types.Type;

/**
 * Every descriptor should at least have a type and an identifier
 */
public abstract class Descriptor {
  @NotNull
  private final Type type;

  public Descriptor(@NotNull Type type) {
    this.type = type;
  }

  public boolean typeIs(@NotNull Type type) {
    checkNotNull(type);
    return this.type == type;
  }

  public boolean typeIsAnyOf(@NotNull Type... types) {
    checkNotNull(types);
    for (Type type : types) {
      if (typeIs(type)) {
        return true;
      }
    }
    return false;
  }

  public boolean typeIsNotAnyOf(@NotNull Type... types) {
    checkNotNull(types);
    return !typeIsAnyOf(types);
  }

  public @NotNull Type getType() {
    return type;
  }
}

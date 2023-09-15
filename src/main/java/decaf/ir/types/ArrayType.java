package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ArrayType extends Type {
  @NotNull
  private final Type containedType;
  private final int length;

  private static final Map<Type, Map<Integer, ArrayType>> typesCache;

  static {
    typesCache = new HashMap<>();
  }

  protected ArrayType(@NotNull Type containedType, int length) {
    super(TypeID.Array);
    this.containedType = containedType;
    this.length = length;
  }

  public static ArrayType get(@NotNull Type containedType, int length) {
    if (!typesCache.containsKey(containedType)) {
      typesCache.put(containedType, new HashMap<>());
    }
    Map<Integer, ArrayType> lengthMap = typesCache.get(containedType);
    if (!lengthMap.containsKey(length)) {
      lengthMap.put(length, new ArrayType(containedType, length));
    }
    return lengthMap.get(length);
  }

  public @NotNull Type getContainedType() {
    return containedType;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return containedType.toString() + "[" + length + "]";
  }
}

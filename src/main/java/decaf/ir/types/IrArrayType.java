package decaf.ir.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class IrArrayType extends IrDerivedType {
  @NotNull
  private final IrType containedType;
  private final int length;

  private static final Map<IrType, Map<Integer, IrArrayType>> typesCache;

  static {
    typesCache = new HashMap<>();
  }

  protected IrArrayType(@NotNull IrType containedType, int length) {
    super();
    this.containedType = containedType;
    this.length = length;
  }

  public static IrArrayType get(@NotNull IrType containedType, int length) {
    if (!typesCache.containsKey(containedType)) {
      typesCache.put(containedType, new HashMap<>());
    }
    var lengthMap = typesCache.get(containedType);
    if (!lengthMap.containsKey(length)) {
      lengthMap.put(length, new IrArrayType(containedType, length));
    }
    return lengthMap.get(length);
  }

  public @NotNull IrType getContainedType() {
    return containedType;
  }

  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return containedType.toString() + "[" + length + "]";
  }

  @Override
  public @NotNull String prettyPrint() {
    return String.format("[%s x %s]", length, containedType.prettyPrint());
  }

  @Override
  public int getBitWidth() {
    return length * containedType.getBitWidth();
  }

  @Override
  public boolean isFirstClassType() {
    return false;
  }
}

package decaf.analysis.syntax.ast.types;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ArrayType extends Type {
    @NotNull
    private final Type containedType;
    private final long numElements;

    static private final HashMap<Type, HashMap<Long, ArrayType>> arrayTypesCache = new HashMap<>();

    public static @NotNull ArrayType get(@NotNull Type containedType, long numElements) {
        var numElementsCache = arrayTypesCache.get(containedType);
        if (numElementsCache != null) {
            var cachedType = numElementsCache.get(numElements);
            if (cachedType != null) {
                return cachedType;
            }
        } else {
            numElementsCache = new HashMap<>();
            arrayTypesCache.put(containedType, numElementsCache);
        }
        var arrayType = new ArrayType(containedType, numElements);
        numElementsCache.put(numElements, arrayType);
        return arrayType;
    }

    protected ArrayType(@NotNull Type containedType, long numElements) {
        super(TypeId.Array);
        this.containedType = containedType;
        this.numElements = numElements;
    }

    public long getNumElements() {
        return numElements;
    }

    public @NotNull Type getContainedType() {
        return containedType;
    }

    @Override
    public String toString() {
        return containedType + "[" + numElements + "]";
    }
}

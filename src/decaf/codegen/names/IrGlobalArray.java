package decaf.codegen.names;

import java.util.List;
import java.util.Objects;

import decaf.ast.Type;

public class IrGlobalArray extends IrValue implements IrRegisterAllocatable, IrGlobal {

    public IrGlobalArray(String label, Type type) {
        super(
            type,
            label
        );
    }

    @Override
    public IrGlobalArray copy() {
        return new IrGlobalArray(label, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IrGlobalArray irGlobalArray)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), irGlobalArray.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getLabel());
    }

    @Override
    public List<IrValue> get() {
        return List.of(this);
    }
}

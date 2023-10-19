package decaf.ir.types;

public abstract class IrDerivedType extends IrType {
    @Override
    public boolean isFirstClassType() {
        return false;
    }
}

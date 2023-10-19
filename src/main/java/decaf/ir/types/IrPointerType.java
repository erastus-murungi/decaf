package decaf.ir.types;

public abstract class IrPointerType extends IrPrimitiveType {
    @Override
    public boolean isFirstClassType() {
        return true;
    }
}

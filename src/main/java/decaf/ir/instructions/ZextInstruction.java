package decaf.ir.instructions;

import decaf.analysis.syntax.ast.types.Type;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZextInstruction extends Instruction {
    public ZextInstruction(@NotNull IrType frm, IrType to) {
        super(to);
        checkTypes();
    }

    private void checkTypes() {
        // The ‘zext’ instruction takes a value to cast, and a type to cast it to.
        // Both types must be of integer types, or vectors of the same number of integers.
        // The bit size of the value must be smaller than the bit size of the destination type, ty2.
        // The result is a value of type ty2.
    }

    @Override
    public String prettyPrint() {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String prettyPrintColored() {
        return null;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }
}

package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrFunctionType;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IrFunction extends Instruction {
    public IrFunction(@NotNull IrFunctionType irFunctionType) {
        super(irFunctionType);
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return null;
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor,
                                                           ArgumentType argument) {
        return visitor.visit(this, argument);
    }
}

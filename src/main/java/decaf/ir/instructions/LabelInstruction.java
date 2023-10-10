package decaf.ir.instructions;

import decaf.ir.Counter;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LabelInstruction extends Instruction {
    @NotNull private String id;

    private LabelInstruction(@NotNull String id) {
        super(IrType.getLabelType());
        this.id = id;
    }

    public static @NotNull LabelInstruction fromString(@NotNull String id) {
        return new LabelInstruction(id);
    }

    public static @NotNull LabelInstruction create() {
        return new LabelInstruction(String.format("L%s", Counter.getInstance().nextId()));
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
    public <T> boolean isWellFormed(T neededContext) throws InstructionMalformed {
        return false;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        return Collections.emptyList();
    }
}

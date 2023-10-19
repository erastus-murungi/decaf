package decaf.ir;

import decaf.ir.instructions.Instruction;
import decaf.ir.values.IrLabel;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class BasicBlock extends LinkedList<Instruction> {
    @NotNull private final IrLabel label;

    protected BasicBlock(@NotNull IrLabel label) {
        this.label = label;
    }

    public static BasicBlock create(@NotNull IrLabel label) {
        return new BasicBlock(label);
    }

    public @NotNull IrLabel getLabel() {
        return label;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(label.prettyPrint()).append(":\n");
        for (Instruction instruction : this) {
            sb.append(instruction.toString()).append("\n");
        }
        return sb.toString();
    }
}

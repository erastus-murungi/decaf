package decaf.ir;

import decaf.ir.instructions.IrFunction;
import decaf.ir.values.IrLabel;
import decaf.shared.LinkedListSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class IrContext {
    private final LinkedListSet<IrLabel> usedLabels;
    private final LinkedListSet<String> usedFunctions = new LinkedListSet<>();

    @Nullable
    private IrFunction currentFunction;

    private IrContext() {
        usedLabels = new LinkedListSet<>();
    }

    public static IrContext create() {
        return new IrContext();
    }

    public void addLabel(@NotNull IrLabel label) {
        usedLabels.add(label);
    }

    public boolean hasLabel(@NotNull IrLabel label) {
        return usedLabels.contains(label);
    }

    public List<IrLabel> getUsedLabels() {
        return List.copyOf(usedLabels);
    }

    public void setCurrentFunction(@Nullable IrFunction currentFunction) {
        this.currentFunction = currentFunction;
    }

    public @Nullable IrFunction getCurrentFunction() {
        return currentFunction;
    }

    public @NotNull IrFunction getCurrentFunctionNonNull() {
        checkState(currentFunction != null, "Current function is null");
        return currentFunction;
    }

    public void addFunction(@NotNull String function) {
        usedFunctions.add(function);
    }

    public boolean hasFunction(@NotNull String function) {
        return usedFunctions.contains(function);
    }

    public List<String> getUsedFunctions() {
        return List.copyOf(usedFunctions);
    }
}

package decaf.ir.instructions;

import decaf.ir.types.IrLabel;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import decaf.shared.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PhiInstruction extends Instruction {
    @NotNull
    private final IrRegister destination;

    @NotNull
    private final Map<IrLabel, IrDirectValue> sources;

    protected PhiInstruction(@NotNull IrRegister destination, @NotNull Map<IrLabel, IrDirectValue> sources) {
        super(destination.getType());
        this.destination = destination;
        this.sources = sources;
    }

    public static PhiInstruction createFromMap(@NotNull IrRegister destination,
                                               @NotNull Map<IrLabel, IrDirectValue> sources) {
        return new PhiInstruction(destination, sources);
    }

    public static PhiInstruction createFromPairs(@NotNull IrRegister destination,
                                                 @NotNull Collection<Pair<IrLabel, IrDirectValue>> pairs) {
        var map = new HashMap<IrLabel, IrDirectValue>();
        pairs.forEach(pair -> map.put(pair.first(), pair.second()));
        return new PhiInstruction(destination, map);
    }

    public static PhiInstruction createFromMapGenDest(@NotNull Map<IrLabel, IrDirectValue> sources) {
        return new PhiInstruction(IrRegister.create(sources.values().iterator().next().getType()), sources);
    }

    public static PhiInstruction createFromPairsGenDest(@NotNull Collection<Pair<IrLabel, IrDirectValue>> pairs) {
        var map = new HashMap<IrLabel, IrDirectValue>();
        pairs.forEach(pair -> map.put(pair.first(), pair.second()));
        return new PhiInstruction(IrRegister.create(map.values().iterator().next().getType()), map);
    }

    @Override
    public String prettyPrint() {
        return String.format("%s = phi %s %s",
                             destination.prettyPrint(),
                             destination.getType().prettyPrint(),
                             sources.entrySet()
                                    .stream()
                                    .map(entry -> String.format("[%s, %s]",
                                                                entry.getKey().typedPrettyPrint(),
                                                                entry.getValue().prettyPrint()
                                                               ))
                                    .collect(Collectors.joining(", "))
                            );
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    @Override
    public String prettyPrintColored() {
        return null;
    }

    @Override
    public List<? extends IrValue> getUsedValues() {
        var rhs = new ArrayList<>(sources.values());
        rhs.add(destination);
        return rhs;
    }
}

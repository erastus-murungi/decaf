package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.values.IrLabel;
import decaf.ir.values.IrDirectValue;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PhiInstruction extends Instruction {
    public record PhiSource(@NotNull IrLabel label, @NotNull IrDirectValue value) { }
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
                                                 @NotNull Collection<PhiSource> phiSources) {
        var map = new HashMap<IrLabel, IrDirectValue>();
        phiSources.forEach(phiSource -> map.put(phiSource.label, phiSource.value));
        return new PhiInstruction(destination, map);
    }

    public static PhiInstruction createFromMapGenDest(@NotNull Map<IrLabel, IrDirectValue> sources) {
        return new PhiInstruction(IrRegister.create(sources.values().iterator().next().getType()), sources);
    }

    public static PhiInstruction createFromPairsGenDest(@NotNull Collection<PhiSource> phiSources) {
        var map = new HashMap<IrLabel, IrDirectValue>();
        phiSources.forEach(phiSource -> map.put(phiSource.label, phiSource.value));
        return new PhiInstruction(IrRegister.create(map.values().iterator().next().getType()), map);
    }

    @Override
    public String toString() {
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
    public List<? extends IrValue> getUsedValues() {
        var rhs = new ArrayList<>(sources.values());
        rhs.add(destination);
        return rhs;
    }

    public static PhiSource createPhiSource(@NotNull IrLabel label, @NotNull IrDirectValue value) {
        return new PhiSource(label, value);
    }

    @Override
    protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
        return visitor.visit(this, argument);
    }

    public List<PhiSource> getPhiSources() {
        return sources.entrySet()
                      .stream()
                      .map(entry -> new PhiSource(entry.getKey(), entry.getValue()))
                      .collect(Collectors.toList());
    }

    public @NotNull Map<IrLabel, IrDirectValue> getSources() {
        return Map.copyOf(sources);
    }

    public @NotNull IrRegister getDestination() {
        return destination;
    }
}

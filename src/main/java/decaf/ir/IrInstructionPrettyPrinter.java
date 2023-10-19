package decaf.ir;

import decaf.ir.instructions.*;
import decaf.ir.types.IrPointerType;
import decaf.ir.types.IrType;
import decaf.ir.types.IrVoidType;
import decaf.ir.values.IrConstantInt;
import decaf.ir.values.IrLabel;
import decaf.ir.values.IrValue;
import decaf.shared.ColorPrint;
import org.jetbrains.annotations.NotNull;

public class IrInstructionPrettyPrinter implements IrInstructionVisitor<Void, String> {
    private final StyleConfig reservedWordStyle = new StyleConfig(ColorPrint.Color.GREEN, ColorPrint.Format.BOLD);
    private final StyleConfig metadataStyle = new StyleConfig(ColorPrint.Color.BLUE, ColorPrint.Format.BOLD);
    private final StyleConfig constantStyle = new StyleConfig(ColorPrint.Color.YELLOW, ColorPrint.Format.NORMAL);
    private final StyleConfig typeStyle = new StyleConfig(ColorPrint.Color.CYAN, ColorPrint.Format.BOLD);
    private final StyleConfig labelStyle = new StyleConfig(ColorPrint.Color.MAGENTA, ColorPrint.Format.NORMAL);
    private final StyleConfig pointerStyle = new StyleConfig(ColorPrint.Color.WHITE, ColorPrint.Format.NORMAL);

    private @NotNull String colorPrintReservedWord(@NotNull String reservedWord) {
        return ColorPrint.getColoredString(reservedWord, reservedWordStyle.color, reservedWordStyle.format);
    }

    private @NotNull String colorPrintMetadata(@NotNull String metadata) {
        return ColorPrint.getColoredString(metadata, metadataStyle.color, metadataStyle.format);
    }

    private @NotNull String colorPrintConstants(@NotNull String constant) {
        return ColorPrint.getColoredString(constant, constantStyle.color, constantStyle.format);
    }

    private @NotNull String colorPrintValue(@NotNull IrValue irValue) {
        if (irValue instanceof IrConstantInt irConstantInt) {
            return colorPrintConstants(irConstantInt.prettyPrint());
        } else {
            return irValue.prettyPrint();
        }
    }

    private @NotNull String colorPrintType(@NotNull IrType irType) {
        if (irType instanceof IrPointerType irPointerType) {
            return ColorPrint.getColoredString(irPointerType.prettyPrint(), pointerStyle.color, pointerStyle.format);
        }
        return ColorPrint.getColoredString(irType.prettyPrint(), typeStyle.color, typeStyle.format);
    }

    private @NotNull String colorPrintLabel(@NotNull IrLabel irLabel) {
        return ColorPrint.getColoredString(irLabel.prettyPrint(), labelStyle.color, labelStyle.format);
    }

    @Override
    public @NotNull String visit(@NotNull AllocaInstruction allocaInstruction, Void argument) {
        return String.format("%s = %s %s, %s %s",
                             colorPrintValue(allocaInstruction.getAddress()),
                             colorPrintReservedWord("alloca"),
                             colorPrintType(allocaInstruction.getType()),
                             colorPrintMetadata("align"),
                             colorPrintMetadata(String.valueOf(allocaInstruction.getNumBytesToAllocate()))
                            );
    }

    @Override
    public @NotNull String visit(@NotNull BinaryInstruction binaryInstruction, Void argument) {
        return String.format("%s = %s %s %s, %s",
                             colorPrintValue(binaryInstruction.getDestination()),
                             colorPrintReservedWord(binaryInstruction.getBinaryOpString()),
                             colorPrintType(binaryInstruction.getDestination().getType()),
                             colorPrintValue(binaryInstruction.getLhs()),
                             colorPrintValue(binaryInstruction.getRhs())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull BranchInstruction branchInstruction, Void argument) {
        return String.format("%s %s %s, %s %s, %s %s",
                             colorPrintReservedWord("branch"),
                             colorPrintType(branchInstruction.getCondition().getType()),
                             colorPrintValue(branchInstruction.getCondition()),
                             colorPrintType(branchInstruction.getTrueTarget().getType()),
                             colorPrintLabel(branchInstruction.getTrueTarget()),
                             colorPrintType(branchInstruction.getFalseTarget().getType()),
                             colorPrintLabel(branchInstruction.getFalseTarget())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull CallInstruction callInstruction, Void argument) {
        if (callInstruction.getDestination() == null) {
            return String.format("%s %s",
                                 colorPrintReservedWord("call"),
                                 colorPrintType(callInstruction.getFunctionPointer().getType())
                                );
        } else {
            return String.format("%s = %s %s, %s",
                                 colorPrintValue(callInstruction.getDestination()),
                                 colorPrintReservedWord("call"),
                                 colorPrintMetadata(callInstruction.getFunctionPointer().getFunctionName()),
                                 colorPrintType(callInstruction.getFunctionPointer().getType())
                                );
        }
    }

    @Override
    public @NotNull String visit(@NotNull ZextInstruction zextInstruction, Void argument) {
        return String.format("%s = %s %s %s %s %s",
                             colorPrintValue(zextInstruction.getDestination()),
                             colorPrintReservedWord("zext"),
                             colorPrintType(zextInstruction.getSource().getType()),
                             colorPrintValue(zextInstruction.getSource()),
                             colorPrintReservedWord("to"),
                             colorPrintType(zextInstruction.getDestination().getType())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull CompareInstruction compareInstruction, Void argument) {
        return String.format("%s = %s %s %s, %s",
                             colorPrintValue(compareInstruction.getDestination()),
                             colorPrintReservedWord(compareInstruction.getCompareOpString()),
                             colorPrintType(compareInstruction.getDestination().getType()),
                             colorPrintValue(compareInstruction.getLhs()),
                             colorPrintValue(compareInstruction.getRhs())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull GetAddressInstruction getElementPtrInstruction, Void argument) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull String visit(@NotNull LoadInstruction loadInstruction, Void argument) {
        return String.format("%s = %s %s, %s %s",
                             colorPrintValue(loadInstruction.getDestination()),
                             colorPrintReservedWord("load"),
                             colorPrintType(loadInstruction.getDestination().getType()),
                             colorPrintType(loadInstruction.getAddress().getType()),
                             colorPrintValue(loadInstruction.getAddress())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull PhiInstruction phiInstruction, Void argument) {
        return String.format("%s = %s %s %s",
                             colorPrintValue(phiInstruction.getDestination()),
                             colorPrintReservedWord("phi"),
                             colorPrintType(phiInstruction.getDestination().getType()),
                             phiInstruction.getPhiSources()
                                           .stream()
                                           .map(phiSource -> String.format("[%s, %s]",
                                                                           colorPrintValue(phiSource.value()),
                                                                           colorPrintLabel(phiSource.label())
                                                                          ))
                                           .reduce((s1, s2) -> String.format("%s, %s", s1, s2))
                                           .orElse("")
                            );
    }

    @Override
    public @NotNull String visit(@NotNull ReturnInstruction returnInstruction, Void argument) {
        return returnInstruction.getReturnValue()
                                .map(irValue -> String.format("%s %s %s",
                                                              colorPrintReservedWord("return"),
                                                              colorPrintType(irValue.getType()),
                                                              colorPrintValue(irValue)
                                                             ))
                                .orElseGet(() -> String.format("%s %s",
                                                               colorPrintReservedWord("return"),
                                                               colorPrintType(IrVoidType.get())
                                                              ));
    }

    @Override
    public @NotNull String visit(@NotNull StoreInstruction storeInstruction, Void argument) {
        return String.format("%s %s, %s %s",
                             colorPrintReservedWord("store"),
                             colorPrintType(storeInstruction.getValue().getType()),
                             colorPrintType(storeInstruction.getAddress().getType()),
                             colorPrintValue(storeInstruction.getAddress())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull UnconditionalBranchInstruction unconditionalBranchInstruction,
                                 Void argument) {
        return String.format("%s %s %s",
                             colorPrintReservedWord("branch"),
                             colorPrintType(unconditionalBranchInstruction.getTarget().getType()),
                             colorPrintLabel(unconditionalBranchInstruction.getTarget())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull UnaryInstruction unaryInstruction, Void argument) {
        return String.format("%s = %s %s %s",
                             colorPrintValue(unaryInstruction.getDestination()),
                             colorPrintReservedWord(unaryInstruction.getUnaryOpString()),
                             colorPrintType(unaryInstruction.getDestination().getType()),
                             colorPrintValue(unaryInstruction.getOperand())
                            );
    }

    @Override
    public @NotNull String visit(@NotNull IrFunction irFunction, Void argument) {
        return "";
    }

    record StyleConfig(ColorPrint.Color color, ColorPrint.Format format) {
    }
}

package decaf.ir;

import decaf.ir.instructions.*;
import org.jetbrains.annotations.NotNull;

public interface IrInstructionVisitor<ArgumentType, ReturnType> {
    @NotNull ReturnType visit(@NotNull AllocaInstruction allocaInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull BinaryInstruction binaryInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull BranchInstruction branchInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull CallInstruction callInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull ZextInstruction zextInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull CompareInstruction compareInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull GetAddressInstruction getElementPtrInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull LoadInstruction loadInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull PhiInstruction phiInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull ReturnInstruction returnInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull StoreInstruction storeInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull UnconditionalBranchInstruction unconditionalBranchInstruction,
                              ArgumentType argument);

    @NotNull ReturnType visit(@NotNull UnaryInstruction unaryInstruction, ArgumentType argument);

    @NotNull ReturnType visit(@NotNull IrFunction irFunction, ArgumentType argument);
}

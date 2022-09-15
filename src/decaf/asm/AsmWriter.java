package decaf.asm;

import org.jetbrains.annotations.NotNull;

import decaf.codegen.codes.ConditionalBranch;
import decaf.codegen.codes.ReturnInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.codes.UnconditionalBranch;
import decaf.codegen.codes.ArrayBoundsCheck;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCallNoResult;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.GetAddress;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.codes.RuntimeException;

public interface AsmWriter {

    void emitInstruction(@NotNull ConditionalBranch conditionalBranch);

    void emitInstruction(@NotNull Method method);

    void emitInstruction(@NotNull FunctionCallWithResult functionCallWithResult);

    void emitInstruction(@NotNull FunctionCallNoResult functionCallNoResult);

    void emitInstruction(@NotNull MethodEnd methodEnd);

    void emitInstruction(@NotNull ReturnInstruction returnInstruction);

    void emitInstruction(@NotNull UnaryInstruction unaryInstruction);

    void emitInstruction(@NotNull BinaryInstruction binaryInstruction);

    void emitInstruction(@NotNull UnconditionalBranch unconditionalBranch);

    void emitInstruction(@NotNull ArrayBoundsCheck arrayBoundsCheck);

    void emitInstruction(@NotNull RuntimeException runtimeException);

    void emitInstruction(@NotNull CopyInstruction copyInstruction);

    void emitInstruction(@NotNull GetAddress getAddress);

}

package edu.mit.compilers.asm;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalBranch;

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

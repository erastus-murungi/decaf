package edu.mit.compilers.asm;

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

    void emitInstruction(ConditionalBranch jumpIfFalse);

    void emitInstruction(Method method);

    void emitInstruction(FunctionCallWithResult functionCallWithResult);

    void emitInstruction(FunctionCallNoResult functionCallNoResult);

    void emitInstruction(MethodEnd methodEnd);

    void emitInstruction(ReturnInstruction returnInstruction);

    void emitInstruction(UnaryInstruction unaryInstruction);

    void emitInstruction(BinaryInstruction binaryInstruction);

    void emitInstruction(UnconditionalBranch unconditionalBranch);

    void emitInstruction(ArrayBoundsCheck arrayBoundsCheck);

    void emitInstruction(RuntimeException runtimeException);

    void emitInstruction(CopyInstruction assignment);

    void emitInstruction(GetAddress getAddress);

}

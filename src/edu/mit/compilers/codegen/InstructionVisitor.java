package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;

public interface InstructionVisitor<ReturnType, ExtraInfoType> {

    ReturnType visit(AllocateInstruction allocateInstruction, ExtraInfoType extraInfo);

    ReturnType visit(ConditionalBranch jumpIfFalse, ExtraInfoType extraInfo);

    ReturnType visit(Label label, ExtraInfoType extraInfo);

    ReturnType visit(Method method, ExtraInfoType extraInfo);

    ReturnType visit(FunctionCallWithResult functionCallWithResult, ExtraInfoType extraInfo);

    ReturnType visit(FunctionCallNoResult functionCallNoResult, ExtraInfoType extraInfo);

    ReturnType visit(MethodEnd methodEnd, ExtraInfoType extraInfo);

    ReturnType visit(ReturnInstruction returnInstruction, ExtraInfoType extraInfo);

    ReturnType visit(UnaryInstruction oneOperandAssign, ExtraInfoType extraInfo);

    ReturnType visit(StringLiteralAllocation stringLiteralAllocation, ExtraInfoType extraInfo);

    ReturnType visit(BinaryInstruction binaryInstruction, ExtraInfoType extraInfo);

    ReturnType visit(UnconditionalJump unconditionalJump, ExtraInfoType extraInfo);

    ReturnType visit(GlobalAllocation globalAllocation, ExtraInfoType extra);

    ReturnType visit(ArrayBoundsCheck arrayBoundsCheck, ExtraInfoType extra);

    ReturnType visit(RuntimeException runtimeException, ExtraInfoType extraInfo);

    ReturnType visit(CopyInstruction assignment, ExtraInfoType extraInfo);

    ReturnType visit(GetAddress getAddress, ExtraInfoType extraInfo);

}

package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.AllocateInstruction;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.codes.UnconditionalBranch;

public interface InstructionVisitor<ReturnType, ExtraInfoType> {

    ReturnType visit(AllocateInstruction allocateInstruction, ExtraInfoType extraInfo);

    ReturnType visit(ConditionalBranch jumpIfFalse, ExtraInfoType extraInfo);

    ReturnType visit(Method method, ExtraInfoType extraInfo);

    ReturnType visit(FunctionCallWithResult functionCallWithResult, ExtraInfoType extraInfo);

    ReturnType visit(FunctionCallNoResult functionCallNoResult, ExtraInfoType extraInfo);

    ReturnType visit(MethodEnd methodEnd, ExtraInfoType extraInfo);

    ReturnType visit(ReturnInstruction returnInstruction, ExtraInfoType extraInfo);

    ReturnType visit(UnaryInstruction oneOperandAssign, ExtraInfoType extraInfo);

    ReturnType visit(StringLiteralAllocation stringLiteralAllocation, ExtraInfoType extraInfo);

    ReturnType visit(BinaryInstruction binaryInstruction, ExtraInfoType extraInfo);

    ReturnType visit(UnconditionalBranch unconditionalBranch, ExtraInfoType extraInfo);

    ReturnType visit(GlobalAllocation globalAllocation, ExtraInfoType extra);

    ReturnType visit(ArrayBoundsCheck arrayBoundsCheck, ExtraInfoType extra);

    ReturnType visit(RuntimeException runtimeException, ExtraInfoType extraInfo);

    ReturnType visit(CopyInstruction assignment, ExtraInfoType extraInfo);

    ReturnType visit(GetAddress getAddress, ExtraInfoType extraInfo);

}

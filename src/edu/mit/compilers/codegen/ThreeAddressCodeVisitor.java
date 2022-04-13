package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;

public interface ThreeAddressCodeVisitor<ReturnType, ExtraInfoType> {

    ReturnType visit(ConditionalJump jumpIfFalse, ExtraInfoType extraInfo);

    ReturnType visit(Label label, ExtraInfoType extraInfo);

    ReturnType visit(MethodBegin methodBegin, ExtraInfoType extraInfo);

    ReturnType visit(MethodCallSetResult methodCallSetResult, ExtraInfoType extraInfo);

    ReturnType visit(MethodCallNoResult methodCallNoResult, ExtraInfoType extraInfo);

    ReturnType visit(MethodEnd methodEnd, ExtraInfoType extraInfo);

    ReturnType visit(MethodReturn methodReturn, ExtraInfoType extraInfo);

    ReturnType visit(Triple oneOperandAssign, ExtraInfoType extraInfo);

    ReturnType visit(PopParameter popParameter, ExtraInfoType extraInfo);

    ReturnType visit(PushParameter pushParameter, ExtraInfoType extraInfo);

    ReturnType visit(StringLiteralStackAllocation stringLiteralStackAllocation, ExtraInfoType extraInfo);

    ReturnType visit(Quadruple quadruple, ExtraInfoType extraInfo);

    ReturnType visit(UnconditionalJump unconditionalJump, ExtraInfoType extraInfo);

    ReturnType visit(GlobalAllocation globalAllocation, ExtraInfoType extra);

    ReturnType visit(ArrayBoundsCheck arrayBoundsCheck, ExtraInfoType extra);

    ReturnType visit(ArrayAccess arrayAccess, ExtraInfoType extra);

    ReturnType visit(RuntimeException runtimeException, ExtraInfoType extraInfo);

    ReturnType visit(Assign assignment, ExtraInfoType extraInfo);
}

package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.symbolTable.SymbolTable;

public interface ThreeAddressCodeVisitor<ReturnType, ExtraInfoType> {

    ReturnType visit(CopyInstruction copyInstruction, ExtraInfoType extraInfo);

    ReturnType visit(JumpIfFalse jumpIfFalse, ExtraInfoType extraInfo);

    ReturnType visit(Label label, ExtraInfoType extraInfo);

    ReturnType visit(MethodBegin methodBegin, ExtraInfoType extraInfo);

    ReturnType visit(MethodCall methodCall, ExtraInfoType extraInfo);

    ReturnType visit(MethodEnd methodEnd, ExtraInfoType extraInfo);

    ReturnType visit(MethodReturn methodReturn, ExtraInfoType extraInfo);

    ReturnType visit(OneOperandAssign oneOperandAssign, ExtraInfoType extraInfo);

    ReturnType visit(PopParameter popParameter, ExtraInfoType extraInfo);

    ReturnType visit(PushParameter pushParameter, ExtraInfoType extraInfo);

    ReturnType visit(StringLiteralStackAllocation stringLiteralStackAllocation, ExtraInfoType extraInfo);

    ReturnType visit(TwoOperandAssign twoOperandAssign, ExtraInfoType extraInfo);

    ReturnType visit(UnconditionalJump unconditionalJump, ExtraInfoType extraInfo);

    ReturnType visit(DataSectionAllocation dataSectionAllocation, ExtraInfoType extra);

    ReturnType visit(ArrayBoundsCheck arrayBoundsCheck, ExtraInfoType extra);
}

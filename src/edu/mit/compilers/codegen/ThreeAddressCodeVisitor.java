package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.symbolTable.SymbolTable;

public interface ThreeAddressCodeVisitor<ReturnType, ExtraInfoType> {
    ReturnType visit(CopyInstruction copyInstruction, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(JumpIfFalse jumpIfFalse, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(Label label, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(MethodBegin methodBegin, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(MethodCall methodCall, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(MethodEnd methodEnd, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(MethodReturn methodReturn, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(OneOperandAssign oneOperandAssign, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(PopParameter popParameter, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(ProgramBegin programBegin, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(PushParameter pushParameter, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(StringLiteralStackAllocation stringLiteralStackAllocation, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(TwoOperandAssign twoOperandAssign, SymbolTable symbolTable, ExtraInfoType extraInfo);

    ReturnType visit(UnconditionalJump unconditionalJump, SymbolTable symbolTable, ExtraInfoType extraInfo);
}

package decaf.synthesis.asm;


import decaf.ir.codes.ArrayBoundsCheck;
import decaf.ir.codes.BinaryInstruction;
import decaf.ir.codes.ConditionalBranch;
import decaf.ir.codes.CopyInstruction;
import decaf.ir.codes.FunctionCallNoResult;
import decaf.ir.codes.FunctionCallWithResult;
import decaf.ir.codes.GetAddress;
import decaf.ir.codes.Method;
import decaf.ir.codes.MethodEnd;
import decaf.ir.codes.ReturnInstruction;
import decaf.ir.codes.RuntimeError;
import decaf.ir.codes.UnaryInstruction;
import decaf.ir.codes.UnconditionalBranch;

public interface AsmWriter {

  void emitInstruction(ConditionalBranch conditionalBranch);

  void emitInstruction(Method method);

  void emitInstruction(FunctionCallWithResult functionCallWithResult);

  void emitInstruction(FunctionCallNoResult functionCallNoResult);

  void emitInstruction(MethodEnd methodEnd);

  void emitInstruction(ReturnInstruction returnInstruction);

  void emitInstruction(UnaryInstruction unaryInstruction);

  void emitInstruction(BinaryInstruction binaryInstruction);

  void emitInstruction(UnconditionalBranch unconditionalBranch);

  void emitInstruction(ArrayBoundsCheck arrayBoundsCheck);

  void emitInstruction(RuntimeError runtimeError);

  void emitInstruction(CopyInstruction copyInstruction);

  void emitInstruction(GetAddress getAddress);

}

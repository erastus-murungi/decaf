package decaf.asm;


import decaf.codegen.codes.ArrayBoundsCheck;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.ConditionalBranch;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCallNoResult;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.GetAddress;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.codes.ReturnInstruction;
import decaf.codegen.codes.RuntimeException;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.codes.UnconditionalBranch;

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

  void emitInstruction(RuntimeException runtimeException);

  void emitInstruction(CopyInstruction copyInstruction);

  void emitInstruction(GetAddress getAddress);

}

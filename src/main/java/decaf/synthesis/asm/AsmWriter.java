package decaf.synthesis.asm;


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

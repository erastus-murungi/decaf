package decaf.ir.instructions;

public abstract class Instruction {
  public abstract String prettyPrint();
  public abstract String toString();
  public abstract String prettyPrintColored();
  public abstract <T> boolean isWellFormed(T neededContext) throws InstructionMalformed;
}

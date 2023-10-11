package decaf.ir.instructions;

public class InstructionMalformed extends IllegalStateException {
    public InstructionMalformed(String message) {
        super(message);
    }
}

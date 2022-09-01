package edu.mit.compilers.codegen.codes;

import java.util.List;
import java.util.Optional;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

public class AllocateInstruction extends StoreInstruction {
    private LValue variable;

    public AllocateInstruction(LValue variable) {
        super(variable.copy());
        this.variable = variable.copy();
    }

    public LValue getVariable() {
        return variable;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllNames() {
        return List.of(variable);
    }

    @Override
    public Instruction copy() {
        return new AllocateInstruction(variable);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(variable);
    }

    @Override
    public List<Value> getOperandNames() {
        return List.of(variable);
    }

    @Override
    public boolean replace(Value oldName, Value newName) {
        var replaced = false;
        if (oldName.equals(variable)) {
            if (newName instanceof LValue) {
                variable = (LValue) newName;
                replaced = true;
            }
        }
        return replaced;
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (variable instanceof MemoryAddress)
            return Optional.empty();
        return Optional.of(getOperand());
    }

    @Override
    public String toString() {
        return String.format("%s%s = %s %s", DOUBLE_INDENT, variable, "allocate", variable.getType().getSourceCode());
    }

    @Override
    public String syntaxHighlightedToString() {
        final var allocateString = Utils.coloredPrint("allocate", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s = %s %s", DOUBLE_INDENT, variable.repr(), allocateString, variable.getType().getColoredSourceCode());
    }
}

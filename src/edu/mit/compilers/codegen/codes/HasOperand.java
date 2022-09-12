package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.dataflow.operand.Operand;

public abstract class HasOperand extends Instruction {
    public HasOperand(AST source, String comment) {
        super(source, comment);
    }

    public HasOperand(AST source) {
        super(source, null);
    }

    public HasOperand() {
        super(null, null);
    }

    public abstract Operand getOperand();

    public abstract List<Value> getOperandValues();

    public abstract boolean replaceValue(Value oldName, Value newName);

    public List<VirtualRegister> getOperandLValues() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> abstractName instanceof VirtualRegister)
                .map(value -> (VirtualRegister) value)
                .toList();
    }

    public List<VirtualRegister> getOperandVirtualRegisters() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> (abstractName instanceof VirtualRegister))
                .map(abstractName -> (VirtualRegister) abstractName)
                .toList();
    }
}

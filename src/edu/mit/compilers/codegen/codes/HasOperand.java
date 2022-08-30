package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.List;
import java.util.stream.Collectors;

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

    public abstract List<Value> getOperandNames();

    public abstract boolean replace(Value oldName, Value newName);

    public List<Value> getOperandNamesNoArray() {
        return getOperandNames()
                .stream()
                .filter(abstractName -> !(abstractName instanceof MemoryAddressName))
                .collect(Collectors.toList());
    }

    public List<LValue> getLValues() {
        return getOperandNamesNoArray()
                .stream()
                .filter(abstractName -> (abstractName instanceof LValue))
                .map(abstractName -> (LValue) abstractName)
                .collect(Collectors.toList());
    }
}

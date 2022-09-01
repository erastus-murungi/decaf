package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.Variable;
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

    public abstract List<Value> getOperandValues();

    public abstract boolean replace(Value oldName, Value newName);

    public List<Variable> getOperandScalarVariables() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> abstractName instanceof Variable)
                .map(value -> (Variable) value)
                .collect(Collectors.toList());
    }

    public List<LValue> getOperandLValues() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> (abstractName instanceof LValue))
                .map(abstractName -> (LValue) abstractName)
                .collect(Collectors.toList());
    }
}

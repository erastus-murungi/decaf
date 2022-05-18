package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
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

    public abstract List<AbstractName> getOperandNames();

    public abstract boolean replace(AbstractName oldName, AbstractName newName);

    public List<AbstractName> getOperandNamesNoArray() {
        return getOperandNames()
                .stream()
                .filter(abstractName -> !(abstractName instanceof MemoryAddressName))
                .collect(Collectors.toList());
    }

    public List<AbstractName> getOperandNamesNoArrayNoConstants() {
        return getOperandNamesNoArray()
                .stream()
                .filter(abstractName -> (abstractName instanceof AssignableName))
                .collect(Collectors.toList());
    }
}

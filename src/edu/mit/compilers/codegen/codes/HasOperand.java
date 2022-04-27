package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface HasOperand {
    boolean hasUnModifiedOperand();

    Operand getOperand();

    List<AbstractName> getOperandNames();

    boolean replace(AbstractName oldName, AbstractName newName);

    default List<AbstractName> getOperandNamesNoArray() {
        return getOperandNames()
                .stream()
                .filter(abstractName -> !(abstractName instanceof ArrayName))
                .collect(Collectors.toList());
    }

    default List<AbstractName> getOperandNamesNoArrayNoConstants() {
        return getOperandNamesNoArray()
                .stream()
                .filter(abstractName -> (abstractName instanceof AssignableName))
                .collect(Collectors.toList());
    }
}
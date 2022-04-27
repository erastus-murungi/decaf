package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.Objects;

public class UnmodifiedOperand extends Operand {
    public AbstractName abstractName;
    public String operator;

    public UnmodifiedOperand(AbstractName abstractName) {
        this.abstractName = abstractName;
        this.operator = DecafScanner.ASSIGN;
    }

    @Override
    public boolean contains(AbstractName comp) {
        return comp.equals(abstractName);
    }

    @Override
    public boolean isContainedIn(HasResult hasResult) {
        return false;
    }

    @Override
    public HasResult fromOperand(AssignableName resultLocation) {
        return new Assign(resultLocation, operator, abstractName, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnmodifiedOperand that = (UnmodifiedOperand) o;
        return Objects.equals(abstractName, that.abstractName);
    }

    @Override
    public int hashCode() {
        return abstractName.hashCode();
    }

    @Override
    public String toString() {
        return abstractName.toString();
    }
}
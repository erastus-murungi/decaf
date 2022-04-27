package edu.mit.compilers.dataflow.reachingvalues;

import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.dataflow.usedef.UseDef;

import java.util.Objects;


public class DefValue {
    public AssignableName variableName;
    public Operand operand;

    public DefValue(HasResult hasResult){
        this.variableName = hasResult.getResultLocation();
        if (hasResult instanceof HasOperand){
            operand = ((HasOperand) hasResult).getOperand();
        }
    }

    public boolean isConstantDefinition(){
        return operand instanceof UnmodifiedOperand ||
                (operand instanceof UnaryOperand && Objects.equals(((UnaryOperand) operand).operator, "-")
                        && ((UnaryOperand) operand).operand instanceof ConstantName);
    }

    @Override
    public int hashCode() {
        return variableName.hashCode() ^ operand.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefValue defValue = (DefValue) o;
        return Objects.equals(this.variableName, defValue.variableName) &&
                Objects.equals(this.operand, defValue.operand);
    }

    @Override
    public String toString() {
        return "(Variable Name {\"" + variableName.toString() + "\"}, Operand {\"" + operand.toString() + "\"}";
    }
}

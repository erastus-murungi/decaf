package decaf.codegen.codes;

import java.util.List;

import decaf.ast.AST;
import decaf.codegen.names.IrValue;
import decaf.codegen.names.IrRegister;
import decaf.dataflow.operand.Operand;

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

    public abstract List<IrValue> getOperandValues();

    public abstract boolean replaceValue(IrValue oldName, IrValue newName);

    public List<IrRegister> getOperandLValues() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> abstractName instanceof IrRegister)
                .map(value -> (IrRegister) value)
                .toList();
    }

    public List<IrRegister> getOperandVirtualRegisters() {
        return getOperandValues()
                .stream()
                .filter(abstractName -> (abstractName instanceof IrRegister))
                .map(abstractName -> (IrRegister) abstractName)
                .toList();
    }
}

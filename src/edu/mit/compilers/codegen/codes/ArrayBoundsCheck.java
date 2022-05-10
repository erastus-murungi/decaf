package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.List;

public class ArrayBoundsCheck extends Instruction implements HasOperand {
    public Label indexIsLessThanArraySize;
    public Label indexIsGTEZero;
    public ArrayAccess arrayAccess;

    public ArrayBoundsCheck(AST source, ArrayAccess arrayAccess, String comment, Label indexIsLessThanArraySize, Label indexIsGTEZero) {
        super(source, comment);
        this.indexIsLessThanArraySize = indexIsLessThanArraySize;
        this.indexIsGTEZero = indexIsGTEZero;
        this.arrayAccess = arrayAccess;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(arrayAccess.accessIndex);
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new ArrayBoundsCheck(source, arrayAccess, getComment().orElse(null), indexIsLessThanArraySize, indexIsGTEZero);
    }

    @Override
    public String toString() {
        return String.format("%scheck bounds *%s ", DOUBLE_INDENT, arrayAccess);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(arrayAccess.accessIndex);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(arrayAccess.accessIndex);
    }

    @Override
    public boolean replace(AbstractName oldName, AbstractName newName) {
        var replaced = false;
        if (arrayAccess.accessIndex.equals(oldName)) {
            arrayAccess.accessIndex = newName;
            replaced = true;
        }
        return replaced;
    }

//    @Override
//    public boolean hasUnModifiedOperand() {
//        return true;
//    }
//
//    @Override
//    public Operand getOperand() {
//        return new UnmodifiedOperand(arrayAccess.accessIndex);
//    }
//
//    @Override
//    public List<AbstractName> getOperandNames() {
//        return List.of(arrayAccess.accessIndex);
//    }
//
//    @Override
//    public boolean replace(AbstractName oldName, AbstractName newName) {
//        var replaced = false;
//        if (arrayAccess.accessIndex.equals(oldName)) {
//            arrayAccess.accessIndex = newName;
//            replaced = true;
//        }
//        return replaced;
//    }
}
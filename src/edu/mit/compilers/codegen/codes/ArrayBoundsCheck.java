package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.List;

public class ArrayBoundsCheck extends ThreeAddressCode {
    public Label indexIsLessThanArraySize;
    public Label indexIsGTEZero;
    public ArrayAccess arrayAccess;

    public ArrayBoundsCheck(AST source, ArrayAccess arrayAccess,String comment, Label indexIsLessThanArraySize, Label indexIsGTEZero) {
        super(source, comment);
        this.indexIsLessThanArraySize = indexIsLessThanArraySize;
        this.indexIsGTEZero = indexIsGTEZero;
        this.arrayAccess = arrayAccess;
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(arrayAccess.accessIndex);
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("%scheck bounds %s ", DOUBLE_INDENT, arrayAccess);
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
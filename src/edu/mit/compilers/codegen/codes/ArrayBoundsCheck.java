package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.TemporaryName;

import java.util.List;

public class ArrayBoundsCheck extends ThreeAddressCode{
    public TemporaryName location;
    public Label boundsBad;
    public Label boundsGood;
    public Long arraySize;

    public ArrayBoundsCheck(AST source, String comment, Long arraySize, TemporaryName location, Label boundsBad, Label boundsGood) {
        super(source, comment);
        this.arraySize = arraySize;
        this.location = location;
        this.boundsBad = boundsBad;
        this.boundsGood = boundsGood;
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return null;
    }
}

package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.VariableName;

import java.util.Collections;
import java.util.List;

public class GlobalAllocation extends ThreeAddressCode {
    public static final int DEFAULT_ALIGNMENT = 8;

    public final AbstractName variableName;
    public final long size;
    public final int alignment;
    public final BuiltinType type;

    public GlobalAllocation(AST source, String comment, AbstractName variableName, long size, BuiltinType builtinType) {
        super(source, comment);
        this.variableName = variableName;
        this.size = size;
        this.type  = builtinType;
        this.alignment = DEFAULT_ALIGNMENT;
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.singletonList(variableName);
    }

    @Override
    public void swapOut(AbstractName oldName, AbstractName newName) {

    }

    @Override
    public String toString() {
        return String.format("%s.comm %s,%s,%s %s %s", INDENT, variableName, size, alignment, DOUBLE_INDENT, getComment().orElse(" ") + " " + type.getSourceCode());
    }
}

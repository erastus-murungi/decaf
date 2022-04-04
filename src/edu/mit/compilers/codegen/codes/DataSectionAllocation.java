package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.VariableName;

import java.util.Collections;
import java.util.List;

public class DataSectionAllocation extends ThreeAddressCode {
    public static final int DEFAULT_ALIGNMENT = 8;

    public final VariableName variableName;
    public final int size;
    public final int alignment;
    public final BuiltinType type;

    public DataSectionAllocation(AST source, String comment, VariableName variableName, int size, BuiltinType builtinType) {
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
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return String.format("%s.comm %s,%s,%s %s %s", INDENT, variableName, size, alignment, DOUBLE_INDENT, getComment().orElse(" ") + type.getSourceCode());
    }
}

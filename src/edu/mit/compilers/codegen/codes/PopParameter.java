package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;

public class PopParameter extends ThreeAddressCode {
    public AssignableName parameterName;
    public int parameterIndex;

    public PopParameter(AssignableName parameterName, AST source, int parameterIndex) {
        super(source);
        this.parameterName = parameterName;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "PopParameter", parameterName);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(parameterName);
    }
}

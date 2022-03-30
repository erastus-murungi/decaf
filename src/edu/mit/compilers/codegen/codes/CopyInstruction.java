package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;

public class CopyInstruction extends AbstractAssignment {
    public AbstractName src;

    public CopyInstruction(AbstractName src, AssignableName dst, AST source) {
        super(dst, source);
        this.src = src;
    }

    public CopyInstruction(AbstractName src, AssignableName dst, AST source, String comment) {
        super(dst, source, comment);
        this.src = src;
    }

    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s%s%s", DOUBLE_INDENT, dst, src, DOUBLE_INDENT, " <<<< " + getComment().get());
        return String.format("%s%s = %s", DOUBLE_INDENT, dst, src);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(src, dst);
    }

}

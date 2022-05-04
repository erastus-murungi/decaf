package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.List;

public class PushParameter extends ThreeAddressCode implements HasOperand {
    public AbstractName parameterName;
    public final int parameterIndex;

    public PushParameter(AbstractName parameterName, int parameterIndex, AST source) {
        super(source);
        this.parameterName = parameterName;
        this.parameterIndex = parameterIndex;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s%s%s", DOUBLE_INDENT, "push", parameterName, DOUBLE_INDENT, getComment().orElse(""));
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(parameterName);
    }

    @Override
    public String repr() {
        String comment = "# " + getComment().orElse("");
        return String.format("%s%s %s%s%20s", DOUBLE_INDENT, "push", parameterName.repr(), DOUBLE_INDENT, comment);
    }

    @Override
    public ThreeAddressCode copy() {
        return new PushParameter(parameterName, parameterIndex, source);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(parameterName);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(parameterName);
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (parameterName.equals(oldVariable)) {
            parameterName = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public boolean hasUnModifiedOperand() {
        return true;
    }

}

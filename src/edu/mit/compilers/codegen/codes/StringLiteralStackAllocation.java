package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.utils.Utils;

import java.util.Collections;
import java.util.List;

public class StringLiteralStackAllocation extends Instruction {
    public String label;
    public String stringConstant;
    public String stringConstantEscaped;

    public StringLiteralStackAllocation(String stringConstant) {
        super(null);
        this.label = TemporaryNameIndexGenerator.getNextStringLiteralIndex();
        this.stringConstant = stringConstant;
        this.stringConstantEscaped = Utils.translateEscapes(stringConstant.substring(1, stringConstant.length() - 1));
    }

    public int size() {
        return stringConstantEscaped.length();
    }

    public String toString() {
        return String.format("%s:\n\t.%s   %s", label, "string", stringConstant);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return String.format("@.%s = %s%s%s", label, stringConstant, DOUBLE_INDENT, "# " + size() + " bytes");
    }

    @Override
    public Instruction copy() {
        return new StringLiteralStackAllocation(stringConstant);
    }

    public String getASM() {
        return String.format("%s:\n\t.%s   %s\n\t.align 16", label, "string", stringConstant);
    }
}

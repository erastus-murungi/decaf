package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.codegen.names.Value;

public class StringLiteralAllocation extends Instruction {
    public String label;
    public String stringConstant;
    public String stringConstantEscaped;

    public StringLiteralAllocation(String stringConstant) {
        super(null);
        this.label = TemporaryNameIndexGenerator.getNextStringLiteralIndex();
        this.stringConstant = stringConstant;
        this.stringConstantEscaped = stringConstant.substring(1, stringConstant.length() - 1).translateEscapes();
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
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public String syntaxHighlightedToString() {
        return String.format("@.%s = %s%s%s", label, stringConstant, DOUBLE_INDENT, "# " + size() + " bytes");
    }

    @Override
    public Instruction copy() {
        return new StringLiteralAllocation(stringConstant);
    }

    public String getASM() {
        return String.format("%s:\n\t.%s   %s\n\t.align 16", label, "string", stringConstant);
    }
}

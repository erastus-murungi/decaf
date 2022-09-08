package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.StringConstant;
import edu.mit.compilers.codegen.names.Value;

public class StringConstantAllocation extends Instruction {
    private final StringConstant stringConstant;

    public StringConstantAllocation(StringConstant stringConstant) {
        super(null);
        this.stringConstant = stringConstant;
    }

    public StringConstant getStringConstant() {
        return stringConstant;
    }

    public String toString() {
        return String.format("%s:\n\t.%s   %s", stringConstant, "string", stringConstant.getValue());
    }

    @Override
    public <T, E> void accept(AsmWriter asmWriter) {
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public String syntaxHighlightedToString() {
        return String.format("@.%s = %s%s%s", stringConstant.getLabel(), stringConstant, DOUBLE_INDENT, "# " + stringConstant.size() + " bytes");
    }

    @Override
    public Instruction copy() {
        return new StringConstantAllocation(stringConstant);
    }

    public String getASM() {
        return String.format("%s:\n\t.%s   %s\n\t.align 16", stringConstant.getLabel(), "string", stringConstant.getValue());
    }
}

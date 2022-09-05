package edu.mit.compilers.dataflow.ssapasses;

import edu.mit.compilers.codegen.codes.Instruction;

public record SCCPOptResult(Instruction before, Instruction after) {

    @Override
    public String toString() {
        return String.format("SCCP :: replaced %s with %s",
                before.noCommentsSyntaxHighlighted().strip(),
                after.noCommentsSyntaxHighlighted().strip());
    }
}

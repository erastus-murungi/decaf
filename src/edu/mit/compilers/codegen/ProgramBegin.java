package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.Program;
import edu.mit.compilers.cfg.CFGNonConditional;

public class ProgramBegin extends ThreeAddressCode {
    CFGNonConditional globalBlock;
    public ProgramBegin(CFGNonConditional globalBlock) {
        super(null);
        this.globalBlock = globalBlock;
    }

    private int computeNumberOfBytesOfGlobalStorage() {
        return 0;
    }

    public String toString() {
        return String.format("Allocate %s bytes of %s-byte aligned memory:", computeNumberOfBytesOfGlobalStorage(), 8);
    }
}

package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.cfg.CFGNonConditional;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.symbolTable.SymbolTable;

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

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}

package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.utils.Utils;

public class CopyInstruction extends ThreeAddressCode {
    String src;
    String dst;

    public CopyInstruction(String src, String dst, AST source) {
        super(source);
        this.src = src;
        this.dst = dst;
    }

    public String toString() {
        return String.format("    %s = %s", Utils.coloredPrint(dst, Utils.ANSIColorConstants.ANSI_CYAN), src);
    }
}

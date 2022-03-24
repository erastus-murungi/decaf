package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class Original extends ThreeAddressCode {
    String loc;

    public Original(String loc, AST source) {
        super(source);
        this.loc = loc;
    }

    @Override
    public String toString() {
        return loc;
    }
}

package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;

public class CFGLine {
    public AST ast;

    public CFGLine(AST ast) {
        this.ast = ast;
    }
}

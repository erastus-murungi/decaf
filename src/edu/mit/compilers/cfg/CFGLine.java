package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class CFGLine {
    public AST ast;

    public CFGLine(AST ast) {
        this.ast = ast;
    }
}

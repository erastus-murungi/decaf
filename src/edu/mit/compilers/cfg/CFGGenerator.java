package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class CFGGenerator {
    public AST rootNode;
    public GlobalDescriptor globalDescriptor;

    public CFGGenerator(AST ast, GlobalDescriptor globalDescriptor) {
        this.rootNode = ast;
        this.globalDescriptor = globalDescriptor;
    }

    public iCFGVisitor buildiCFG() {
        iCFGVisitor visitor = new iCFGVisitor();
        MaximalVisitor maximalVisitor = new MaximalVisitor();
        NopVisitor nopVisitor = new NopVisitor();
        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);
        SymbolTable theSymbolWeCareAbout = globalDescriptor.globalVariablesSymbolTable;
        visitor.methodCFGBlocks.forEach((k, v) -> {
            nopVisitor.exit = new NOP();
            ((CFGNonConditional) v).autoChild.accept(nopVisitor, theSymbolWeCareAbout);
        });
        visitor.methodCFGBlocks.forEach((k, v) -> ((CFGNonConditional) v).autoChild.accept(maximalVisitor, theSymbolWeCareAbout));
        return visitor;
        // visitor has all the individual method CFGs and initial declarations
        // visitor.methodCFGBlocks
        // visitor.initialGlobalBlock
    }
}

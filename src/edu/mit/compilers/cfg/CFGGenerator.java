package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.descriptors.GlobalDescriptor;

public class CFGGenerator {
    
    public AST rootNode;
    public GlobalDescriptor globalDescriptor;

    public CFGGenerator(AST ast, GlobalDescriptor globalDescriptor){
        this.rootNode = ast;
        this.globalDescriptor = globalDescriptor;
    }

    public void buildiCFG(){
        iCFGVisitor visitor = new iCFGVisitor();
        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);
        // visitor has all the individual method CFGs and initial declarations
        // visitor.methodCFGBlocks
        // visitor.initialGlobalBlock
    }
}

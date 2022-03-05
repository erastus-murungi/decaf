package edu.mit.compilers.ir;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.DecafExceptionProcessor;


public class DecafSemanticChecker {

    private boolean trace;

    private boolean hasError;

    private AST rootNode;

    public DecafSemanticChecker(Program rootNode, DecafExceptionProcessor decafExceptionProcessor) {
        this.rootNode = rootNode;
    }

    public DecafSemanticChecker(AST rootNode) {
        this.rootNode = rootNode;
    }

    public void runChecks(DecafExceptionProcessor decafExceptionProcessor) {
        IRVisitor irVisitor = new IRVisitor();
        rootNode.accept(irVisitor, null);
        TypeCheckVisitor typeCheckVisitor = new TypeCheckVisitor((Program) rootNode, irVisitor.methods, irVisitor.fields, irVisitor.imports);
        rootNode.accept(typeCheckVisitor, irVisitor.fields);
        hasError = Visitor.exceptions.size() > 0;
        if (trace) {
            printAllExceptions(decafExceptionProcessor);
        }
    }

    public void printAllExceptions(DecafExceptionProcessor decafExceptionProcessor) {
        for (DecafSemanticException decafSemanticException : Visitor.exceptions) {
            decafExceptionProcessor.processDecafSemanticException(decafSemanticException).printStackTrace();
        }
    }

    public void setTrace(boolean shouldTrace) {
        trace = shouldTrace;
    }

    public boolean hasError() {
        return hasError;
    }


}

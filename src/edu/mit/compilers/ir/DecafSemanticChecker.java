package edu.mit.compilers.ir;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.utils.DecafExceptionProcessor;


public class DecafSemanticChecker {

    private final AST rootNode;
    public GlobalDescriptor globalDescriptor;
    private boolean trace;
    private boolean hasError;

    public DecafSemanticChecker(Program rootNode) {
        this.rootNode = rootNode;
    }

    public void runChecks(DecafExceptionProcessor decafExceptionProcessor) {
        var semanticCheckerVisitor = new SemanticCheckerASTVisitor();
        rootNode.accept(semanticCheckerVisitor, null);
        var typeCheckVisitor = new TypeCheckASTVisitor((Program) rootNode, semanticCheckerVisitor.methods, semanticCheckerVisitor.fields, semanticCheckerVisitor.imports);
        rootNode.accept(typeCheckVisitor, semanticCheckerVisitor.fields);
        globalDescriptor = new GlobalDescriptor(Type.Undefined, semanticCheckerVisitor.fields, semanticCheckerVisitor.methods, semanticCheckerVisitor.imports);
        hasError = ASTVisitor.exceptions.size() > 0;
        if (trace) {
            printAllExceptions(decafExceptionProcessor);
        }
    }

    public void printAllExceptions(DecafExceptionProcessor decafExceptionProcessor) {
        for (DecafSemanticException decafSemanticException : ASTVisitor.exceptions) {
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

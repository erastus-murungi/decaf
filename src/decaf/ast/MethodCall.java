package decaf.ast;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class MethodCall extends Expression {
    final public Name nameId;
    final public List<MethodCallParameter> methodCallParameterList;

    public boolean isImported = false;

    public MethodCall(Name nameId, List<MethodCallParameter> methodCallParameterList) {
        super(nameId.tokenPosition);
        this.nameId = nameId;
        this.methodCallParameterList = methodCallParameterList;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
        nodeArrayList.add(new Pair<>("methodName", nameId));
        for (MethodCallParameter methodCallParameter : methodCallParameterList)
            nodeArrayList.add(new Pair<>("arg", methodCallParameter));
        return nodeArrayList;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "MethodCall{" + "nameId=" + nameId + ", methodCallParameterList=" + methodCallParameterList + '}';
    }

    @Override
    public String getSourceCode() {
        List<String> stringList = new ArrayList<>();
        for (MethodCallParameter methodCallParameter : methodCallParameterList) {
            String sourceCode = methodCallParameter.getSourceCode();
            stringList.add(sourceCode);
        }
        return String.format("%s(%s)",
                nameId.getSourceCode(),
                String.join(", ", stringList));
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}

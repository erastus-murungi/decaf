package decaf.ast;

import java.util.ArrayList;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class MethodDefinition extends AST {
    final public TokenPosition tokenPosition;
    final public Type returnType;
    final public Name methodName;
    final public List<MethodDefinitionParameter> parameterList;
    final public Block block;

    public MethodDefinition(TokenPosition tokenPosition, Type returnType, List<MethodDefinitionParameter> parameterList, Name methodName, Block block) {
        this.tokenPosition = tokenPosition;
        this.returnType = returnType;
        this.parameterList = parameterList;
        this.methodName = methodName;
        this.block = block;
    }

    @Override
    public Type getType() {
        return Type.Undefined;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        List<Pair<String, AST>> nodes = new ArrayList<>();
        nodes.add(new Pair<>("returnType", new Name(returnType.toString(), tokenPosition, ExprContext.DECLARE)));
        nodes.add(new Pair<>("methodName", methodName));
        for (MethodDefinitionParameter methodDefinitionParameter : parameterList) {
            nodes.add(new Pair<>("arg", methodDefinitionParameter));
        }
        nodes.add(new Pair<>("block", block));

        return nodes;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "MethodDefinition{" +
                ", returnType=" + returnType +
                ", methodName=" + methodName +
                ", parameterList=" + parameterList +
                ", block=" + block +
                '}';
    }

    @Override
    public String getSourceCode() {
        List<String> params = new ArrayList<>();
        for (MethodDefinitionParameter methodDefinitionParameter : parameterList) {
            String sourceCode = methodDefinitionParameter.getSourceCode();
            params.add(sourceCode);
        }
        String indent = " ".repeat(returnType.getSourceCode().length() + methodName.getSourceCode().length() + 2);
        return String.format("%s %s(%s) {\n    %s\n}",
                returnType.getSourceCode(),
                methodName.getSourceCode(),
                String.join(",\n" + indent, params)
                , block.getSourceCode());
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}

package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;

import java.util.ArrayList;
import java.util.List;

public class IRVisitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    public void visit(IntLiteral intLiteral) {}
    public void visit(BooleanLiteral booleanLiteral) {}
    public void visit(DecimalLiteral decimalLiteral) {}
    public void visit(HexLiteral hexLiteral) {}
    public void visit(FieldDeclaration fieldDeclaration) {}
    public void visit(MethodDefinition methodDefinition) {}
    public void visit(ImportDeclaration importDeclaration) {}
    public void visit(For forStatement) {}
    public void visit(Break breakStatement) {}
    public void visit(Continue continueStatement) {}
    public void visit(While whileStatement) {}
    public void visit(Program program) {}
    public void visit(UnaryOpExpression unaryOpExpression) {}
    public void visit(BinaryOpExpression binaryOpExpression) {}
    public void visit(Block block) {}
    public void visit(ParenthesizedExpression parenthesizedExpression) {}
    public void visit(LocationArray locationArray) {}
    public void visit(CompoundAssignOpExpr compoundAssignOpExpr) {}
    public void visit(ExpressionParameter expressionParameter) {}
    public void visit(If ifStatement) {}
    public void visit(Return returnStatement) {}
    public void visit(Array array) {}
    public void visit(MethodCall methodCall) {}
    public void visit(MethodCallStatement methodCallStatement) {}
    public void visit(LocationAssignExpr locationAssignExpr) {}
    public void visit(AssignOpExpr assignOpExpr) {}
    public void visit(MethodDefinitionParameter methodDefinitionParameter) {}
    public void visit(Name name) {}
    public void visit(Location location) {}
    public void visit(Len len) {}
    public void visit(Increment increment) {}
    public void visit(Decrement decrement) {}
    public void visit(CharLiteral charLiteral) {}
    public void visit(MethodCallParameter methodCallParameter) {}
    public void visit(StringLiteral stringLiteral) {}
}

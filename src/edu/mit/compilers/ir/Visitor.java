package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;

import java.util.ArrayList;
import java.util.List;

public interface Visitor<T> {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    T visit(IntLiteral intLiteral);
    T visit(BooleanLiteral booleanLiteral);
    T visit(DecimalLiteral decimalLiteral);
    T visit(HexLiteral hexLiteral);
    T visit(FieldDeclaration fieldDeclaration);
    T visit(MethodDefinition methodDefinition);
    T visit(ImportDeclaration importDeclaration);
    T visit(For forStatement);
    T visit(Break breakStatement);
    T visit(Continue continueStatement);
    T visit(While whileStatement);
    T visit(Program program);
    T visit(UnaryOpExpression unaryOpExpression);
    T visit(BinaryOpExpression binaryOpExpression);
    T visit(Block block);
    T visit(ParenthesizedExpression parenthesizedExpression);
    T visit(LocationArray locationArray);
    T visit(CompoundAssignOpExpr compoundAssignOpExpr);
    T visit(ExpressionParameter expressionParameter);
    T visit(If ifStatement);
    T visit(Return returnStatement);
    T visit(Array array);
    T visit(MethodCall methodCall);
    T visit(MethodCallStatement methodCallStatement);
    T visit(LocationAssignExpr locationAssignExpr);
    T visit(AssignOpExpr assignOpExpr);
    T visit(MethodDefinitionParameter methodDefinitionParameter);
    T visit(Name name);
    T visit(Location location);
    T visit(Len len);
    T visit(Increment increment);
    T visit(Decrement decrement);
    T visit(CharLiteral charLiteral);
    T visit(MethodCallParameter methodCallParameter);
    T visit(StringLiteral stringLiteral);
}

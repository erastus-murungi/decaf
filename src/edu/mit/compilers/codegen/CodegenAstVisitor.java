package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.FieldDeclaration;
import edu.mit.compilers.ast.Initialization;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationAssignExpr;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallStatement;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.ParenthesizedExpression;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.codegen.names.AssignableName;

public interface CodegenAstVisitor<T> {
    T visit(BooleanLiteral booleanLiteral, AssignableName resultLocation);
    T visit(IntLiteral intLiteral, AssignableName resultLocation);
    T visit(StringLiteral stringLiteral, AssignableName resultLocation);
    T visit(FieldDeclaration fieldDeclaration, AssignableName resultLocation);
    T visit(UnaryOpExpression unaryOpExpression, AssignableName resultLocation);
    T visit(BinaryOpExpression binaryOpExpression, AssignableName resultLocation);
    T visit(Block block, AssignableName resultLocation);
    T visit(ParenthesizedExpression parenthesizedExpression, AssignableName resultLocation);
    T visit(LocationArray locationArray, AssignableName resultLocation);
    T visit(ExpressionParameter expressionParameter, AssignableName resultLocation);
    T visit(Return returnStatement, AssignableName resultLocation);
    T visit(MethodCall methodCall, AssignableName resultLocation);
    T visit(MethodCallStatement methodCallStatement, AssignableName resultLocation);
    T visit(LocationAssignExpr locationAssignExpr, AssignableName resultLocation);
    T visit(Name name, AssignableName resultLocation);
    T visit(LocationVariable locationVariable, AssignableName resultLocation);
    T visit(Len len);
    T visit(CompoundAssignOpExpr compoundAssignOpExpr, AssignableName resultLocation);
    T visit(Initialization initialization, AssignableName resultLocation);
    T visit(Assignment assignment, AssignableName resultLocation);
    T visit(MethodDefinitionParameter methodDefinitionParameter, AssignableName resultLocation);
}

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
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.VirtualRegister;

public interface CodegenAstVisitor<T> {
    T visit(BooleanLiteral booleanLiteral, LValue resultLocation);

    T visit(IntLiteral intLiteral, LValue resultLocation);

    T visit(StringLiteral stringLiteral, LValue resultLocation);

    T visit(FieldDeclaration fieldDeclaration, LValue resultLocation);

    T visit(UnaryOpExpression unaryOpExpression, LValue resultLocation);

    T visit(BinaryOpExpression binaryOpExpression, LValue resultLocation);

    T visit(Block block, LValue resultLocation);

    T visit(ParenthesizedExpression parenthesizedExpression, LValue resultLocation);

    T visit(LocationArray locationArray, LValue resultLocation);

    T visit(ExpressionParameter expressionParameter, LValue resultLocation);

    T visit(Return returnStatement, LValue resultLocation);

    T visit(MethodCall methodCall, LValue resultLocation);

    T visit(MethodCallStatement methodCallStatement, LValue resultLocation);

    T visit(LocationAssignExpr locationAssignExpr, LValue resultLocation);

    T visit(Name name, LValue resultLocation);

    T visit(LocationVariable locationVariable, LValue resultLocation);

    T visit(Len len);

    T visit(CompoundAssignOpExpr compoundAssignOpExpr, LValue resultLocation);

    T visit(Initialization initialization, LValue resultLocation);

    T visit(Assignment assignment, LValue resultLocation);

    T visit(MethodDefinitionParameter methodDefinitionParameter, LValue resultLocation);
}

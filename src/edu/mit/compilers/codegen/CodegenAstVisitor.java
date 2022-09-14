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
import edu.mit.compilers.codegen.names.IrAssignableValue;

public interface CodegenAstVisitor<T> {
    T visit(BooleanLiteral booleanLiteral, IrAssignableValue resultLocation);

    T visit(IntLiteral intLiteral, IrAssignableValue resultLocation);

    T visit(StringLiteral stringLiteral, IrAssignableValue resultLocation);

    T visit(FieldDeclaration fieldDeclaration, IrAssignableValue resultLocation);

    T visit(UnaryOpExpression unaryOpExpression, IrAssignableValue resultLocation);

    T visit(BinaryOpExpression binaryOpExpression, IrAssignableValue resultLocation);

    T visit(Block block, IrAssignableValue resultLocation);

    T visit(ParenthesizedExpression parenthesizedExpression, IrAssignableValue resultLocation);

    T visit(LocationArray locationArray, IrAssignableValue resultLocation);

    T visit(ExpressionParameter expressionParameter, IrAssignableValue resultLocation);

    T visit(Return returnStatement, IrAssignableValue resultLocation);

    T visit(MethodCall methodCall, IrAssignableValue resultLocation);

    T visit(MethodCallStatement methodCallStatement, IrAssignableValue resultLocation);

    T visit(LocationAssignExpr locationAssignExpr, IrAssignableValue resultLocation);

    T visit(Name name, IrAssignableValue resultLocation);

    T visit(LocationVariable locationVariable, IrAssignableValue resultLocation);

    T visit(Len len);

    T visit(CompoundAssignOpExpr compoundAssignOpExpr, IrAssignableValue resultLocation);

    T visit(Initialization initialization, IrAssignableValue resultLocation);

    T visit(Assignment assignment, IrAssignableValue resultLocation);

    T visit(MethodDefinitionParameter methodDefinitionParameter, IrAssignableValue resultLocation);
}

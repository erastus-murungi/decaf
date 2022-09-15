package decaf.codegen;

import decaf.ast.IntLiteral;
import decaf.ast.Len;
import decaf.ast.MethodDefinitionParameter;
import decaf.ast.ParenthesizedExpression;
import decaf.codegen.names.IrAssignableValue;
import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.ExpressionParameter;
import decaf.ast.FieldDeclaration;
import decaf.ast.Initialization;
import decaf.ast.LocationArray;
import decaf.ast.LocationAssignExpr;
import decaf.ast.LocationVariable;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallStatement;
import decaf.ast.Name;
import decaf.ast.Return;
import decaf.ast.StringLiteral;
import decaf.ast.UnaryOpExpression;

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

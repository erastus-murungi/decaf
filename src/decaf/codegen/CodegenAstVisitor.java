package decaf.codegen;

import decaf.ast.Assignment;
import decaf.ast.BinaryOpExpression;
import decaf.ast.Block;
import decaf.ast.BooleanLiteral;
import decaf.ast.CompoundAssignOpExpr;
import decaf.ast.ExpressionParameter;
import decaf.ast.FieldDeclaration;
import decaf.ast.Initialization;
import decaf.ast.IntLiteral;
import decaf.ast.Len;
import decaf.ast.LocationArray;
import decaf.ast.LocationAssignExpr;
import decaf.ast.LocationVariable;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallStatement;
import decaf.ast.MethodDefinitionParameter;
import decaf.ast.Name;
import decaf.ast.ParenthesizedExpression;
import decaf.ast.Return;
import decaf.ast.StringLiteral;
import decaf.ast.UnaryOpExpression;
import decaf.codegen.names.IrAssignable;

public interface CodegenAstVisitor<T> {
  T visit(
      BooleanLiteral booleanLiteral,
      IrAssignable resultLocation
  );

  T visit(
      IntLiteral intLiteral,
      IrAssignable resultLocation
  );

  T visit(
      StringLiteral stringLiteral,
      IrAssignable resultLocation
  );

  T visit(
      FieldDeclaration fieldDeclaration,
      IrAssignable resultLocation
  );

  T visit(
      UnaryOpExpression unaryOpExpression,
      IrAssignable resultLocation
  );

  T visit(
      BinaryOpExpression binaryOpExpression,
      IrAssignable resultLocation
  );

  T visit(
      Block block,
      IrAssignable resultLocation
  );

  T visit(
      ParenthesizedExpression parenthesizedExpression,
      IrAssignable resultLocation
  );

  T visit(
      LocationArray locationArray,
      IrAssignable resultLocation
  );

  T visit(
      ExpressionParameter expressionParameter,
      IrAssignable resultLocation
  );

  T visit(
      Return returnStatement,
      IrAssignable resultLocation
  );

  T visit(
      MethodCall methodCall,
      IrAssignable resultLocation
  );

  T visit(
      MethodCallStatement methodCallStatement,
      IrAssignable resultLocation
  );

  T visit(
      LocationAssignExpr locationAssignExpr,
      IrAssignable resultLocation
  );

  T visit(
      Name name,
      IrAssignable resultLocation
  );

  T visit(
      LocationVariable locationVariable,
      IrAssignable resultLocation
  );

  T visit(Len len);

  T visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      IrAssignable resultLocation
  );

  T visit(
      Initialization initialization,
      IrAssignable resultLocation
  );

  T visit(
      Assignment assignment,
      IrAssignable resultLocation
  );

  T visit(
      MethodDefinitionParameter methodDefinitionParameter,
      IrAssignable resultLocation
  );
}

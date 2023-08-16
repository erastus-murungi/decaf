package decaf.ir;

import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.Initialization;
import decaf.analysis.syntax.ast.IntLiteral;
import decaf.analysis.syntax.ast.Len;
import decaf.analysis.syntax.ast.LocationArray;
import decaf.analysis.syntax.ast.LocationAssignExpr;
import decaf.analysis.syntax.ast.LocationVariable;
import decaf.analysis.syntax.ast.MethodCall;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.MethodDefinitionParameter;
import decaf.analysis.syntax.ast.Name;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.ir.names.IrAssignable;

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

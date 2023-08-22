package decaf.shared;

import decaf.analysis.syntax.ast.Array;
import decaf.analysis.syntax.ast.AssignOpExpr;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.Break;
import decaf.analysis.syntax.ast.CharLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.Continue;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.For;
import decaf.analysis.syntax.ast.If;
import decaf.analysis.syntax.ast.ImportDeclaration;
import decaf.analysis.syntax.ast.Increment;
import decaf.analysis.syntax.ast.Initialization;
import decaf.analysis.syntax.ast.IntLiteral;
import decaf.analysis.syntax.ast.Len;
import decaf.analysis.syntax.ast.LocationArray;
import decaf.analysis.syntax.ast.LocationAssignExpr;
import decaf.analysis.syntax.ast.LocationVariable;
import decaf.analysis.syntax.ast.MethodCall;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.FormalArgument;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.RValue;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.analysis.syntax.ast.VoidExpression;
import decaf.analysis.syntax.ast.While;
import decaf.shared.env.Scope;

public interface AstVisitor<T> {
  T visit(IntLiteral intLiteral, Scope scope);

  T visit(BooleanLiteral booleanLiteral, Scope scope);

  T visit(FieldDeclaration fieldDeclaration, Scope scope);

  T visit(MethodDefinition methodDefinition, Scope scope);

  T visit(ImportDeclaration importDeclaration, Scope scope);

  T visit(For forStatement, Scope scope);

  T visit(Break breakStatement, Scope scope);

  T visit(Continue continueStatement, Scope scope);

  T visit(While whileStatement, Scope scope);

  T visit(Program program, Scope scope);

  T visit(UnaryOpExpression unaryOpExpression, Scope scope);

  T visit(BinaryOpExpression binaryOpExpression, Scope scope);

  T visit(Block block, Scope scope);

  T visit(
      ParenthesizedExpression parenthesizedExpression,
      Scope scope
  );

  T visit(LocationArray locationArray, Scope scope);

  T visit(ExpressionParameter expressionParameter, Scope scope);

  T visit(If ifStatement, Scope scope);

  T visit(Return returnStatement, Scope scope);

  T visit(Array array, Scope scope);

  T visit(MethodCall methodCall, Scope scope);

  T visit(MethodCallStatement methodCallStatement, Scope scope);

  T visit(LocationAssignExpr locationAssignExpr, Scope scope);

  T visit(AssignOpExpr assignOpExpr, Scope scope);

  T visit(FormalArgument formalArgument, Scope scope);

  T visit(RValue RValue, Scope scope);

  T visit(LocationVariable locationVariable, Scope scope);

  T visit(Len len, Scope scope);

  T visit(Increment increment, Scope scope);

  T visit(Decrement decrement, Scope scope);

  T visit(CharLiteral charLiteral, Scope scope);

  T visit(StringLiteral stringLiteral, Scope scope);

  T visit(CompoundAssignOpExpr compoundAssignOpExpr, Scope scope);

  T visit(Initialization initialization, Scope scope);

  T visit(Assignment assignment, Scope scope);

  T visit(VoidExpression voidExpression, Scope scope);
}

package decaf.shared;

import decaf.analysis.syntax.ast.*;
import decaf.analysis.syntax.ast.types.Type;
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

  T visit(Type type, Scope scope);

  T visit(FormalArguments formalArguments, Scope scope);
}

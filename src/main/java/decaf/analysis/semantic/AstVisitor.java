package decaf.analysis.semantic;

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
import decaf.analysis.syntax.ast.DecimalLiteral;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.For;
import decaf.analysis.syntax.ast.HexLiteral;
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
import decaf.analysis.syntax.ast.MethodDefinitionParameter;
import decaf.analysis.syntax.ast.Name;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.analysis.syntax.ast.VoidExpression;
import decaf.analysis.syntax.ast.While;
import decaf.shared.symboltable.SymbolTable;

public interface AstVisitor<T> {
  T visit(IntLiteral intLiteral, SymbolTable symbolTable);

  T visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable);

  T visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable);

  T visit(HexLiteral hexLiteral, SymbolTable symbolTable);

  T visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable);

  T visit(MethodDefinition methodDefinition, SymbolTable symbolTable);

  T visit(ImportDeclaration importDeclaration, SymbolTable symbolTable);

  T visit(For forStatement, SymbolTable symbolTable);

  T visit(Break breakStatement, SymbolTable symbolTable);

  T visit(Continue continueStatement, SymbolTable symbolTable);

  T visit(While whileStatement, SymbolTable symbolTable);

  T visit(Program program, SymbolTable symbolTable);

  T visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable);

  T visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable);

  T visit(Block block, SymbolTable symbolTable);

  T visit(ParenthesizedExpression parenthesizedExpression,
          SymbolTable symbolTable);

  T visit(LocationArray locationArray, SymbolTable symbolTable);

  T visit(ExpressionParameter expressionParameter, SymbolTable symbolTable);

  T visit(If ifStatement, SymbolTable symbolTable);

  T visit(Return returnStatement, SymbolTable symbolTable);

  T visit(Array array, SymbolTable symbolTable);

  T visit(MethodCall methodCall, SymbolTable symbolTable);

  T visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable);

  T visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable);

  T visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable);

  T visit(MethodDefinitionParameter methodDefinitionParameter,
          SymbolTable symbolTable);

  T visit(Name name, SymbolTable symbolTable);

  T visit(LocationVariable locationVariable, SymbolTable symbolTable);

  T visit(Len len, SymbolTable symbolTable);

  T visit(Increment increment, SymbolTable symbolTable);

  T visit(Decrement decrement, SymbolTable symbolTable);

  T visit(CharLiteral charLiteral, SymbolTable symbolTable);

  T visit(StringLiteral stringLiteral, SymbolTable symbolTable);

  T visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable symbolTable);

  T visit(Initialization initialization, SymbolTable symbolTable);

  T visit(Assignment assignment, SymbolTable symbolTable);

  T visit(VoidExpression voidExpression, SymbolTable symbolTable);
}

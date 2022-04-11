package edu.mit.compilers.ir;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable;

public interface Visitor<T> {
    List<DecafSemanticException> exceptions = new ArrayList<>();

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
     T visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable);
     T visit(LocationArray locationArray, SymbolTable symbolTable);
     T visit(ExpressionParameter expressionParameter, SymbolTable symbolTable);
     T visit(If ifStatement, SymbolTable symbolTable);
     T visit(Return returnStatement, SymbolTable symbolTable);
     T visit(Array array, SymbolTable symbolTable);
     T visit(MethodCall methodCall, SymbolTable symbolTable);
     T visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable);
     T visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable);
     T visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable);
     T visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable);
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
}

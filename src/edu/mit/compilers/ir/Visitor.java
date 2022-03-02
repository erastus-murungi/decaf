package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 

import java.util.ArrayList;
import java.util.List;

public interface Visitor<T> {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    T visit(IntLiteral intLiteral, SymbolTable<String, Descriptor> symbolTable);
    T visit(BooleanLiteral booleanLiteral, SymbolTable<String, Descriptor> symbolTable);
    T visit(DecimalLiteral decimalLiteral, SymbolTable<String, Descriptor> symbolTable);
    T visit(HexLiteral hexLiteral, SymbolTable<String, Descriptor> symbolTable);
    T visit(FieldDeclaration fieldDeclaration, SymbolTable<String, Descriptor> symbolTable);
    T visit(MethodDefinition methodDefinition, SymbolTable<String, Descriptor> symbolTable);
    T visit(ImportDeclaration importDeclaration, SymbolTable<String, Descriptor> symbolTable);
    T visit(For forStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(Break breakStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(Continue continueStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(While whileStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(Program program, SymbolTable<String, Descriptor> symbolTable);
    T visit(UnaryOpExpression unaryOpExpression, SymbolTable<String, Descriptor> symbolTable);
    T visit(BinaryOpExpression binaryOpExpression, SymbolTable<String, Descriptor> symbolTable);
    T visit(Block block, SymbolTable<String, Descriptor> symbolTable);
    T visit(ParenthesizedExpression parenthesizedExpression, SymbolTable<String, Descriptor> symbolTable);
    T visit(LocationArray locationArray, SymbolTable<String, Descriptor> symbolTable);
    T visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable<String, Descriptor> symbolTable);
    T visit(ExpressionParameter expressionParameter, SymbolTable<String, Descriptor> symbolTable);
    T visit(If ifStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(Return returnStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(Array array, SymbolTable<String, Descriptor> symbolTable);
    T visit(MethodCall methodCall, SymbolTable<String, Descriptor> symbolTable);
    T visit(MethodCallStatement methodCallStatement, SymbolTable<String, Descriptor> symbolTable);
    T visit(LocationAssignExpr locationAssignExpr, SymbolTable<String, Descriptor> symbolTable);
    T visit(AssignOpExpr assignOpExpr, SymbolTable<String, Descriptor> symbolTable);
    T visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable<String, Descriptor> symbolTable);
    T visit(Name name, SymbolTable<String, Descriptor> symbolTable);
    T visit(Location location, SymbolTable<String, Descriptor> symbolTable);
    T visit(Len len, SymbolTable<String, Descriptor> symbolTable);
    T visit(Increment increment, SymbolTable<String, Descriptor> symbolTable);
    T visit(Decrement decrement, SymbolTable<String, Descriptor> symbolTable);
    T visit(CharLiteral charLiteral, SymbolTable<String, Descriptor> symbolTable);
    T visit(MethodCallParameter methodCallParameter, SymbolTable<String, Descriptor> symbolTable);
    T visit(StringLiteral stringLiteral, SymbolTable<String, Descriptor> symbolTable);
}

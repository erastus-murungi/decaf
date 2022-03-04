package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 

import java.util.ArrayList;
import java.util.List;

public interface Visitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    void visit(BooleanLiteral booleanLiteral, SymbolTable<String, Descriptor> symbolTable);
    void visit(DecimalLiteral decimalLiteral, SymbolTable<String, Descriptor> symbolTable);
    void visit(HexLiteral hexLiteral, SymbolTable<String, Descriptor> symbolTable);
    void visit(FieldDeclaration fieldDeclaration, SymbolTable<String, Descriptor> symbolTable);
    void visit(MethodDefinition methodDefinition, SymbolTable<String, Descriptor> symbolTable);
    void visit(ImportDeclaration importDeclaration, SymbolTable<String, Descriptor> symbolTable);
    void visit(For forStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(Break breakStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(Continue continueStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(While whileStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(Program program, SymbolTable<String, Descriptor> symbolTable);
    void visit(UnaryOpExpression unaryOpExpression, SymbolTable<String, Descriptor> symbolTable);
    void visit(BinaryOpExpression binaryOpExpression, SymbolTable<String, Descriptor> symbolTable);
    void visit(Block block, SymbolTable<String, Descriptor> symbolTable);
    void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable<String, Descriptor> symbolTable);
    void visit(LocationArray locationArray, SymbolTable<String, Descriptor> symbolTable);
    void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable<String, Descriptor> symbolTable);
    void visit(ExpressionParameter expressionParameter, SymbolTable<String, Descriptor> symbolTable);
    void visit(If ifStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(Return returnStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(Array array, SymbolTable<String, Descriptor> symbolTable);
    void visit(MethodCall methodCall, SymbolTable<String, Descriptor> symbolTable);
    void visit(MethodCallStatement methodCallStatement, SymbolTable<String, Descriptor> symbolTable);
    void visit(LocationAssignExpr locationAssignExpr, SymbolTable<String, Descriptor> symbolTable);
    void visit(AssignOpExpr assignOpExpr, SymbolTable<String, Descriptor> symbolTable);
    void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable<String, Descriptor> symbolTable);
    void visit(Name name, SymbolTable<String, Descriptor> symbolTable);
    void visit(Location location, SymbolTable<String, Descriptor> symbolTable);
    void visit(Len len, SymbolTable<String, Descriptor> symbolTable);
    void visit(Increment increment, SymbolTable<String, Descriptor> symbolTable);
    void visit(Decrement decrement, SymbolTable<String, Descriptor> symbolTable);
    void visit(CharLiteral charLiteral, SymbolTable<String, Descriptor> symbolTable);
//    void visit(MethodCallParameter methodCallParameter, SymbolTable<String, Descriptor> symbolTable);
    void visit(StringLiteral stringLiteral, SymbolTable<String, Descriptor> symbolTable);
}

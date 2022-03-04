package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

import java.util.ArrayList;
import java.util.List;

public class IRVisitor implements Visitor<Void> {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    public Void visit(IntLiteral intLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(HexLiteral hexLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {return null;}
    public Void visit(MethodDefinition methodDefinition, SymbolTable symbolTable){return null;}
    public Void visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {return null;}
    public Void visit(For forStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Break breakStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Continue continueStatement, SymbolTable symbolTable) {return null;}
    public Void visit(While whileStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Program program, SymbolTable symbolTable) {return null;}
    public Void visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {return null;}
    public Void visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {return null;}
    public Void visit(Block block, SymbolTable symbolTable) {return null;}
    public Void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {return null;}
    public Void visit(LocationArray locationArray, SymbolTable symbolTable) {return null;}
    public Void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable symbolTable) {return null;}
    public Void visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {return null;}
    public Void visit(If ifStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Return returnStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Array array, SymbolTable symbolTable) {return null;}
    public Void visit(MethodCall methodCall, SymbolTable symbolTable) {return null;}
    public Void visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {return null;}
    public Void visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {return null;}
    public Void visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {return null;}
    public Void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {return null;}
    public Void visit(Name name, SymbolTable symbolTable) {return null;}
    public Void visit(Location location, SymbolTable symbolTable) {return null;}
    public Void visit(Len len, SymbolTable symbolTable) {return null;}
    public Void visit(Increment increment, SymbolTable symbolTable) {return null;}
    public Void visit(Decrement decrement, SymbolTable symbolTable) {return null;}
    public Void visit(CharLiteral charLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(MethodCallParameter methodCallParameter, SymbolTable symbolTable) {return null;}
    public Void visit(StringLiteral stringLiteral, SymbolTable symbolTable) {return null;}
}

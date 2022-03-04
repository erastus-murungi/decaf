package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

import java.util.ArrayList;
import java.util.List;

public class IRVisitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    public void visit(IntLiteral intLiteral, SymbolTable symbolTable) {}
    public void visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {}
    public void visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {}
    public void visit(HexLiteral hexLiteral, SymbolTable symbolTable) {}
    public void visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {}
    public void visit(MethodDefinition methodDefinition, SymbolTable symbolTable){}
    public void visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {}
    public void visit(For forStatement, SymbolTable symbolTable) {}
    public void visit(Break breakStatement, SymbolTable symbolTable) {}
    public void visit(Continue continueStatement, SymbolTable symbolTable) {}
    public void visit(While whileStatement, SymbolTable symbolTable) {}
    public void visit(Program program, SymbolTable symbolTable) {}
    public void visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {}
    public void visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {}
    public void visit(Block block, SymbolTable symbolTable) {}
    public void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {}
    public void visit(LocationArray locationArray, SymbolTable symbolTable) {}
    public void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable symbolTable) {}
    public void visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {}
    public void visit(If ifStatement, SymbolTable symbolTable) {}
    public void visit(Return returnStatement, SymbolTable symbolTable) {}
    public void visit(Array array, SymbolTable symbolTable) {}
    public void visit(MethodCall methodCall, SymbolTable symbolTable) {}
    public void visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {}
    public void visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {}
    public void visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {}
    public void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {}
    public void visit(Name name, SymbolTable symbolTable) {}
    public void visit(Location location, SymbolTable symbolTable) {}
    public void visit(Len len, SymbolTable symbolTable) {}
    public void visit(Increment increment, SymbolTable symbolTable) {}
    public void visit(Decrement decrement, SymbolTable symbolTable) {}
    public void visit(CharLiteral charLiteral, SymbolTable symbolTable) {}
    public void visit(MethodCallParameter methodCallParameter, SymbolTable symbolTable) {}
    public void visit(StringLiteral stringLiteral, SymbolTable symbolTable) {}
}

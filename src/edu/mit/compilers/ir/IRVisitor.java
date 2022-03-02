package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

import java.util.ArrayList;
import java.util.List;

public class IRVisitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    public void visit(IntLiteral intLiteral, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(BooleanLiteral booleanLiteral, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(DecimalLiteral decimalLiteral, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(HexLiteral hexLiteral, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(FieldDeclaration fieldDeclaration, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(MethodDefinition methodDefinition, SymbolTable<String, Descriptor> symbolTable){}
    public void visit(ImportDeclaration importDeclaration, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(For forStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Break breakStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Continue continueStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(While whileStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Program program, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(UnaryOpExpression unaryOpExpression, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(BinaryOpExpression binaryOpExpression, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Block block, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(LocationArray locationArray, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(ExpressionParameter expressionParameter, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(If ifStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Return returnStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Array array, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(MethodCall methodCall, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(MethodCallStatement methodCallStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(LocationAssignExpr locationAssignExpr, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(AssignOpExpr assignOpExpr, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Name name, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Location location, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Len len, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Increment increment, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Decrement decrement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(CharLiteral charLiteral, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(MethodCallParameter methodCallParameter, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(StringLiteral stringLiteral, SymbolTable<String, Descriptor> symbolTable) {}
}

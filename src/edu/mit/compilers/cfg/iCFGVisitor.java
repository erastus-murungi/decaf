package edu.mit.compilers.cfg;

import java.util.HashMap;

import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.AssignOpExpr;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Break;
import edu.mit.compilers.ast.CharLiteral;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.Continue;
import edu.mit.compilers.ast.DecimalLiteral;
import edu.mit.compilers.ast.Decrement;
import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.FieldDeclaration;
import edu.mit.compilers.ast.For;
import edu.mit.compilers.ast.HexLiteral;
import edu.mit.compilers.ast.If;
import edu.mit.compilers.ast.ImportDeclaration;
import edu.mit.compilers.ast.Increment;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationAssignExpr;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallStatement;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.ParenthesizedExpression;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.ast.While;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class iCFGVisitor implements Visitor<CFGPair> {
    public CFGBlock initialGlobalBlock = new NOP();
    public HashMap<String, CFGBlock> methodCFGBlocks = new HashMap<>();

    @Override
    public CFGPair visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(For forStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Break breakStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Continue continueStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(While whileStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Program program, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Block block, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(LocationArray locationArray, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(If ifStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Return returnStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Array array, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(MethodCall methodCall, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Name name, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Len len, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Increment increment, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(Decrement decrement, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    @Override
    public CFGPair visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        // TODO Auto-generated method stub
        return null;
    }
    // all one liners should just return blocks with itself as one line, and no pointers to children or parents
    
}

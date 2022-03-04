package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.*;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class IRVisitor implements Visitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    SymbolTable globals = new SymbolTable(null);
    SymbolTable methods = new SymbolTable(null);

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
    public void visit(If ifStatement, SymbolTable<String, Descriptor> symbolTable) {
        // variable lookup happens in ifCondition or if/else body
        for (Pair<String, AST> child: ifStatement.getChildren())
            child.second().accept(this, symbolTable);
    }
    public void visit(Return returnStatement, SymbolTable<String, Descriptor> symbolTable) {
        return;
    }
    public void visit(Array array, SymbolTable<String, Descriptor> symbolTable) {
        // add field variables during field declaration bc don't know type
        return;
    }
    public void visit(MethodCall methodCall, SymbolTable<String, Descriptor> symbolTable) {
        List<Pair<String, AST>> children = methodCall.getChildren();
        Name methodName = methodCall.nameId;

        if (!methods.containsEntry(methodName.id))
            exceptions.add(new DecafSemanticException(methodName.tokenPosition, methodName.id + "hasn't been defined yet"));

        for (MethodCallParameter parameter: methodCall.methodCallParameterList)
            parameter.accept(this, symbolTable);
    }
    public void visit(MethodCallStatement methodCallStatement, SymbolTable<String, Descriptor> symbolTable) {
        methodCallStatement.methodCall.accept(this, symbolTable);
    }
    public void visit(LocationAssignExpr locationAssignExpr, SymbolTable<String, Descriptor> symbolTable) {
        Name location = locationAssignExpr.location.name;
        // checking location has been initialized
        if (!symbolTable.containsEntry(location.id))
            exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "hasn't been defined yet"));
        // type-checking expr
        else {
            BuiltinType locationType = symbolTable.get(location.id).type;

            // Can only increment an int
            if (locationAssignExpr.assignExpr instanceof Increment && locationType != BuiltinType.Int)
                exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "needs to have type int in order to be incremented"));
            // location type has to match expression type
            else if (locationType != locationAssignExpr.assignExpr.expression.builtinType)
                exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "needs to have same type of expression"));
        }

        // update location variable in symbolTable, but how? Need evaluation of expr???
        // can we just store expr node in symbol table

    }
    public void visit(AssignOpExpr assignOpExpr, SymbolTable<String, Descriptor> symbolTable) {
        // no node for AssignOperator?
        assignOpExpr.expression.accept(this, symbolTable);
    }
    public void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable<String, Descriptor> symbolTable) {
        String paramName = methodDefinitionParameter.id.id;
        BuiltinType paramType = methodDefinitionParameter.builtinType;
        symbolTable.addEntry(paramName, new ParameterDescriptor(paramName, paramType));
    }
    public void visit(Name name, SymbolTable<String, Descriptor> symbolTable) { return; }
    public void visit(Location location, SymbolTable<String, Descriptor> symbolTable) {
        if (!symbolTable.containsEntry(location.name.id)) {
            exceptions.add(new DecafSemanticException(location.name.tokenPosition, "Locations must be defined"));
        }
    }
    public void visit(Len len, SymbolTable<String, Descriptor> symbolTable) {
        String arrayName = len.nameId.id;
        if (!symbolTable.containsEntry(arrayName) && (symbolTable.get(arrayName).type == BuiltinType.IntArray || symbolTable.get(arrayName).type == BuiltinType.BoolArray)) {
            exceptions.add(new DecafSemanticException(len.nameId.tokenPosition, "the argument of the len operator must be an array"));
        }
    }
    public void visit(Increment increment, SymbolTable<String, Descriptor> symbolTable) { return; }
    public void visit(Decrement decrement, SymbolTable<String, Descriptor> symbolTable) { return; }
    public void visit(CharLiteral charLiteral, SymbolTable<String, Descriptor> symbolTable) { return; }
//    public void visit(MethodCallParameter methodCallParameter, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(StringLiteral stringLiteral, SymbolTable<String, Descriptor> symbolTable) { return; }
}

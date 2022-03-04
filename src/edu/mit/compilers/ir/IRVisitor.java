package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.ImportDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.descriptors.VariableDescriptor;
import edu.mit.compilers.descriptors.ParameterDescriptor;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class IRVisitor implements Visitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    SymbolTable<String, Descriptor> fields = new SymbolTable<String, Descriptor>(null);
    SymbolTable<String, Descriptor> methods = new SymbolTable<String, Descriptor>(null);
    public TreeSet<String> imports = new TreeSet<>();
    
    public void visit(IntLiteral intLiteral, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, handled in assignment expression
        // int literal to check if
    }
    public void visit(BooleanLiteral booleanLiteral, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, handled in assignment expression
    }
    public void visit(DecimalLiteral decimalLiteral, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, handled in assignment expression
    }
    public void visit(HexLiteral hexLiteral, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, handled in assignment expression
    }
    public void visit(FieldDeclaration fieldDeclaration, SymbolTable<String, Descriptor> symbolTable) {
        BuiltinType type = fieldDeclaration.builtinType;
        for (Name name : fieldDeclaration.names){
            if (fields.containsKey(name.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ name.id+" already declared"));
            } else {
                // fields just declared do not have a value.
                fields.addEntry(name.id, new VariableDescriptor(name.id, null, type));
            }
        }
        for (Array array : fieldDeclaration.arrays){
            if (fields.containsKey(array.id.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ array.id.id+" already declared"));
            } else {
                // TODO: Check hex parse long
                fields.addEntry(array.id.id, new ArrayDescriptor(array.id.id, array.size.convertToLong(), type));
            }
        }
    }

    public void visit(MethodDefinition methodDefinition, SymbolTable<String, Descriptor> symbolTable){
        if (methods.containsKey(methodDefinition.methodName.id)){
            // method already defined. add an exception
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "Method name "+ methodDefinition.methodName.id+" already defined"));
        } else {
            SymbolTable<String, Descriptor> parameterSymbolTable = new SymbolTable<>(fields);
            SymbolTable<String, Descriptor> localSymbolTable = new SymbolTable<>(parameterSymbolTable);
            for (MethodDefinitionParameter parameter : methodDefinition.methodDefinitionParameterList){
                parameterSymbolTable.addEntry(parameter.id.id, new ParameterDescriptor(parameter.id.id, parameter.builtinType));
            }
            // visit the method definition and populate the local symbol table
            // TODO: encounter bug
            methodDefinition.block.accept(this, localSymbolTable);
            methods.addEntry(methodDefinition.methodName.id, new MethodDescriptor(methodDefinition, parameterSymbolTable, localSymbolTable));
        }   
    }

    public void visit(ImportDeclaration importDeclaration, SymbolTable<String, Descriptor> symbolTable) {
        if (imports.contains(importDeclaration.nameId.id)){
            exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "Import identifier "+ importDeclaration.nameId.id+" already declared"));
        } else {
            imports.add(importDeclaration.nameId.id);
        }
    }


    public void visit(For forStatement, SymbolTable<String, Descriptor> symbolTable) {
        // this is the name of our loop variable that we initialize in the creation of the for loop
        // for ( index = 0 ...) <-- index is the example here
        String initializedVariableName = forStatement.initId.id;

        // check if the variable exists
        if (symbolTable.containsEntry(initializedVariableName)){
            Descriptor initVariableDescriptor = symbolTable.getEntryValue(initializedVariableName);
            Expression initExpression = forStatement.initExpression;
            // update the symbol table to have the full expression
            symbolTable.updateEntry(initializedVariableName, new VariableDescriptor(initializedVariableName, initExpression, initVariableDescriptor.type));

            // visit the block
            this.visit(forStatement.block, symbolTable);
        } else {
            // the variable referred to was not declared. Add an exception.
            exceptions.add(new DecafSemanticException(forStatement.tokenPosition, "Variable "+initializedVariableName+" was not declared"));
        }
    }

    public void visit(Break breakStatement, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, break does not affect symbol table
    }

    public void visit(Continue continueStatement, SymbolTable<String, Descriptor> symbolTable) {
        // nothing to add, continue does not affect symbol table
    }

    public void visit(While whileStatement, SymbolTable<String, Descriptor> symbolTable) {}
    public void visit(Program program, SymbolTable<String, Descriptor> symbolTable) {
       

    }
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

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
import edu.mit.compilers.descriptors.MethodParameterDescriptor;
import java.util.ArrayList;
import java.util.List;

public class IRVisitor {
    List<DecafSemanticException> exceptions = new ArrayList<>();
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
        // TODO: what is the symbol table we are passing in? This is top-level. I'm assuming it's a field symbol table
        BuiltinType type = fieldDeclaration.builtinType;
        for (Name name : fieldDeclaration.names){
            if (symbolTable.containsKey(name.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ name.id+" already declared"));
            } else {
                // fields just declared do not have a value.
                symbolTable.addEntry(name.id, new VariableDescriptor(name.id, null, type));
            }
        }
        for (Array array : fieldDeclaration.arrays){
            if (symbolTable.containsKey(array.id.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ array.id.id+" already declared"));
            } else {
                // TODO: Check hex parse long
                symbolTable.addEntry(array.id.id, new ArrayDescriptor(array.id.id, array.size.convertToLong(), type));
            }
        }
    }

    public void visit(MethodDefinition methodDefinition, SymbolTable<String, Descriptor> symbolTable){
        if (symbolTable.containsKey(methodDefinition.methodName.id)){
            // method already defined. add an exception
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "Method name "+ methodDefinition.methodName.id+" already defined"));
        } else {
            SymbolTable<String, Descriptor> parameterSymbolTable = new SymbolTable<>(symbolTable);
            SymbolTable<String, Descriptor> localSymbolTable = new SymbolTable<>(symbolTable);
            for (MethodDefinitionParameter parameter : methodDefinition.methodDefinitionParameterList){
                parameterSymbolTable.addEntry(parameter.id.id, new MethodParameterDescriptor(parameter.id.id, parameter.builtinType));
            }
            // visit the method definition and populate the local symbol table
            this.visit(methodDefinition.block, localSymbolTable);
            symbolTable.addEntry(methodDefinition.methodName.id, new MethodDescriptor(methodDefinition, parameterSymbolTable, localSymbolTable));
        }   
    }

    public void visit(ImportDeclaration importDeclaration, SymbolTable<String, Descriptor> symbolTable) {
        if (symbolTable.containsKey(importDeclaration.nameId.id)){
            exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "Import identifier "+ importDeclaration.nameId.id+" already declared"));
        } else {
            symbolTable.addEntry(importDeclaration.nameId.id, new ImportDescriptor(BuiltinType.Import, importDeclaration.nameId.id));
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
        SymbolTable<String, Descriptor> globalSymbolTable = new SymbolTable<>(null);

    }
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

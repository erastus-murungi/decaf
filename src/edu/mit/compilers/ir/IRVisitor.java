package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.descriptors.*;
import edu.mit.compilers.symbolTable.SymbolTableType;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class IRVisitor implements Visitor<Void> {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    SymbolTable fields = new SymbolTable(null, SymbolTableType.Field);
    SymbolTable methods = new SymbolTable(null, SymbolTableType.Method);
    public TreeSet<String> imports = new TreeSet<>();

    public Void visit(IntLiteral intLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(HexLiteral hexLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        BuiltinType type = fieldDeclaration.builtinType;
        for (Name name : fieldDeclaration.names){
            if (fields.containsEntry(name.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ name.id+" already declared"));
            } else {
                // fields just declared do not have a value.
                fields.entries.put(name.id, new VariableDescriptor(name.id, null, type));
            }
        }
        for (Array array : fieldDeclaration.arrays){
            if (fields.containsEntry(array.id.id)){
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field "+ array.id.id+" already declared"));
            } else {
                // TODO: Check hex parse long
                fields.entries.put(array.id.id, new ArrayDescriptor(array.id.id, array.size.convertToLong(), type));
            }
        }
        return null;
    }
    public Void visit(MethodDefinition methodDefinition, SymbolTable symbolTable){
        if (methods.containsEntry(methodDefinition.methodName.id)){
            // method already defined. add an exception
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "Method name "+ methodDefinition.methodName.id+" already defined"));
        } else {
            SymbolTable parameterSymbolTable = new SymbolTable(fields, SymbolTableType.Parameter);
            SymbolTable localSymbolTable = new SymbolTable(parameterSymbolTable, SymbolTableType.Field);
            for (MethodDefinitionParameter parameter : methodDefinition.methodDefinitionParameterList){
                parameterSymbolTable.entries.put(parameter.id.id, new ParameterDescriptor(parameter.id.id, parameter.builtinType));
            }
            // visit the method definition and populate the local symbol table
            // TODO: encounter bug
            methodDefinition.block.accept(this, localSymbolTable);
            methods.entries.put(methodDefinition.methodName.id, new MethodDescriptor(methodDefinition, parameterSymbolTable, localSymbolTable));
        }
        return null;
    }
    public Void visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        if (imports.contains(importDeclaration.nameId.id)){
            exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "Import identifier "+ importDeclaration.nameId.id+" already declared"));
        } else {
            imports.add(importDeclaration.nameId.id);
        }
        return null;
    }
    public Void visit(For forStatement, SymbolTable symbolTable) {
        // this is the name of our loop variable that we initialize in the creation of the for loop
        // for ( index = 0 ...) <-- index is the example here
        String initializedVariableName = forStatement.initId.id;

        // check if the variable exists
        if (symbolTable.containsEntry(initializedVariableName)){
            Descriptor initVariableDescriptor = symbolTable.entries.get(initializedVariableName);
            Expression initExpression = forStatement.initExpression;
            // update the symbol table to have the full expression
            symbolTable.entries.put(initializedVariableName, new VariableDescriptor(initializedVariableName, initExpression, initVariableDescriptor.type));

            // visit the block
            forStatement.block.accept(this, symbolTable);
        } else {
            // the variable referred to was not declared. Add an exception.
            exceptions.add(new DecafSemanticException(forStatement.tokenPosition, "Variable "+initializedVariableName+" was not declared"));
        }
        return null;
    }
    public Void visit(Break breakStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Continue continueStatement, SymbolTable symbolTable) {return null;}
    public Void visit(While whileStatement, SymbolTable symbolTable) {
        whileStatement.test.accept(this, symbolTable);
        whileStatement.body.accept(this, symbolTable);
        return null;
    }
    public Void visit(Program program, SymbolTable symbolTable) {
        for (ImportDeclaration importDeclaration: program.importDeclarationList)
            imports.add(importDeclaration.nameId.id);
        for (FieldDeclaration fieldDeclaration: program.fieldDeclarationList)
            fieldDeclaration.accept(this, fields);
        for (MethodDefinition methodDefinition: program.methodDefinitionList)
            methodDefinition.accept(this, methods);
        return null;
    }
    public Void visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {return null;}
    public Void visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {return null;}
    public Void visit(Block block, SymbolTable symbolTable) {
        block.blockSymbolTable = new SymbolTable(symbolTable, SymbolTableType.Field);
        for (FieldDeclaration fieldDeclaration: block.fieldDeclarationList)
            fieldDeclaration.accept(this, block.blockSymbolTable);
        for (Statement statement: block.statementList)
            statement.accept(this, block.blockSymbolTable);
        return null;
    }
    public Void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {return null;}
    public Void visit(LocationArray locationArray, SymbolTable symbolTable) {
        final Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
        if (optionalDescriptor.isEmpty())
            exceptions.add(new DecafSemanticException(locationArray.tokenPosition, "Array " + locationArray.name.id + " not declared"));
        else if (!(optionalDescriptor.get() instanceof ArrayDescriptor))
            exceptions.add(new DecafSemanticException(locationArray.tokenPosition, locationArray.name.id + " is not an array"));
        locationArray.expression.accept(this, symbolTable);
        return null;
    }
    public Void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable symbolTable) {
        compoundAssignOpExpr.expression.accept(this, symbolTable);
        return null;
    }

    public Void visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {return null;}
    public Void visit(If ifStatement, SymbolTable symbolTable) {
        // variable lookup happens in ifCondition or if/else body
        for (Pair<String, AST> child: ifStatement.getChildren())
            child.second().accept(this, symbolTable);
        return null;
    }
    public Void visit(Return returnStatement, SymbolTable symbolTable) {return null;}
    public Void visit(Array array, SymbolTable symbolTable) {return null;}
    public Void visit(MethodCall methodCall, SymbolTable symbolTable) {
        List<Pair<String, AST>> children = methodCall.getChildren();
        Name methodName = methodCall.nameId;

        if (!methods.containsEntry(methodName.id))
            exceptions.add(new DecafSemanticException(methodName.tokenPosition, methodName.id + "hasn't been defined yet"));

        for (MethodCallParameter parameter: methodCall.methodCallParameterList)
            parameter.accept(this, symbolTable);
        return null;
    }
    public Void visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        methodCallStatement.methodCall.accept(this, symbolTable);
        return null;
    }
    public Void visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        Name location = locationAssignExpr.location.name;
        // checking location has been initialized
        if (!symbolTable.containsEntry(location.id))
            exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "hasn't been defined yet"));
            // type-checking expr
        else {
            BuiltinType locationType = symbolTable.entries.get(location.id).type;

            // Can only increment an int
            if (locationAssignExpr.assignExpr instanceof Increment && locationType != BuiltinType.Int)
                exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "needs to have type int in order to be incremented"));
                // location type has to match expression type
            else if (locationType != locationAssignExpr.assignExpr.expression.builtinType)
                exceptions.add(new DecafSemanticException(location.tokenPosition, location.id + "needs to have same type of expression"));
        }

        // update location variable in symbolTable, but how? Need evaluation of expr???
        // can we just store expr node in symbol table
        return null;
    }
    public Void visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // no node for AssignOperator?
        assignOpExpr.expression.accept(this, symbolTable);
        return null;
    }
    public Void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        String paramName = methodDefinitionParameter.id.id;
        BuiltinType paramType = methodDefinitionParameter.builtinType;
        symbolTable.entries.put(paramName, new ParameterDescriptor(paramName, paramType));
        return null;
    }
    public Void visit(Name name, SymbolTable symbolTable) {return null;}
    public Void visit(Location location, SymbolTable symbolTable) {
        if (!symbolTable.containsEntry(location.name.id)) {
            exceptions.add(new DecafSemanticException(location.name.tokenPosition, "Locations must be defined"));
        }
        return null;
    }
    public Void visit(Len len, SymbolTable symbolTable) {
        String arrayName = len.nameId.id;
        if (!symbolTable.containsEntry(arrayName) && (symbolTable.entries.get(arrayName).type == BuiltinType.IntArray || symbolTable.entries.get(arrayName).type == BuiltinType.BoolArray)) {
            exceptions.add(new DecafSemanticException(len.nameId.tokenPosition, "the argument of the len operator must be an array"));
        }
        return null;}
    public Void visit(Increment increment, SymbolTable symbolTable) {return null;}
    public Void visit(Decrement decrement, SymbolTable symbolTable) {return null;}
    public Void visit(CharLiteral charLiteral, SymbolTable symbolTable) {return null;}
    public Void visit(MethodCallParameter methodCallParameter, SymbolTable symbolTable) {return null;}
    public Void visit(StringLiteral stringLiteral, SymbolTable symbolTable) {return null;}
}

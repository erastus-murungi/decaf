package edu.mit.compilers.ir;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.descriptors.*;
import edu.mit.compilers.symbolTable.SymbolTableType;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public class IRVisitor implements Visitor<Void> {
    List<DecafSemanticException> exceptions = new ArrayList<>();
    SymbolTable fields = new SymbolTable(null, SymbolTableType.Field);
    SymbolTable methods = new SymbolTable(null, SymbolTableType.Method);
    public TreeSet<String> imports = new TreeSet<>();

    int depth = 0; // the number of nested while/for loops we are in

    public Void visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        BuiltinType type = fieldDeclaration.builtinType;
        for (Name name : fieldDeclaration.names) {
            if (symbolTable.isShadowingParameter(name.id)) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + name.id + " shadows a parameter"));
            } else if (symbolTable.parent == null && (methods.containsEntry(name.id) || imports.contains(name.id) || fields.containsEntry(name.id))){
                // global field already declared in global scope
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "(global) Field " + name.id + " already declared"));
            } else if (symbolTable.containsEntry(name.id)) {
                // field already declared in scope
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + name.id + " already declared"));
            } else {
                // fields just declared do not have a value.
                symbolTable.entries.put(name.id, new VariableDescriptor(name.id, null, type));
            }
        }
        for (Array array : fieldDeclaration.arrays) {
            if (symbolTable.isShadowingParameter(array.id.id)) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + array.id.id + " shadows a parameter"));
            } else if (symbolTable.getDescriptorFromValidScopes(array.id.id).isPresent()) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + array.id.id + " already declared"));
            } else {
                // TODO: Check hex parse long
                symbolTable.entries.put(array.id.id, new ArrayDescriptor(array.id.id, array.size.convertToLong(), type));
            }
        }
        return null;
    }

    private String simplify(List<MethodDefinitionParameter> methodDefinitionParameterList) {
        if (methodDefinitionParameterList.size() == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (MethodDefinitionParameter methodDefinitionParameter : methodDefinitionParameterList) {
            stringBuilder.append(Utils.coloredPrint(methodDefinitionParameter.builtinType.toString(), Utils.ANSIColorConstants.ANSI_PURPLE));
            stringBuilder.append(" ");
            stringBuilder.append(Utils.coloredPrint(methodDefinitionParameter.id.id, Utils.ANSIColorConstants.ANSI_WHITE));
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        return stringBuilder.toString();
    }

    private void checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
        for (MethodDefinition methodDefinition : methodDefinitionList) {
            if (methodDefinition.methodName.id.equals("main")) {
                if (methodDefinition.returnType == BuiltinType.Void) {
                    if (!(methodDefinition.methodDefinitionParameterList.size() == 0)) {
                        exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "main method must have no parameters, yours has: " + simplify(methodDefinition.methodDefinitionParameterList)));
                    }
                } else {
                    exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "main method return type must be void, not `" + methodDefinition.returnType + "`"));
                }
                return;
            }
        }
        exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "main method not found"));
    }

    public Void visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        if (methods.containsEntry(methodDefinition.methodName.id) || imports.contains(methodDefinition.methodName.id) || fields.containsEntry(methodDefinition.methodName.id)) {
            // method already defined. add an exception
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "Method name " + methodDefinition.methodName.id + " already defined"));
        } else {
            SymbolTable parameterSymbolTable = new SymbolTable(fields, SymbolTableType.Parameter);
            SymbolTable localSymbolTable = new SymbolTable(parameterSymbolTable, SymbolTableType.Field);
            for (MethodDefinitionParameter parameter : methodDefinition.methodDefinitionParameterList) {
                parameter.accept(this, parameterSymbolTable);
            }
            // visit the method definition and populate the local symbol table
            methods.entries.put(methodDefinition.methodName.id, new MethodDescriptor(methodDefinition, parameterSymbolTable, localSymbolTable));
            methodDefinition.block.accept(this, localSymbolTable);
        }
        return null;
    }

    public Void visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        if (symbolTable.isShadowingParameter(importDeclaration.nameId.id)) {
            exceptions.add(new DecafSemanticException(importDeclaration.nameId.tokenPosition, "Import identifier " + importDeclaration.nameId.id + " shadows a parameter"));
        } else if (imports.contains(importDeclaration.nameId.id)) {
            exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "Import identifier " + importDeclaration.nameId.id + " already declared"));
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
        if (symbolTable.getDescriptorFromValidScopes(initializedVariableName).isPresent()) {
            Descriptor initVariableDescriptor = symbolTable.entries.get(initializedVariableName);
            Expression initExpression = forStatement.initExpression;
            // update the symbol table to have the full expression
            symbolTable.entries.put(initializedVariableName, new VariableDescriptor(initializedVariableName, initExpression, initVariableDescriptor.type));

            // visit the block
            ++depth;
            forStatement.block.accept(this, symbolTable);
            --depth;
        } else {
            // the variable referred to was not declared. Add an exception.
            exceptions.add(new DecafSemanticException(forStatement.tokenPosition, "Variable " + initializedVariableName + " was not declared"));
        }
        return null;
    }

    public Void visit(Break breakStatement, SymbolTable symbolTable) {
        if (depth < 1) {
            exceptions.add(new DecafSemanticException(breakStatement.tokenPosition, "break statement not enclosed"));
        }
        return null;
    }

    public Void visit(Continue continueStatement, SymbolTable symbolTable) {
        if (depth < 1) {
            exceptions.add(new DecafSemanticException(continueStatement.tokenPosition, "continue statement not enclosed"));
        }
        return null;
    }

    public Void visit(While whileStatement, SymbolTable symbolTable) {
        whileStatement.test.accept(this, symbolTable);
        ++depth;
        whileStatement.body.accept(this, symbolTable);
        --depth;
        return null;
    }

    public Void visit(Program program, SymbolTable symbolTable) {
        checkMainMethodExists(program.methodDefinitionList);
        for (ImportDeclaration importDeclaration : program.importDeclarationList)
            imports.add(importDeclaration.nameId.id);
        for (FieldDeclaration fieldDeclaration : program.fieldDeclarationList)
            fieldDeclaration.accept(this, fields);
        for (MethodDefinition methodDefinition : program.methodDefinitionList)
            methodDefinition.accept(this, methods);
        return null;
    }

    public Void visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(Block block, SymbolTable symbolTable) {
        block.blockSymbolTable = new SymbolTable(symbolTable, SymbolTableType.Field);
        for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
            fieldDeclaration.accept(this, block.blockSymbolTable);
        for (Statement statement : block.statementList)
            statement.accept(this, block.blockSymbolTable);

        return null;
    }

    public Void visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        return null;
    }

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

    public Void visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(If ifStatement, SymbolTable symbolTable) {
        // variable lookup happens in ifCondition or if/else body
        for (Pair<String, AST> child : ifStatement.getChildren())
            child.second().accept(this, symbolTable);
        return null;
    }

    public Void visit(Return returnStatement, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(Array array, SymbolTable symbolTable) {
        if (Integer.parseInt(array.size.literal) <= 0) {
            exceptions.add(new DecafSemanticException(array.size.tokenPosition, "array declaration " + array.id + "[" + array.size.literal + "]" + " must be greater than 0"));
        }
        return null;
    }

    public Void visit(MethodCall methodCall, SymbolTable symbolTable) {
        Name methodName = methodCall.nameId;

        if (methods.getDescriptorFromValidScopes(methodName.id).isEmpty() && !imports.contains(methodName.id))
            exceptions.add(new DecafSemanticException(methodName.tokenPosition, "Method name " + methodName.id + " hasn't been defined yet"));

        for (MethodCallParameter parameter : methodCall.methodCallParameterList) {
            // TODO: partial rule 7 - array checking will be done in second pass
            if (!imports.contains(methodName.id) && parameter instanceof StringLiteral)
                exceptions.add(new DecafSemanticException(methodName.tokenPosition, "String " + parameter + " cannot be arguments to non-import methods"));
            parameter.accept(this, symbolTable);
        }
        return null;
    }

    public Void visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        methodCallStatement.methodCall.accept(this, symbolTable);
        return null;
    }

    public Void visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        Name location = locationAssignExpr.location.name;
        // checking location has been initialized
        if (symbolTable.isShadowingParameter(location.id))
            exceptions.add(new DecafSemanticException(location.tokenPosition, "Location " + location.id + " is shadowing a parameter"));
        else if (symbolTable.getDescriptorFromValidScopes(location.id).isEmpty())
            exceptions.add(new DecafSemanticException(location.tokenPosition, "Location " + location.id + " hasn't been defined yet"));

        locationAssignExpr.assignExpr.expression.accept(this, symbolTable);
        symbolTable.entries.put(location.id, new VariableDescriptor(location.id, locationAssignExpr.assignExpr.expression, null));
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
        if (symbolTable.isShadowingParameter(paramName))
            exceptions.add(new DecafSemanticException(methodDefinitionParameter.id.tokenPosition, "MethodDefinitionParameter " + paramName + " is shadowing a parameter"));
        else
            symbolTable.entries.put(paramName, new ParameterDescriptor(paramName, paramType));
        return null;
    }

    public Void visit(Name name, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(Location location, SymbolTable symbolTable) {
        if (symbolTable.getDescriptorFromValidScopes(location.name.id).isEmpty()) {
            exceptions.add(new DecafSemanticException(location.name.tokenPosition, "Location " + location.name.id + " must be defined"));
        }
        return null;
    }

    public Void visit(Len len, SymbolTable symbolTable) {
        String arrayName = len.nameId.id;
        if (symbolTable.getDescriptorFromValidScopes(arrayName).isPresent()) {
            exceptions.add(new DecafSemanticException(len.nameId.tokenPosition, "the argument of the len operator must be an array"));
        }
        return null;
    }

    public Void visit(Increment increment, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(Decrement decrement, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        return null;
    }
}

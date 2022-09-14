package edu.mit.compilers.ir;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import edu.mit.compilers.ast.AST;
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
import edu.mit.compilers.ast.Initialization;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationAssignExpr;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallParameter;
import edu.mit.compilers.ast.MethodCallStatement;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.ParenthesizedExpression;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.Statement;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.ast.While;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.descriptors.ParameterDescriptor;
import edu.mit.compilers.descriptors.VariableDescriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.symboltable.SymbolTableType;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

public class SemanticCheckerASTVisitor implements ASTVisitor<Void> {
    public TreeSet<String> imports = new TreeSet<>();
    SymbolTable fields = new SymbolTable(null, SymbolTableType.Field, null);
    SymbolTable methods = new SymbolTable(null, SymbolTableType.Method, null);
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
        Type type = fieldDeclaration.getType();
        for (Name name : fieldDeclaration.names) {
            if (symbolTable.isShadowingParameter(name.getLabel())) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + name.getLabel() + " shadows a parameter"));
            } else if (symbolTable.parent == null && (methods.containsEntry(name.getLabel()) || imports.contains(name.getLabel()) || fields.containsEntry(name.getLabel()))) {
                // global field already declared in global scope
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "(global) Field " + name.getLabel() + " already declared"));
            } else if (symbolTable.containsEntry(name.getLabel())) {
                // field already declared in scope
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + name.getLabel() + " already declared"));
            } else {
                // fields just declared do not have a value.
                symbolTable.entries.put(name.getLabel(), new VariableDescriptor(name.getLabel(), type, name));
            }
        }

        for (Array array : fieldDeclaration.arrays) {
            var arrayIdLabel = array.getId().getLabel();
            if (symbolTable.isShadowingParameter(array.getId().getLabel())) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + arrayIdLabel + " shadows a parameter"));
            } else if (symbolTable.getDescriptorFromValidScopes(array.getId().getLabel()).isPresent()) {
                exceptions.add(new DecafSemanticException(fieldDeclaration.tokenPosition, "Field " + arrayIdLabel + " already declared"));
            } else {
                // TODO: Check hex parse long
                symbolTable.entries.put(arrayIdLabel, new ArrayDescriptor(arrayIdLabel, array.getSize().convertToLong(), type == Type.Int ? Type.IntArray : (type == Type.Bool) ? Type.BoolArray : Type.Undefined, array));
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
            stringBuilder.append(Utils.coloredPrint(methodDefinitionParameter.getType().toString(), Utils.ANSIColorConstants.ANSI_PURPLE));
            stringBuilder.append(" ");
            stringBuilder.append(Utils.coloredPrint(methodDefinitionParameter.getName(), Utils.ANSIColorConstants.ANSI_WHITE));
            stringBuilder.append(", ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        return stringBuilder.toString();
    }

    private void checkMainMethodExists(List<MethodDefinition> methodDefinitionList) {
        for (MethodDefinition methodDefinition : methodDefinitionList) {
            if (methodDefinition.methodName.getLabel().equals("main")) {
                if (methodDefinition.returnType == Type.Void) {
                    if (!(methodDefinition.parameterList.size() == 0)) {
                        exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "main method must have no parameters, yours has: " + simplify(methodDefinition.parameterList)));
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
        SymbolTable parameterSymbolTable = new SymbolTable(fields, SymbolTableType.Parameter, methodDefinition.block);
        SymbolTable localSymbolTable = new SymbolTable(parameterSymbolTable, SymbolTableType.Field, methodDefinition.block);
        if (methods.containsEntry(methodDefinition.methodName.getLabel()) || imports.contains(methodDefinition.methodName.getLabel()) || fields.containsEntry(methodDefinition.methodName.getLabel())) {
            // method already defined. add an exception
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "Method name " + methodDefinition.methodName.getLabel() + " already defined"));
            methodDefinition.block.blockSymbolTable = localSymbolTable;
        } else {
            for (MethodDefinitionParameter parameter : methodDefinition.parameterList) {
                parameter.accept(this, parameterSymbolTable);
            }
            // visit the method definition and populate the local symbol table
            methods.entries.put(methodDefinition.methodName.getLabel(), new MethodDescriptor(methodDefinition, parameterSymbolTable, localSymbolTable));
            methodDefinition.block.accept(this, localSymbolTable);
        }
        return null;
    }

    public Void visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        if (symbolTable.isShadowingParameter(importDeclaration.nameId.getLabel())) {
            exceptions.add(new DecafSemanticException(importDeclaration.nameId.tokenPosition, "Import identifier " + importDeclaration.nameId.getLabel() + " shadows a parameter"));
        } else if (imports.contains(importDeclaration.nameId.getLabel())) {
            exceptions.add(new DecafSemanticException(new TokenPosition(0, 0, 0), "Import identifier " + importDeclaration.nameId.getLabel() + " already declared"));
        } else {
            imports.add(importDeclaration.nameId.getLabel());
        }
        return null;
    }

    public Void visit(For forStatement, SymbolTable symbolTable) {
        // this is the name of our loop irAssignableValue that we initialize in the creation of the for loop
        // for ( index = 0 ...) <-- index is the example here
        String initializedVariableName = forStatement.initialization.initLocation.getLabel();

        if (symbolTable.getDescriptorFromValidScopes(initializedVariableName).isPresent()) {
            ++depth;
            forStatement.block.accept(this, symbolTable);
            --depth;
        } else {
            // the irAssignableValue referred to was not declared. Add an exception.
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
            imports.add(importDeclaration.nameId.getLabel());
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
        SymbolTable blockST = new SymbolTable(symbolTable, SymbolTableType.Field, block);
        block.blockSymbolTable = blockST;
        symbolTable.children.add(blockST);
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
        final Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
        if (optionalDescriptor.isEmpty())
            exceptions.add(new DecafSemanticException(locationArray.tokenPosition, "Array " + locationArray.name.getLabel() + " not declared"));
        else if (!(optionalDescriptor.get() instanceof ArrayDescriptor))
            exceptions.add(new DecafSemanticException(locationArray.tokenPosition, locationArray.name.getLabel() + " is not an array"));
        locationArray.expression.accept(this, symbolTable);
        return null;
    }

    public Void visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable symbolTable) {
        compoundAssignOpExpr.expression.accept(this, symbolTable);
        return null;
    }

    @Override
    public Void visit(Initialization initialization, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public Void visit(Assignment assignment, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(If ifStatement, SymbolTable symbolTable) {
        // irAssignableValue lookup happens in ifCondition or if/else body
        for (Pair<String, AST> child : ifStatement.getChildren())
            child.second().accept(this, symbolTable);
        return null;
    }

    public Void visit(Return returnStatement, SymbolTable symbolTable) {
        if (!returnStatement.isTerminal())
            returnStatement.retExpression.accept(this, symbolTable);

        return null;
    }

    public Void visit(Array array, SymbolTable symbolTable) {
        if (Integer.parseInt(array.getSize().literal) <= 0) {
            exceptions.add(new DecafSemanticException(array.getSize().tokenPosition, "array declaration " + array.getId() + "[" + array.getSize().literal + "]" + " must be greater than 0"));
        }
        return null;
    }

    public Void visit(MethodCall methodCall, SymbolTable symbolTable) {
        Name methodName = methodCall.nameId;

        if (methods.getDescriptorFromValidScopes(methodName.getLabel()).isEmpty() && !imports.contains(methodName.getLabel()))
            exceptions.add(new DecafSemanticException(methodName.tokenPosition, "Method name " + methodName.getLabel() + " hasn't been defined yet"));

        for (MethodCallParameter parameter : methodCall.methodCallParameterList) {
            // TODO: partial rule 7 - array checking will be done in second pass
            if (!imports.contains(methodName.getLabel()) && parameter instanceof StringLiteral)
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
        locationAssignExpr.location.accept(this, symbolTable);
        return locationAssignExpr.assignExpr.accept(this, symbolTable);
    }

    public Void visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // no node for AssignOperator?
        assignOpExpr.expression.accept(this, symbolTable);
        return null;
    }

    public Void visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        String paramName = methodDefinitionParameter.getName();
        Type paramType = methodDefinitionParameter.getType();
        if (symbolTable.isShadowingParameter(paramName))
            exceptions.add(new DecafSemanticException(methodDefinitionParameter.tokenPosition, "MethodDefinitionParameter " + paramName + " is shadowing a parameter"));
        else
            symbolTable.entries.put(paramName, new ParameterDescriptor(paramName, paramType));
        return null;
    }

    public Void visit(Name name, SymbolTable symbolTable) {
        return null;
    }

    public Void visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        if (symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel()).isEmpty()) {
            exceptions.add(new DecafSemanticException(locationVariable.name.tokenPosition, "Location " + locationVariable.name.getLabel() + " must be defined"));
        }
        return null;
    }

    public Void visit(Len len, SymbolTable symbolTable) {
        String arrayName = len.nameId.getLabel();
        Optional<Descriptor> descriptor = symbolTable.getDescriptorFromValidScopes(arrayName);
        if (descriptor.isEmpty()) {
            exceptions.add(new DecafSemanticException(len.nameId.tokenPosition, arrayName + " not in scope"));
        } else if (!(descriptor.get() instanceof ArrayDescriptor)) {
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

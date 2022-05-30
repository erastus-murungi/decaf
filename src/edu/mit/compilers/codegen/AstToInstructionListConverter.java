package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.FieldDeclaration;
import edu.mit.compilers.ast.Initialization;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
import edu.mit.compilers.ast.Location;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationAssignExpr;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallParameter;
import edu.mit.compilers.ast.MethodCallStatement;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.ParenthesizedExpression;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.MethodReturn;
import edu.mit.compilers.codegen.codes.PushArgument;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Operators;

class AstToInstructionListConverter implements CodegenAstVisitor<InstructionList> {
    private final SymbolTable symbolTable;
    private final HashMap<String, StringLiteralAllocation> stringLiteralMapping;

    public AstToInstructionListConverter(SymbolTable symbolTable, HashMap<String, StringLiteralAllocation> stringLiteralMapping) {
        this.symbolTable = symbolTable;
        this.stringLiteralMapping = stringLiteralMapping;
    }

    @Override
    public InstructionList visit(LocationArray locationArray, AssignableName resultLocation) {
        final var indexInstructionList = locationArray.expression.accept(this, resultLocation);
        final var locationArrayInstructionList = new InstructionList();
        locationArrayInstructionList.addAll(indexInstructionList);

        final var maybeArrayDescriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
        final var arrayDescriptor = (ArrayDescriptor) maybeArrayDescriptorFromValidScopes.orElseThrow(
                () -> new IllegalStateException("expected to find array " + locationArray.name.getLabel() + " in scope")
        );
        final var getAddressInstruction = new GetAddress(locationArray,
                new VariableName(locationArray.name.getLabel(), arrayDescriptor.type), indexInstructionList.place, generateAddressName(arrayDescriptor.type), new ConstantName(arrayDescriptor.size, Type.Int));
        locationArrayInstructionList.add(getAddressInstruction);

        if (!(indexInstructionList.place instanceof ConstantName)) {
            locationArrayInstructionList.add(new ArrayBoundsCheck(getAddressInstruction, TemporaryNameIndexGenerator.getNextArrayBoundsCheckLabelIndex()));
        }
        locationArrayInstructionList.place = getAddressInstruction.getStore();
        return locationArrayInstructionList;
    }

    private Stack<AbstractName> flattenMethodCallArguments(InstructionList instructionList,
                                                           List<MethodCallParameter> methodCallParameterList) {
        var paramNames = new Stack<AbstractName>();
        for (MethodCallParameter methodCallParameter : methodCallParameterList) {
            var paramTACList = methodCallParameter.accept(this, null);
            instructionList.addAll(paramTACList);
            paramNames.add((paramTACList.place));
        }
        return paramNames;
    }

    private AssignableName resolveStoreLocation(Type type) {
        return TemporaryName.generateTemporaryName(type);
    }

    private AssignableName generateAddressName(Type type) {
        return new MemoryAddressName(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    @Override
    public InstructionList visit(MethodCall methodCall, AssignableName resultLocation) {
        final InstructionList instructionList = new InstructionList();
        var place = resolveStoreLocation(methodCall.getType());
        instructionList.add(new FunctionCallWithResult(methodCall, place, flattenMethodCallArguments(instructionList, methodCall.methodCallParameterList), methodCall.getSourceCode()));
        instructionList.place = place.copy();
        return instructionList;
    }


    @Override
    public InstructionList visit(MethodCallStatement methodCallStatement, AssignableName resultLocation) {
        InstructionList instructionList = new InstructionList();
        instructionList.add(new FunctionCallNoResult(methodCallStatement.methodCall, flattenMethodCallArguments(instructionList, methodCallStatement.methodCall.methodCallParameterList), methodCallStatement.getSourceCode()));
        return instructionList;
    }

    @Override
    public InstructionList visit(LocationAssignExpr locationAssignExpr, AssignableName resultLocation) {
        final InstructionList lhs = locationAssignExpr.location.accept(this, resultLocation);
        AssignableName locationVariable = (AssignableName) lhs.place;
        final InstructionList rhs = locationAssignExpr.assignExpr.accept(this, resultLocation);
        AbstractName valueVariable = rhs.place;
        lhs.addAll(rhs);
        lhs.add(flattenCompoundAssign(locationVariable, valueVariable, locationAssignExpr.getSourceCode(), locationAssignExpr));
        return lhs;
    }

    @Override
    public InstructionList visit(MethodDefinitionParameter methodDefinitionParameter, AssignableName resultLocation) {
        return new InstructionList(new VariableName(methodDefinitionParameter.getName(), methodDefinitionParameter.getType()));
    }

    @Override
    public InstructionList visit(Name name, AssignableName resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(name.getLabel())
                               .orElseThrow().type;
        return new InstructionList(new VariableName(name.getLabel(), type));
    }

    @Override
    public InstructionList visit(LocationVariable locationVariable, AssignableName resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel())
                               .orElseThrow().type;
        return new InstructionList(new VariableName(locationVariable.name.getLabel(), type));
    }

    @Override
    public InstructionList visit(Len len) {
        final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable
                .getDescriptorFromValidScopes(len.nameId.getLabel())
                .orElseThrow(() -> new IllegalStateException(len.nameId.getLabel() + " should be present"));
        return new InstructionList(new ConstantName(arrayDescriptor.size, Type.Int));
    }

    @Override
    public InstructionList visit(StringLiteral stringLiteral, AssignableName resultLocation) {
        return new InstructionList(new StringConstantName(stringLiteralMapping.get(stringLiteral.literal)));
    }

    @Override
    public InstructionList visit(CompoundAssignOpExpr compoundAssignOpExpr, AssignableName resultLocation) {
        return compoundAssignOpExpr.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(Initialization initialization, AssignableName resultLocation) {
        var initializationLocationInstructionList = initialization.initLocation.accept(this, resultLocation);
        var initializationExpressionInstructionList = initialization.initExpression.accept(this, resultLocation);
        var initializationInstructionList = new InstructionList(initializationLocationInstructionList.place);
        initializationInstructionList.addAll(initializationLocationInstructionList);
        initializationInstructionList.addAll(initializationExpressionInstructionList);
        initializationInstructionList.add(new Assign(initializationLocationInstructionList.place.copy(), initializationExpressionInstructionList.place.copy(), initialization, initialization.getSourceCode()));
        return initializationInstructionList;
    }

    private Instruction flattenCompoundAssign(AssignableName lhs, AbstractName rhs, String op, AST assignment) {
        final var constant = new ConstantName(1L, Type.Int);
        switch (op) {
            case Operators.ADD_ASSIGN:
                return new BinaryInstruction(lhs.copy(), lhs, Operators.PLUS, rhs, assignment.getSourceCode(), assignment);
            case Operators.MULTIPLY_ASSIGN:
                return new BinaryInstruction(lhs.copy(), lhs, Operators.MULTIPLY, rhs, assignment.getSourceCode(), assignment);
            case Operators.MINUS_ASSIGN:
                return new BinaryInstruction(lhs.copy(), lhs, Operators.MINUS, rhs, assignment.getSourceCode(), assignment);
            case Operators.DECREMENT:
                return new BinaryInstruction(lhs.copy(), lhs, Operators.MINUS, constant, assignment.getSourceCode(), assignment);
            case Operators.INCREMENT:
                return new BinaryInstruction(lhs.copy(), lhs, Operators.PLUS, constant, assignment.getSourceCode(), assignment);
            default:
                return new Assign(lhs.copy(), rhs.copy(), assignment, assignment.getSourceCode());
        }
    }

    @Override
    public InstructionList visit(Assignment assignment, AssignableName resultLocation) {
        var assignmentInstructionList = new InstructionList();
        var storeLocationInstructionList = assignment.location.accept(this, resultLocation);
        InstructionList operandInstructionList;
        if (assignment.assignExpr.expression != null) {
            operandInstructionList = assignment.assignExpr.expression.accept(this, storeLocationInstructionList.place.copy());
        } else {
            operandInstructionList = new InstructionList();
        }
        assignmentInstructionList.addAll(operandInstructionList);
        assignmentInstructionList.addAll(storeLocationInstructionList);

        if (!(assignment.getOperator()
                        .equals(Operators.ASSIGN) && storeLocationInstructionList.place.equals(operandInstructionList.place)))
            assignmentInstructionList.add(flattenCompoundAssign((AssignableName) storeLocationInstructionList.place, operandInstructionList.place, assignment.getOperator(), assignment));
        assignmentInstructionList.place = storeLocationInstructionList.place.copy();
        return assignmentInstructionList;
    }

    @Override
    public InstructionList visit(BooleanLiteral booleanLiteral, AssignableName resultLocation) {
        return new InstructionList(ConstantName.fromBooleanLiteral(booleanLiteral));
    }

    @Override
    public InstructionList visit(IntLiteral intLiteral, AssignableName resultLocation) {
        return new InstructionList(ConstantName.fromIntLiteral(intLiteral));
    }

    @Override
    public InstructionList visit(FieldDeclaration fieldDeclaration, AssignableName resultLocation) {
        InstructionList instructionList = new InstructionList();
        for (Name name : fieldDeclaration.names) {
            if (fieldDeclaration.getType().equals(Type.Int) || fieldDeclaration.getType().equals(Type.Bool)) {
                instructionList.add(new Assign(new VariableName(name.getLabel(), Type.Int), ConstantName.zero(), name, name.getLabel() + " = " + 0));
            }
        }
        for (Array array : fieldDeclaration.arrays) {
            var arrayName = new VariableName(array.getId()
                                                  .getLabel(), fieldDeclaration.getType());
            var arrayLength = new ConstantName(array.getSize()
                                                    .convertToLong(), Type.Int);
            for (long i = 0; i < array.getSize()
                                      .convertToLong(); i++) {
                var getAddressInstruction = new GetAddress(array, arrayName, new ConstantName(i, Type.Int), generateAddressName(fieldDeclaration.getType()), arrayLength);
                instructionList.add(getAddressInstruction);
                instructionList.add(new Assign(getAddressInstruction.getStore().copy(), ConstantName.zero(), array, String.format("%s[%s] = 0", arrayName, i)));
            }
        }
        return instructionList;
    }

    @Override
    public InstructionList visit(UnaryOpExpression unaryOpExpression, AssignableName resultLocation) {
        final var operandInstructionList = unaryOpExpression.operand.accept(this, resultLocation);
        final var unaryOpExpressionInstructionList = new InstructionList();
        unaryOpExpressionInstructionList.addAll(operandInstructionList);
        var place = resolveStoreLocation(unaryOpExpression.getType());
        unaryOpExpressionInstructionList.add(new UnaryInstruction(place, unaryOpExpression.getUnaryOperator()
                                                                                          .getSourceCode(), operandInstructionList.place.copy(), unaryOpExpression));
        unaryOpExpressionInstructionList.place = place;
        return unaryOpExpressionInstructionList;
    }

    @Override
    public InstructionList visit(BinaryOpExpression binaryOpExpression, AssignableName resultLocation) {
        final var firstOperandInstructionList = binaryOpExpression.lhs.accept(this, null);
        final var secondOperandInstructionList = binaryOpExpression.rhs.accept(this, null);

        final var binaryOpExpressionInstructionList = new InstructionList();
        binaryOpExpressionInstructionList.addAll(firstOperandInstructionList);
        binaryOpExpressionInstructionList.addAll(secondOperandInstructionList);

        binaryOpExpressionInstructionList.add(new BinaryInstruction(resultLocation, firstOperandInstructionList.place.copy(), binaryOpExpression.op.getSourceCode(),
                secondOperandInstructionList.place.copy(), binaryOpExpression.getSourceCode(), binaryOpExpression));
        binaryOpExpressionInstructionList.place = resultLocation.copy();
        return binaryOpExpressionInstructionList;
    }

    @Override
    public InstructionList visit(Block block, AssignableName resultLocation) {
        final InstructionList blockInstructionList = new InstructionList();
        block.fieldDeclarationList.forEach(fieldDeclaration -> blockInstructionList.addAll(fieldDeclaration.accept(this, null)));
        block.statementList.forEach(statement -> statement.accept(this, null));
        return blockInstructionList;
    }

    @Override
    public InstructionList visit(ParenthesizedExpression parenthesizedExpression, AssignableName resultLocation) {
        return parenthesizedExpression.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(ExpressionParameter expressionParameter, AssignableName resultLocation) {
        InstructionList expressionParameterInstructionList = new InstructionList();

        if (expressionParameter.expression instanceof LocationVariable) {
            // no need for temporaries
            expressionParameterInstructionList.place = new VariableName(((Location) expressionParameter.expression).name.getLabel(), expressionParameter.expression.getType());
        } else if (expressionParameter.expression instanceof IntLiteral) {
            expressionParameterInstructionList.place = new ConstantName(((IntLiteral) expressionParameter.expression).convertToLong(), Type.Int);
        } else {
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(expressionParameter.expression.getType());
            InstructionList expressionInstructionList = expressionParameter.expression.accept(this, resultLocation);
            expressionParameterInstructionList.addAll(expressionInstructionList);
            expressionParameterInstructionList.add(new Assign(temporaryVariable, expressionInstructionList.place.copy(), expressionParameter, temporaryVariable + " = " + expressionInstructionList.place));
            expressionParameterInstructionList.place = temporaryVariable.copy();
        }
        return expressionParameterInstructionList;
    }

    @Override
    public InstructionList visit(Return returnStatement, AssignableName resultLocation) {
        final InstructionList returnStatementInstructionList;
        if (returnStatement.retExpression != null) {
            InstructionList returnExpressionInstructionList = returnStatement.retExpression.accept(this, resultLocation);
            returnStatementInstructionList = new InstructionList();
            returnStatementInstructionList.addAll(returnExpressionInstructionList);
            returnStatementInstructionList.place = returnExpressionInstructionList.place;
            returnStatementInstructionList.add(new MethodReturn(returnStatement, returnStatementInstructionList.place));
        } else {
            returnStatementInstructionList = new InstructionList();
            returnStatementInstructionList.add(new MethodReturn(returnStatement));
        }
        return returnStatementInstructionList;
    }
}

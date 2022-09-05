package edu.mit.compilers.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
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
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.StringConstant;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Operators;

class AstToInstructionListConverter implements CodegenAstVisitor<InstructionList> {
    private final SymbolTable symbolTable;
    private final HashMap<String, StringLiteralAllocation> stringLiteralMapping;

    public AstToInstructionListConverter(SymbolTable symbolTable, HashMap<String, StringLiteralAllocation> stringLiteralMapping) {
        this.symbolTable = symbolTable;
        this.stringLiteralMapping = stringLiteralMapping;
    }

    @Override
    public InstructionList visit(LocationArray locationArray, LValue resultLocation) {
        final var indexInstructionList = locationArray.expression.accept(this, resultLocation);
        final var locationArrayInstructionList = new InstructionList();
        locationArrayInstructionList.addAll(indexInstructionList);

        final var maybeArrayDescriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
        final var arrayDescriptor = (ArrayDescriptor) maybeArrayDescriptorFromValidScopes.orElseThrow(
                () -> new IllegalStateException("expected to find array " + locationArray.name.getLabel() + " in scope")
        );
        final var getAddressInstruction = new GetAddress(locationArray,
                new Variable(locationArray.name.getLabel(), arrayDescriptor.type), indexInstructionList.place, generateAddressName(arrayDescriptor.type), new NumericalConstant(arrayDescriptor.size, Type.Int));
        locationArrayInstructionList.add(getAddressInstruction);

        if (!(indexInstructionList.place instanceof NumericalConstant)) {
            locationArrayInstructionList.add(new ArrayBoundsCheck(getAddressInstruction, TemporaryNameIndexGenerator.getNextArrayBoundsCheckLabelIndex()));
        }
        locationArrayInstructionList.place = getAddressInstruction.getDestination();
        return locationArrayInstructionList;
    }

    private Stack<Value> flattenMethodCallArguments(InstructionList instructionList,
                                                    List<MethodCallParameter> methodCallParameterList) {
        var paramNames = new Stack<Value>();
        for (MethodCallParameter methodCallParameter : methodCallParameterList) {
            var paramTACList = methodCallParameter.accept(this, null);
            instructionList.addAll(paramTACList);
            paramNames.add((paramTACList.place));
        }
        return paramNames;
    }

    private LValue resolveStoreLocation(Type type) {
        return Variable.genTemp(type);
    }

    private LValue resolveStoreLocation(LValue resultLocation, Type type) {
        if (resultLocation == null)
            return resolveStoreLocation(type);
        return resultLocation.copy();
    }

    private LValue generateAddressName(Type type) {
        return new MemoryAddress(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    @Override
    public InstructionList visit(MethodCall methodCall, LValue resultLocation) {
        final InstructionList instructionList = new InstructionList();
        var place = resolveStoreLocation(methodCall.getType());
        instructionList.add(new FunctionCallWithResult(methodCall, place, flattenMethodCallArguments(instructionList, methodCall.methodCallParameterList), methodCall.getSourceCode()));
        instructionList.place = place.copy();
        return instructionList;
    }


    @Override
    public InstructionList visit(MethodCallStatement methodCallStatement, LValue resultLocation) {
        InstructionList instructionList = new InstructionList();
        instructionList.add(new FunctionCallNoResult(methodCallStatement.methodCall, flattenMethodCallArguments(instructionList, methodCallStatement.methodCall.methodCallParameterList), methodCallStatement.getSourceCode()));
        return instructionList;
    }

    @Override
    public InstructionList visit(LocationAssignExpr locationAssignExpr, LValue resultLocation) {
        final InstructionList lhs = locationAssignExpr.location.accept(this, resultLocation);
        LValue locationVariable = (LValue) lhs.place;
        final InstructionList rhs = locationAssignExpr.assignExpr.accept(this, resultLocation);
        Value valueVariable = rhs.place;
        lhs.addAll(rhs);
        lhs.add(flattenCompoundAssign(locationVariable, valueVariable, locationAssignExpr.getSourceCode(), locationAssignExpr));
        return lhs;
    }

    @Override
    public InstructionList visit(MethodDefinitionParameter methodDefinitionParameter, LValue resultLocation) {
        return new InstructionList(new Variable(methodDefinitionParameter.getName(), methodDefinitionParameter.getType()));
    }

    @Override
    public InstructionList visit(Name name, LValue resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(name.getLabel())
                .orElseThrow().type;
        return new InstructionList(new Variable(name.getLabel(), type));
    }

    @Override
    public InstructionList visit(LocationVariable locationVariable, LValue resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel())
                .orElseThrow().type;
        return new InstructionList(new Variable(locationVariable.name.getLabel(), type));
    }

    @Override
    public InstructionList visit(Len len) {
        final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable
                .getDescriptorFromValidScopes(len.nameId.getLabel())
                .orElseThrow(() -> new IllegalStateException(len.nameId.getLabel() + " should be present"));
        return new InstructionList(new NumericalConstant(arrayDescriptor.size, Type.Int));
    }

    @Override
    public InstructionList visit(StringLiteral stringLiteral, LValue resultLocation) {
        return new InstructionList(new StringConstant(stringLiteralMapping.get(stringLiteral.literal)));
    }

    @Override
    public InstructionList visit(CompoundAssignOpExpr compoundAssignOpExpr, LValue resultLocation) {
        return compoundAssignOpExpr.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(Initialization initialization, LValue resultLocation) {
        var initializationLocationInstructionList = initialization.initLocation.accept(this, resultLocation);
        var initializationExpressionInstructionList = initialization.initExpression.accept(this, resultLocation);
        var initializationInstructionList = new InstructionList(initializationLocationInstructionList.place);
        initializationInstructionList.addAll(initializationLocationInstructionList);
        initializationInstructionList.addAll(initializationExpressionInstructionList);
        initializationInstructionList.add(new CopyInstruction(initializationLocationInstructionList.place.copy(), initializationExpressionInstructionList.place.copy(), initialization, initialization.getSourceCode()));
        return initializationInstructionList;
    }

    private Instruction flattenCompoundAssign(LValue lhs, Value rhs, String op, AST assignment) {
        final var constant = new NumericalConstant(1L, Type.Int);
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
                return new CopyInstruction(lhs.copy(), rhs.copy(), assignment, assignment.getSourceCode());
        }
    }

    @Override
    public InstructionList visit(Assignment assignment, LValue resultLocation) {
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
            assignmentInstructionList.add(flattenCompoundAssign((LValue) storeLocationInstructionList.place, operandInstructionList.place, assignment.getOperator(), assignment));
        assignmentInstructionList.place = storeLocationInstructionList.place.copy();
        return assignmentInstructionList;
    }

    @Override
    public InstructionList visit(BooleanLiteral booleanLiteral, LValue resultLocation) {
        return new InstructionList(NumericalConstant.fromBooleanLiteral(booleanLiteral));
    }

    @Override
    public InstructionList visit(IntLiteral intLiteral, LValue resultLocation) {
        return new InstructionList(NumericalConstant.fromIntLiteral(intLiteral));
    }

    @Override
    public InstructionList visit(FieldDeclaration fieldDeclaration, LValue resultLocation) {
        InstructionList instructionList = new InstructionList();
        for (Array array : fieldDeclaration.arrays) {
            var arrayName = new Variable(array.getId()
                    .getLabel(), fieldDeclaration.getType());
            var arrayLength = new NumericalConstant(array.getSize()
                    .convertToLong(), Type.Int);
            for (long i = 0; i < array.getSize()
                    .convertToLong(); i++) {
                var getAddressInstruction = new GetAddress(array, arrayName, new NumericalConstant(i, Type.Int), generateAddressName(fieldDeclaration.getType()), arrayLength);
                instructionList.add(getAddressInstruction);
                instructionList.add(new CopyInstruction(getAddressInstruction.getDestination().copy(), NumericalConstant.zero(), array, String.format("%s[%s] = 0", arrayName, i)));
            }
        }
        return instructionList;
    }

    @Override
    public InstructionList visit(UnaryOpExpression unaryOpExpression, LValue resultLocation) {
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
    public InstructionList visit(BinaryOpExpression binaryOpExpression, LValue resultLocation) {
        final var firstOperandInstructionList = binaryOpExpression.lhs.accept(this, null);
        final var secondOperandInstructionList = binaryOpExpression.rhs.accept(this, null);

        final var binaryOpExpressionInstructionList = new InstructionList();
        binaryOpExpressionInstructionList.addAll(firstOperandInstructionList);
        binaryOpExpressionInstructionList.addAll(secondOperandInstructionList);

        resultLocation = resolveStoreLocation(resultLocation, binaryOpExpression.getType());
        binaryOpExpressionInstructionList.add(new BinaryInstruction(resultLocation, firstOperandInstructionList.place.copy(), binaryOpExpression.op.getSourceCode(),
                secondOperandInstructionList.place.copy(), binaryOpExpression.getSourceCode(), binaryOpExpression));
        binaryOpExpressionInstructionList.place = resultLocation.copy();
        return binaryOpExpressionInstructionList;
    }

    @Override
    public InstructionList visit(Block block, LValue resultLocation) {
        final InstructionList blockInstructionList = new InstructionList();
        block.fieldDeclarationList.forEach(fieldDeclaration -> blockInstructionList.addAll(fieldDeclaration.accept(this, null)));
        block.statementList.forEach(statement -> statement.accept(this, null));
        return blockInstructionList;
    }

    @Override
    public InstructionList visit(ParenthesizedExpression parenthesizedExpression, LValue resultLocation) {
        return parenthesizedExpression.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(ExpressionParameter expressionParameter, LValue resultLocation) {
        InstructionList expressionParameterInstructionList = new InstructionList();

        if (expressionParameter.expression instanceof LocationVariable) {
            // no need for temporaries
            expressionParameterInstructionList.place = new Variable(((Location) expressionParameter.expression).name.getLabel(), expressionParameter.expression.getType());
        } else if (expressionParameter.expression instanceof IntLiteral) {
            expressionParameterInstructionList.place = new NumericalConstant(((IntLiteral) expressionParameter.expression).convertToLong(), Type.Int);
        } else {
            var temporaryVariable = Variable.genTemp(expressionParameter.expression.getType());
            InstructionList expressionInstructionList = expressionParameter.expression.accept(this, resultLocation);
            expressionParameterInstructionList.addAll(expressionInstructionList);
            expressionParameterInstructionList.add(new CopyInstruction(temporaryVariable, expressionInstructionList.place.copy(), expressionParameter, temporaryVariable + " = " + expressionInstructionList.place));
            expressionParameterInstructionList.place = temporaryVariable.copy();
        }
        return expressionParameterInstructionList;
    }

    @Override
    public InstructionList visit(Return returnStatement, LValue resultLocation) {
        final InstructionList returnStatementInstructionList;
        if (returnStatement.retExpression != null) {
            InstructionList returnExpressionInstructionList = returnStatement.retExpression.accept(this, resultLocation);
            returnStatementInstructionList = new InstructionList();
            returnStatementInstructionList.addAll(returnExpressionInstructionList);
            returnStatementInstructionList.place = returnExpressionInstructionList.place;
            returnStatementInstructionList.add(new ReturnInstruction(returnStatement, returnStatementInstructionList.place));
        } else {
            returnStatementInstructionList = new InstructionList();
            returnStatementInstructionList.add(new ReturnInstruction(returnStatement));
        }
        return returnStatementInstructionList;
    }
}

package edu.mit.compilers.codegen;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import edu.mit.compilers.codegen.codes.ReturnInstruction;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.IrGlobal;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrStringConstant;
import edu.mit.compilers.codegen.names.IrRegister;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Operators;

class AstToInstructionListConverter implements CodegenAstVisitor<InstructionList> {
    private final SymbolTable symbolTable;
    private final HashMap<String, IrStringConstant> stringLiteralMapping;
    private final Map<String, IrGlobal> globalAddressMap = new HashMap<>();

    public AstToInstructionListConverter(SymbolTable symbolTable,
                                         HashMap<String, IrStringConstant> stringLiteralMapping,
                                         Set<IrGlobal> irGlobals
    ) {
        this.symbolTable = symbolTable;
        this.stringLiteralMapping = stringLiteralMapping;
        for (var global : irGlobals) {
            globalAddressMap.put(global.getLabel(), global);
        }
    }

    private IrAssignableValue newLValue(String label, Type type) {
        return Objects.requireNonNullElseGet(
                globalAddressMap.get(label),
                () -> new IrRegister(label, type)
        );
    }

    @Override
    public InstructionList visit(LocationArray locationArray, IrAssignableValue resultLocation) {
        final var indexInstructionList = locationArray.expression.accept(this, resultLocation);
        final var locationArrayInstructionList = new InstructionList();
        locationArrayInstructionList.addAll(indexInstructionList);

        final var maybeArrayDescriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.getLabel());
        final var arrayDescriptor = (ArrayDescriptor) maybeArrayDescriptorFromValidScopes.orElseThrow(
                () -> new IllegalStateException("expected to find array " + locationArray.name.getLabel() + " in scope")
        );
        var base = newLValue(locationArray.name.getLabel(), arrayDescriptor.type);
        final var getAddressInstruction = new GetAddress(
                base,
                indexInstructionList.place,
                generateAddressName(arrayDescriptor.type, base, indexInstructionList.place),
                new IrIntegerConstant(arrayDescriptor.size, Type.Int),
                locationArray
        );
        locationArrayInstructionList.add(getAddressInstruction);

        if (!(indexInstructionList.place instanceof IrIntegerConstant)) {
            locationArrayInstructionList.add(new ArrayBoundsCheck(getAddressInstruction, TemporaryNameIndexGenerator.getNextArrayBoundsCheckLabelIndex()));
        }
        locationArrayInstructionList.place = getAddressInstruction.getDestination();
        return locationArrayInstructionList;
    }

    private Stack<IrValue> flattenMethodCallArguments(InstructionList instructionList,
                                                      List<MethodCallParameter> methodCallParameterList) {
        var paramNames = new Stack<IrValue>();
        for (MethodCallParameter methodCallParameter : methodCallParameterList) {
            var paramTACList = methodCallParameter.accept(this, null);
            instructionList.addAll(paramTACList);
            paramNames.add((paramTACList.place));
        }
        return paramNames;
    }

    private IrAssignableValue resolveStoreLocation(Type type) {
        return IrRegister.gen(type);
    }

    private IrAssignableValue resolveStoreLocation(IrAssignableValue resultLocation, Type type) {
        if (resultLocation == null)
            return resolveStoreLocation(type);
        return resultLocation.copy();
    }

    private IrMemoryAddress generateAddressName(Type type, IrAssignableValue base, IrValue index) {
        return new IrMemoryAddress(Type.lower(type), TemporaryNameIndexGenerator.getNextTemporaryVariable(), base, index);
    }

    @Override
    public InstructionList visit(MethodCall methodCall, IrAssignableValue resultLocation) {
        final InstructionList instructionList = new InstructionList();
        var place = resolveStoreLocation(methodCall.getType());
        instructionList.add(new FunctionCallWithResult(methodCall, place, flattenMethodCallArguments(instructionList, methodCall.methodCallParameterList), methodCall.getSourceCode()));
        instructionList.place = place.copy();
        return instructionList;
    }


    @Override
    public InstructionList visit(MethodCallStatement methodCallStatement, IrAssignableValue resultLocation) {
        InstructionList instructionList = new InstructionList();
        instructionList.add(new FunctionCallNoResult(methodCallStatement.methodCall, flattenMethodCallArguments(instructionList, methodCallStatement.methodCall.methodCallParameterList), methodCallStatement.getSourceCode()));
        return instructionList;
    }

    @Override
    public InstructionList visit(LocationAssignExpr locationAssignExpr, IrAssignableValue resultLocation) {
        final InstructionList lhs = locationAssignExpr.location.accept(this, resultLocation);
        IrRegister locationVariable = (IrRegister) lhs.place;
        final InstructionList rhs = locationAssignExpr.assignExpr.accept(this, resultLocation);
        IrValue irValueVariable = rhs.place;
        lhs.addAll(rhs);
        lhs.addAll(lowerAugmentedAssignment(locationVariable, irValueVariable, locationAssignExpr.getSourceCode(), locationAssignExpr));
        return lhs;
    }

    @Override
    public InstructionList visit(MethodDefinitionParameter methodDefinitionParameter, IrAssignableValue resultLocation) {
        return new InstructionList(new IrRegister(methodDefinitionParameter.getName(), methodDefinitionParameter.getType()));
    }

    @Override
    public InstructionList visit(Name name, IrAssignableValue resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(name.getLabel())
                               .orElseThrow().type;
        return new InstructionList(newLValue(name.getLabel(), type));
    }

    @Override
    public InstructionList visit(LocationVariable locationVariable, IrAssignableValue resultLocation) {
        Type type = symbolTable.getDescriptorFromValidScopes(locationVariable.name.getLabel())
                               .orElseThrow().type;
        return new InstructionList(newLValue(locationVariable.name.getLabel(), type));
    }

    @Override
    public InstructionList visit(Len len) {
        final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable
                .getDescriptorFromValidScopes(len.nameId.getLabel())
                .orElseThrow(() -> new IllegalStateException(len.nameId.getLabel() + " should be present"));
        return new InstructionList(new IrIntegerConstant(arrayDescriptor.size, Type.Int));
    }

    @Override
    public InstructionList visit(StringLiteral stringLiteral, IrAssignableValue resultLocation) {
        return new InstructionList(stringLiteralMapping.get(stringLiteral.literal));
    }

    @Override
    public InstructionList visit(CompoundAssignOpExpr compoundAssignOpExpr, IrAssignableValue resultLocation) {
        return compoundAssignOpExpr.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(Initialization initialization, IrAssignableValue resultLocation) {
        var initializationLocationInstructionList = initialization.initLocation.accept(this, resultLocation);
        var initializationExpressionInstructionList = initialization.initExpression.accept(this, resultLocation);
        var initializationInstructionList = new InstructionList(initializationLocationInstructionList.place);
        initializationInstructionList.addAll(initializationLocationInstructionList);
        initializationInstructionList.addAll(initializationExpressionInstructionList);
        initializationInstructionList.add(new CopyInstruction(initializationLocationInstructionList.place.copy(), initializationExpressionInstructionList.place.copy(), initialization, initialization.getSourceCode()));
        return initializationInstructionList;
    }

    private InstructionList lowerAugmentedAssignment(@NotNull IrValue lhs, IrValue rhs, @NotNull String op, @NotNull AST assignment) {
        var instructionList = new InstructionList(lhs);
        switch (op) {
            case Operators.ADD_ASSIGN, Operators.MINUS_ASSIGN, Operators.MULTIPLY_ASSIGN, Operators.DECREMENT, Operators.INCREMENT -> {
                final var constant = new IrIntegerConstant(1L, Type.Int);
                var dest = IrRegister.gen(lhs.getType());
                var inst = switch (op) {
                    case Operators.ADD_ASSIGN ->
                            new BinaryInstruction(dest, lhs.copy(), Operators.PLUS, rhs.copy(), assignment.getSourceCode(), assignment);
                    case Operators.MULTIPLY_ASSIGN ->
                            new BinaryInstruction(dest, lhs.copy(), Operators.MULTIPLY, rhs.copy(), assignment.getSourceCode(), assignment);
                    case Operators.MINUS_ASSIGN ->
                            new BinaryInstruction(dest, lhs.copy(), Operators.MINUS, rhs.copy(), assignment.getSourceCode(), assignment);
                    case Operators.DECREMENT ->
                            new BinaryInstruction(dest, lhs.copy(), Operators.MINUS, constant.copy(), assignment.getSourceCode(), assignment);
                    default ->
                            new BinaryInstruction(dest, lhs.copy(), Operators.PLUS, constant.copy(), assignment.getSourceCode(), assignment);
                };
                instructionList.add(inst);
                instructionList.add(CopyInstruction.noAstConstructor(lhs.copy(), dest.copy()));
            }
            default -> instructionList.add(new CopyInstruction(lhs.copy(), rhs.copy(), assignment, assignment.getSourceCode()));
        }
        return instructionList;
    }

    @Override
    public InstructionList visit(@NotNull Assignment assignment, IrAssignableValue resultLocation) {
        var assignmentInstructionList = new InstructionList();
        var storeLocationInstructionList = assignment.getLocation()
                                                     .accept(this, resultLocation);
        InstructionList operandInstructionList;
        if (assignment.assignExpr.expression != null) {
            if (Operators.isCompoundOperator(assignment.getOperator())) {
                var type = assignment.assignExpr.expression.getType();
                operandInstructionList = assignment.assignExpr.expression.accept(this, IrRegister.gen(type));
            } else {
                operandInstructionList = assignment.assignExpr.expression.accept(this, storeLocationInstructionList.place.copy());
            }
        } else {
            operandInstructionList = new InstructionList();
        }
        assignmentInstructionList.addAll(storeLocationInstructionList);
        assignmentInstructionList.addAll(operandInstructionList);

        if (!(assignment.getOperator()
                        .equals(Operators.ASSIGN) && storeLocationInstructionList.place.equals(operandInstructionList.place))) {
            assignmentInstructionList.addAll(lowerAugmentedAssignment(storeLocationInstructionList.place, operandInstructionList.place, assignment.getOperator(), assignment));
        }
        assignmentInstructionList.place = storeLocationInstructionList.place.copy();
        return assignmentInstructionList;
    }

    @Override
    public InstructionList visit(BooleanLiteral booleanLiteral, IrAssignableValue resultLocation) {
        return new InstructionList(IrIntegerConstant.fromBooleanLiteral(booleanLiteral));
    }

    @Override
    public InstructionList visit(IntLiteral intLiteral, IrAssignableValue resultLocation) {
        return new InstructionList(IrIntegerConstant.fromIntLiteral(intLiteral));
    }

    @Override
    public InstructionList visit(FieldDeclaration fieldDeclaration, IrAssignableValue resultLocation) {
        InstructionList instructionList = new InstructionList();
        for (Array array : fieldDeclaration.arrays) {
            var arrayName = new IrRegister(array.getId()
                                                .getLabel(), fieldDeclaration.getType());
            var arrayLength = new IrIntegerConstant(array.getSize()
                                                         .convertToLong(), Type.Int);
            for (long i = 0; i < array.getSize()
                                      .convertToLong(); i++) {
                var getAddressInstruction = new GetAddress(arrayName, new IrIntegerConstant(i, Type.Int), generateAddressName(fieldDeclaration.getType(), arrayName, IrIntegerConstant.zero()), arrayLength, array);
                instructionList.add(getAddressInstruction);
                instructionList.add(new CopyInstruction(getAddressInstruction.getDestination()
                                                                             .copy(), IrIntegerConstant.zero(), array, String.format("%s[%s] = 0", arrayName, i)));
            }
        }
        return instructionList;
    }

    @Override
    public InstructionList visit(UnaryOpExpression unaryOpExpression, IrAssignableValue resultLocation) {
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
    public InstructionList visit(BinaryOpExpression binaryOpExpression, IrAssignableValue resultLocation) {
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
    public InstructionList visit(Block block, IrAssignableValue resultLocation) {
        final InstructionList blockInstructionList = new InstructionList();
        block.fieldDeclarationList.forEach(fieldDeclaration -> blockInstructionList.addAll(fieldDeclaration.accept(this, null)));
        block.statementList.forEach(statement -> statement.accept(this, null));
        return blockInstructionList;
    }

    @Override
    public InstructionList visit(ParenthesizedExpression parenthesizedExpression, IrAssignableValue resultLocation) {
        return parenthesizedExpression.expression.accept(this, resultLocation);
    }

    @Override
    public InstructionList visit(ExpressionParameter expressionParameter, IrAssignableValue resultLocation) {
        InstructionList expressionParameterInstructionList = new InstructionList();

        if (expressionParameter.expression instanceof LocationVariable) {
            // no need for temporaries
            expressionParameterInstructionList.place = newLValue(((Location) expressionParameter.expression).name.getLabel(), expressionParameter.expression.getType());
        } else if (expressionParameter.expression instanceof IntLiteral) {
            expressionParameterInstructionList.place = new IrIntegerConstant(((IntLiteral) expressionParameter.expression).convertToLong(), Type.Int);
        } else {
            var temporaryVariable = IrRegister.gen(expressionParameter.expression.getType());
            InstructionList expressionInstructionList = expressionParameter.expression.accept(this, resultLocation);
            expressionParameterInstructionList.addAll(expressionInstructionList);
            expressionParameterInstructionList.add(new CopyInstruction(temporaryVariable, expressionInstructionList.place.copy(), expressionParameter, temporaryVariable + " = " + expressionInstructionList.place));
            expressionParameterInstructionList.place = temporaryVariable.copy();
        }
        return expressionParameterInstructionList;
    }

    @Override
    public InstructionList visit(Return returnStatement, IrAssignableValue resultLocation) {
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

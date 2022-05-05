package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;


import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class InstructionListConverter implements BasicBlockVisitor<InstructionList> {
    private final List<DecafException> errors;
    public Set<AbstractName> globalNames;

    private static class InstructionConverter implements Visitor<InstructionList> {
        HashMap<String, String> temporaryToStringLiteral;

        public InstructionConverter() {
            this.temporaryToStringLiteral = new HashMap<>();
        }

        @Override
        public InstructionList visit(IntLiteral intLiteral, SymbolTable symbolTable) {
            throw new IllegalStateException("unreachable AST node");
        }

        @Override
        public InstructionList visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation(BuiltinType.Bool);
            resetStoreLocation();
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromBooleanLiteral(booleanLiteral), booleanLiteral, place + " = " + booleanLiteral)));
        }

        @Override
        public InstructionList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation(BuiltinType.Int);
            resetStoreLocation();
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromIntLiteral(decimalLiteral), decimalLiteral, place + " = " + decimalLiteral)));
        }

        @Override
        public InstructionList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation(BuiltinType.Int);
            resetStoreLocation();
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromIntLiteral(hexLiteral), hexLiteral, place + " = " + hexLiteral)));
        }

        @Override
        public InstructionList visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
            InstructionList instructionList = new InstructionList();
            for (Name name: fieldDeclaration.names) {
                if (fieldDeclaration.builtinType.equals(BuiltinType.Int) || fieldDeclaration.builtinType.equals(BuiltinType.Bool)) {
                    instructionList.add(new Assign(new VariableName(name.id, Utils.WORD_SIZE, BuiltinType.Int), "=", ConstantName.zero(), name, name.id + " = " + 0));
                }

            }
            return instructionList;

        }

        @Override
        public InstructionList visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
            throw new IllegalStateException("A method definition is illegal");
        }

        @Override
        public InstructionList visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
            throw new IllegalStateException("An import statement is illegal");
        }

        @Override
        public InstructionList visit(For forStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A for statement is illegal");
        }

        @Override
        public InstructionList visit(Break breakStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A break statement is illegal");
        }

        @Override
        public InstructionList visit(Continue continueStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A continue statement is illegal");
        }

        @Override
        public InstructionList visit(While whileStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A while statement is illegal");
        }

        @Override
        public InstructionList visit(Program program, SymbolTable symbolTable) {
            throw new IllegalStateException("A program is illegal");
        }

        @Override
        public InstructionList visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
            final var operandTACList = unaryOpExpression.operand.accept(this, symbolTable);
            final var retTACList = new InstructionList();
            retTACList.addAll(operandTACList);
            var place = resolveStoreLocation(unaryOpExpression.builtinType);
            resetStoreLocation();
            retTACList.add(new UnaryInstruction(place, unaryOpExpression.op.getSourceCode(), operandTACList.place, unaryOpExpression));
            retTACList.place = place;
            return retTACList;
        }

        @Override
        public InstructionList visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
            final var cachedPlace = methodSetResultLocation;
            resetStoreLocation();

            final var leftTACList = binaryOpExpression.lhs.accept(this, symbolTable);
            final var rightTACList = binaryOpExpression.rhs.accept(this, symbolTable);

            final var binOpExpressionTACList = new InstructionList();
            binOpExpressionTACList.addAll(leftTACList);
            binOpExpressionTACList.addAll(rightTACList);

            AssignableName place;
            if (cachedPlace == null)
                place = resolveStoreLocation(binaryOpExpression.builtinType);
            else
                place = cachedPlace;
            resetStoreLocation();

            binOpExpressionTACList.add(
                    new BinaryInstruction(
                            place,
                            leftTACList.place,
                            binaryOpExpression.op.getSourceCode(),
                            rightTACList.place,
                            binaryOpExpression.getSourceCode(),
                            binaryOpExpression
                    ));
            binOpExpressionTACList.place = place;

            methodSetResultLocation = cachedPlace;
            return binOpExpressionTACList;
        }

        @Override
        public InstructionList visit(Block block, SymbolTable symbolTable) {
            final InstructionList instructionList = new InstructionList();
            for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
                instructionList.addAll(fieldDeclaration.accept(this, symbolTable));
            for (Statement statement : block.statementList)
                statement.accept(this, symbolTable);
            return instructionList;
        }

        @Override
        public InstructionList visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
            return parenthesizedExpression.expression.accept(this, symbolTable);
        }


        private void addArrayAccessBoundsCheck(
                InstructionList instructionList,
                ArrayAccess arrayAccess
        ) {

            final String boundsIndex = TemporaryNameGenerator.getNextBoundsCheckLabel();
            Label boundsBad = new Label("IndexIsLessThanArrayLengthCheckDone_" + boundsIndex, null);
            Label boundsGood = new Label("IndexIsNotNegativeCheckDone_" + boundsIndex, null);
            instructionList.add(new ArrayBoundsCheck(null, arrayAccess, null, boundsBad, boundsGood));
        }

        @Override
        public InstructionList visit(LocationArray locationArray, SymbolTable symbolTable) {
            final var locationThreeAddressCodeList = locationArray.expression.accept(this, symbolTable);
            final var threeAddressCodeList = new InstructionList();
            threeAddressCodeList.addAll(locationThreeAddressCodeList);

            final var descriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
            final var arrayDescriptor = (ArrayDescriptor) descriptorFromValidScopes.orElseThrow(
                    () -> new IllegalStateException("expected to find array " + locationArray.name.id + " in scope")
            );

            final var arrayName = new ArrayName(locationArray.name.id, arrayDescriptor.size * Utils.WORD_SIZE, arrayDescriptor.type, null);
            final var arrayAccess = new ArrayAccess(
                    locationArray, locationArray.getSourceCode(),
                    arrayName,
                    new ConstantName(arrayDescriptor.size, arrayDescriptor.type),
                    locationThreeAddressCodeList.place);
            arrayName.arrayAccess = arrayAccess;
            addArrayAccessBoundsCheck(threeAddressCodeList, arrayAccess);
            threeAddressCodeList.add(arrayAccess);
            threeAddressCodeList.place = arrayAccess.arrayName;
            return threeAddressCodeList;
        }

        @Override
        public InstructionList visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
            InstructionList expressionParameterTACList = new InstructionList();

            if (expressionParameter.expression instanceof LocationVariable) {
                // no need for temporaries
                expressionParameterTACList.place = new VariableName(((Location) expressionParameter.expression).name.id, Utils.WORD_SIZE, expressionParameter.expression.builtinType);
            } else if (expressionParameter.expression instanceof IntLiteral) {
                expressionParameterTACList.place = new ConstantName(((IntLiteral) expressionParameter.expression).convertToLong(), BuiltinType.Int);
            } else {
                TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(expressionParameter.expression.builtinType);
                InstructionList expressionTACList = expressionParameter.expression.accept(this, symbolTable);
                expressionParameterTACList.addAll(expressionTACList);
                expressionParameterTACList.add(new Assign(temporaryVariable, "=", expressionTACList.place, expressionParameter, temporaryVariable + " = " + expressionTACList.place));
                expressionParameterTACList.place = temporaryVariable;
            }
            return expressionParameterTACList;
        }

        @Override
        public InstructionList visit(If ifStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("An if statement is illegal");
        }

        @Override
        public InstructionList visit(Return returnStatement, SymbolTable symbolTable) {
            final InstructionList retTACList;
            if (returnStatement.retExpression != null) {
                InstructionList retExpressionTACList = returnStatement.retExpression.accept(this, symbolTable);
                retTACList = new InstructionList();
                retTACList.addAll(retExpressionTACList);
                retTACList.place = retExpressionTACList.place;
                retTACList.add(new MethodReturn(returnStatement, retTACList.place));
            } else {
                retTACList = new InstructionList();
                retTACList.add(new MethodReturn(returnStatement));
            }
            return retTACList;
        }

        @Override
        public InstructionList visit(Array array, SymbolTable symbolTable) {
            return null;
        }

        private void flattenMethodCallArguments(InstructionList instructionList,
                                                List<MethodCallParameter> methodCallParameterList,
                                                SymbolTable symbolTable) {
            var paramNames = new ArrayList<AbstractName>();
            for (MethodCallParameter methodCallParameter : methodCallParameterList) {
                var paramTACList = methodCallParameter.accept(this, symbolTable);
                instructionList.addAll(paramTACList);
                paramNames.add((paramTACList.place));
            }

            getPushParameterCode(instructionList, paramNames, methodCallParameterList);
        }

        private void getPushParameterCode(InstructionList instructionList,
                                          List<AbstractName> newParamNames,
                                          List<? extends AST> methodCallOrDefinitionArguments) {
            for (int i = newParamNames.size() - 1; i >= 0; i--) {
                final var pushParameter = new PushParameter(newParamNames.get(i), i, methodCallOrDefinitionArguments.get(i));
                instructionList.add(pushParameter);
                pushParameter.setComment("index of parameter = " + i);
            }
        }


        private AssignableName resolveStoreLocation(BuiltinType builtinType) {
            return Objects.requireNonNullElseGet(methodSetResultLocation, () -> TemporaryName.generateTemporaryName(builtinType));
        }

        private void resetStoreLocation() {
            methodSetResultLocation = null;
        }

        private void setStoreLocation(AssignableName place) {
            if (place != null && (!(place instanceof ArrayName)))
                methodSetResultLocation = place;
        }

        @Override
        public InstructionList visit(MethodCall methodCall, SymbolTable symbolTable) {
            final InstructionList instructionList = new InstructionList();
            flattenMethodCallArguments(instructionList, methodCall.methodCallParameterList, symbolTable);
            var place = resolveStoreLocation(methodCall.builtinType);
            resetStoreLocation();
            instructionList.add(new FunctionCallWithResult(methodCall, place, methodCall.getSourceCode()));
            instructionList.place = place;
            return instructionList;
        }


        @Override
        public InstructionList visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
            InstructionList instructionList = new InstructionList();
            flattenMethodCallArguments(instructionList, methodCallStatement.methodCall.methodCallParameterList, symbolTable);
            instructionList.add(new FunctionCallNoResult(methodCallStatement.methodCall, methodCallStatement.getSourceCode()));
            return instructionList;
        }

        @Override
        public InstructionList visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
            final InstructionList lhs = locationAssignExpr.location.accept(this, symbolTable);
            AssignableName locationVariable = (AssignableName) lhs.place;
            final InstructionList rhs = locationAssignExpr.assignExpr.accept(this, symbolTable);
            AbstractName valueVariable = rhs.place;
            lhs.addAll(rhs);
            lhs.add(new Assign(locationVariable, locationAssignExpr.getSourceCode(), valueVariable, locationAssignExpr, locationAssignExpr.assignExpr.getSourceCode()));
            return lhs;
        }

        @Override
        public InstructionList visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public InstructionList visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
            return new InstructionList(new VariableName(methodDefinitionParameter.id.id, Utils.WORD_SIZE, methodDefinitionParameter.builtinType));
        }

        @Override
        public InstructionList visit(Name name, SymbolTable symbolTable) {
            BuiltinType type = symbolTable.getDescriptorFromValidScopes(name.id)
                    .orElseThrow().type;
            return new InstructionList(new VariableName(name.id, Utils.WORD_SIZE, type));
        }

        @Override
        public InstructionList visit(LocationVariable locationVariable, SymbolTable symbolTable) {
            BuiltinType type = symbolTable.getDescriptorFromValidScopes(locationVariable.name.id)
                    .orElseThrow().type;
            return new InstructionList(new VariableName(locationVariable.name.id, Utils.WORD_SIZE, type));
        }

        @Override
        public InstructionList visit(Len len, SymbolTable symbolTable) {
            final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable
                    .getDescriptorFromValidScopes(len.nameId.id)
                    .orElseThrow(() -> new IllegalStateException(len.nameId.id + " should be present"));
            return new InstructionList(new ConstantName(arrayDescriptor.size, BuiltinType.Int));
        }

        @Override
        public InstructionList visit(Increment increment, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public InstructionList visit(Decrement decrement, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public InstructionList visit(CharLiteral charLiteral, SymbolTable symbolTable) {
            return new InstructionList(ConstantName.fromIntLiteral(charLiteral));
        }

        @Override
        public InstructionList visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
            StringLiteralStackAllocation label = literalStackAllocationHashMap.get(stringLiteral.literal);
            StringConstantName stringConstantName = new StringConstantName(label);
            return new InstructionList(stringConstantName);
        }

        @Override
        public InstructionList visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
            return compoundAssignOpExpr.expression.accept(this, curSymbolTable);
        }

        @Override
        public InstructionList visit(Initialization initialization, SymbolTable symbolTable) {
            InstructionList initIdThreeAddressList = initialization.initId.accept(this, symbolTable);
            setStoreLocation((AssignableName) initIdThreeAddressList.place);
            InstructionList initExpressionThreeAddressList = initialization.initExpression.accept(this, symbolTable);
            Assign copyInstruction = new Assign((AssignableName) initIdThreeAddressList.place, "=", initExpressionThreeAddressList.place, initialization, initialization.getSourceCode());

            InstructionList initializationInstructionList = new InstructionList(initIdThreeAddressList.place);
            initializationInstructionList.addAll(initIdThreeAddressList);
            initializationInstructionList.addAll(initExpressionThreeAddressList);
            initializationInstructionList.add(copyInstruction);
            resetStoreLocation();
            return initializationInstructionList;
        }

        @Override
        public InstructionList visit(Assignment assignment, SymbolTable symbolTable) {
            InstructionList returnTACList = new InstructionList();
            InstructionList lhs = assignment.location.accept(this, symbolTable);
            InstructionList rhs;
            if (assignment.assignExpr.expression != null) {
                if (assignment.getOperator()
                        .equals("="))
                    setStoreLocation((AssignableName) lhs.place);
                rhs = assignment.assignExpr.expression.accept(this, symbolTable);
            } else {
                rhs = new InstructionList();
            }
            returnTACList.addAll(rhs);
            returnTACList.addAll(lhs);

            if (assignment.assignExpr instanceof AssignOpExpr) {
                returnTACList.add(new Assign((AssignableName) lhs.place, ((AssignOpExpr) assignment.assignExpr).assignOp.getSourceCode(), rhs.place, assignment, assignment.getSourceCode()));
            } else if (assignment.assignExpr instanceof CompoundAssignOpExpr) {
                returnTACList.add(new Assign((AssignableName) lhs.place, ((CompoundAssignOpExpr) assignment.assignExpr).compoundAssignOp.getSourceCode(), rhs.place, assignment, assignment.getSourceCode()));
            } else if (assignment.assignExpr instanceof Decrement || assignment.assignExpr instanceof Increment) {
                returnTACList.add(new Assign((AssignableName) lhs.place, assignment.assignExpr.getSourceCode(), lhs.place, assignment.assignExpr, assignment.getSourceCode()));
            } else {
                returnTACList.add(new Assign((AssignableName) lhs.place, DecafScanner.ASSIGN, rhs.place, assignment, assignment.getSourceCode()));
            }
            resetStoreLocation();
            returnTACList.place = lhs.place;
            return returnTACList;
        }
    }

    final private static HashMap<String, StringLiteralStackAllocation> literalStackAllocationHashMap = new HashMap<>();

    InstructionConverter visitor;
    Label endLabelGlobal;
    Set<BasicBlock> visited = new HashSet<>();
    HashMap<BasicBlock, Label> blockToLabelHashMap = new HashMap<>();
    HashMap<BasicBlock, InstructionList> blockToCodeHashMap = new HashMap<>();
    static AssignableName methodSetResultLocation;
    public HashMap<String, SymbolTable> cfgSymbolTables;


    private MethodBegin convertMethodDefinition(MethodDefinition methodDefinition,
                                                BasicBlock methodStart,
                                                SymbolTable symbolTable) {

        endLabelGlobal = new Label("exit_" + methodDefinition.methodName.id, methodStart);

        var instructions = new InstructionList();
        var methodBegin = new MethodBegin(methodDefinition);
        instructions.add(methodBegin);

        instructions.addAll(
                errors.stream()
                        .map(error -> new RuntimeException(error.getMessage(), -2, error))
                        .collect(Collectors.toList()));

        flattenMethodDefinitionArguments(instructions, methodDefinition.methodDefinitionParameterList);

        final var firstBasicBlockTacList = methodStart.accept(this, symbolTable);
        firstBasicBlockTacList.addAll(0, instructions);

        firstBasicBlockTacList.addToTail(endLabelGlobal);

        firstBasicBlockTacList.addToTail(new MethodEnd(methodDefinition));
        methodStart.instructionList = firstBasicBlockTacList;
        methodBegin.unoptimized = firstBasicBlockTacList.flatten();
        methodBegin.entryBlock = methodStart;
        return methodBegin;
    }


    public void flattenMethodDefinitionArguments(InstructionList instructionList,
                                                 List<MethodDefinitionParameter> methodDefinitionParameterList) {

        for (int i = 0; i < methodDefinitionParameterList.size(); i++) {
            MethodDefinitionParameter parameter = methodDefinitionParameterList.get(i);
            instructionList.add(new PopParameter(
                    new VariableName(parameter.id.id, Utils.WORD_SIZE, parameter.builtinType),
                    parameter,
                    i,
                    "# index of parameter = " + i
            ));
        }
    }

    private MethodDefinition getMethodDefinitionFromProgram(String name, Program program) {
        for (MethodDefinition methodDefinition : program.methodDefinitionList) {
            if (methodDefinition.methodName.id.equals(name)) {
                return methodDefinition;
            }
        }
        throw new IllegalStateException("expected to find method " + name);
    }

    private InstructionList fillOutGlobals(List<FieldDeclaration> fieldDeclarationList) {
        InstructionList instructionList = new InstructionList();
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            for (Name name : fieldDeclaration.names) {
                instructionList.add(new GlobalAllocation(name, "# " + name.getSourceCode(),
                        new VariableName(name.id, Utils.WORD_SIZE, fieldDeclaration.builtinType), fieldDeclaration.builtinType.getFieldSize(), fieldDeclaration.builtinType));
            }
            for (Array array : fieldDeclaration.arrays) {
                long size = (fieldDeclaration.builtinType.getFieldSize() * array.size.convertToLong());
                instructionList.add(new GlobalAllocation(array, "# " + array.getSourceCode(),
                        new ArrayName(array.id.id,
                                size, fieldDeclaration.builtinType), size, fieldDeclaration.builtinType));
            }
        }
        getGlobals(instructionList);
        return instructionList;
    }


    private InstructionList initProgram(Program program) {
        InstructionList instructionList = fillOutGlobals(program.fieldDeclarationList);
        Set<String> stringLiteralList = findAllStringLiterals(program);
        for (String stringLiteral : stringLiteralList) {
            final StringLiteralStackAllocation literalStackAllocation = new StringLiteralStackAllocation(stringLiteral);
            instructionList.add(literalStackAllocation);
            literalStackAllocationHashMap.put(stringLiteral, literalStackAllocation);
        }
        return instructionList;
    }

    private void getGlobals(InstructionList instructionList) {
        globalNames = new HashSet<>();
        for (Instruction instruction : instructionList) {
            for (AbstractName name : instruction.getAllNames()) {
                if (name instanceof VariableName || name instanceof ArrayName)
                    globalNames.add(name);
            }
        }
    }

    private Set<String> findAllStringLiterals(Program program) {
        Set<String> literalList = new HashSet<>();
        Stack<AST> toExplore = new Stack<>();
        toExplore.addAll(program.methodDefinitionList);
        while (!toExplore.isEmpty()) {
            final AST node = toExplore.pop();
            if (node instanceof StringLiteral)
                literalList.add(((StringLiteral) node).literal);
            else {
                for (Pair<String, AST> astPair : node.getChildren()) {
                    toExplore.add(astPair.second());
                }
            }
        }
        return literalList;
    }

    public Pair<InstructionList, List<MethodBegin>> fill(iCFGVisitor visitor,
                                                         Program program) {
        var programStartTacList = initProgram(program);

        var methodsTacLists = new ArrayList<MethodBegin>();
        methodsTacLists.add(
                convertMethodDefinition(
                        getMethodDefinitionFromProgram("main", program),
                        visitor.methodCFGBlocks.get("main"),
                        cfgSymbolTables.get("main")
                ));

        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (!k.equals("main")) {
                methodsTacLists.add(convertMethodDefinition(getMethodDefinitionFromProgram(k, program), v, cfgSymbolTables.get(k)));
            }
        });
        return new Pair<>(programStartTacList, methodsTacLists);
    }

    public InstructionListConverter(CFGGenerator cfgGenerator) {
        this.errors = cfgGenerator.errors;
        this.visitor = new InstructionConverter();
        SymbolTableFlattener symbolTableFlattener = new SymbolTableFlattener(cfgGenerator.globalDescriptor);
        this.cfgSymbolTables = symbolTableFlattener.createCFGSymbolTables();
    }

    @Override
    public InstructionList visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable) {
        visited.add(basicBlockBranchLess);
        InstructionList universalInstructionList = new InstructionList();
        for (AST line : basicBlockBranchLess.lines) {
            universalInstructionList.addAll(line.accept(visitor, symbolTable));
        }
        blockToCodeHashMap.put(basicBlockBranchLess, universalInstructionList);
        if (!(basicBlockBranchLess instanceof NOP) && !(basicBlockBranchLess.autoChild instanceof NOP)) {
            if (visited.contains(basicBlockBranchLess.autoChild)) {
		        var label = blockToLabelHashMap.get(basicBlockBranchLess.autoChild);
                if (label == null) {
                    label = getLabel(basicBlockBranchLess.autoChild, null);
                }
                universalInstructionList.appendInInstructionListLinkedList(InstructionList.of(new UnconditionalJump(label)));
             } else {
                universalInstructionList.appendInInstructionListLinkedList(basicBlockBranchLess.autoChild.accept(this, symbolTable));
            }
        } else {
            universalInstructionList.addToTail(new UnconditionalJump(endLabelGlobal));
        }
        basicBlockBranchLess.instructionList = universalInstructionList;
        return universalInstructionList;
    }

    private Label getLabel(BasicBlock cfgBlock, Label from) {
        if (cfgBlock instanceof NOP) {
            return endLabelGlobal;
        }
        BiFunction<BasicBlock, Label, Label> function = (cfgBlock1, label) -> {
            if (label == null) {
                return new Label(TemporaryNameGenerator.getNextLabel(), cfgBlock1);
            } else {
                label.aliasLabels.add(from == null ? "haha" : from.label + "_False");
            }
            return label;
        };
        return blockToLabelHashMap.compute(cfgBlock, function);
    }

    private InstructionList getConditionTACList(Expression condition, SymbolTable symbolTable) {
        if (condition instanceof BinaryOpExpression)
            return visitor.visit((BinaryOpExpression) condition, symbolTable);
        else if (condition instanceof UnaryOpExpression)
            return visitor.visit((UnaryOpExpression) condition, symbolTable);
        else if (condition instanceof MethodCall)
            return visitor.visit((MethodCall) condition, symbolTable);
        else if (condition instanceof LocationVariable)
            return visitor.visit((LocationVariable) condition, symbolTable);
        else if (condition instanceof ParenthesizedExpression)
            return visitor.visit((ParenthesizedExpression) condition, symbolTable);
        else throw new IllegalStateException("an expression of type " + condition + " is not allowed");
    }

    private InstructionList getConditionalChildBlock(
            BasicBlock child,
            Label conditionalLabel,
            SymbolTable symbolTable) {

        InstructionList codeList;
        InstructionList trueBlock;

        if (!(child instanceof NOP)) {
            if (visited.contains(child)) {
                codeList = blockToCodeHashMap.get(child);
                Label label;
                if (codeList.get(0) instanceof Label)
                    label = (Label) codeList.get(0);
                else {
                    label = getLabel(child, conditionalLabel);
                    codeList.add(0, label);
                }
                trueBlock = InstructionList.of(new UnconditionalJump(label));
            } else
                trueBlock = child.accept(this, symbolTable);
        } else {
            trueBlock = InstructionList.of(new UnconditionalJump(endLabelGlobal));
        }
        return trueBlock;
    }

    @Override
    public InstructionList visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable) {
        visited.add(basicBlockWithBranch);

        final Expression condition = basicBlockWithBranch.condition;
        final InstructionList testConditionThreeAddressList = getConditionTACList(condition, symbolTable);
        final Label conditionLabel = getLabel(basicBlockWithBranch, null);

        final InstructionList conditionLabelTACList = InstructionList.of(conditionLabel);
        conditionLabelTACList.addAll(testConditionThreeAddressList);

        blockToCodeHashMap.put(basicBlockWithBranch, conditionLabelTACList);

        final InstructionList trueBlock = getConditionalChildBlock(basicBlockWithBranch.trueChild, conditionLabel, symbolTable);
        final InstructionList falseBlock = getConditionalChildBlock(basicBlockWithBranch.falseChild, conditionLabel, symbolTable);

        final Label falseLabel = getLabel(basicBlockWithBranch.falseChild, conditionLabel);
        final Label endLabel = new Label(conditionLabel.label + "end", null);

        ConditionalJump jumpIfFalse =
                new ConditionalJump(condition,
                        testConditionThreeAddressList.place,
                        falseLabel, "if !(" + basicBlockWithBranch.condition.getSourceCode() + ")");
        conditionLabelTACList.add(jumpIfFalse);
        if (!(trueBlock.lastCodeInLinkedList() instanceof UnconditionalJump))
            trueBlock.addToTail(new UnconditionalJump(endLabel));

        // no need for consecutive similar labels
        if (!falseBlock.isEmpty() && falseBlock.firstCode() instanceof UnconditionalJump) {
            UnconditionalJump unconditionalJump = (UnconditionalJump) falseBlock.firstCode();
            if (!unconditionalJump.goToLabel.label.equals(falseLabel.label))
                falseBlock.add(0, falseLabel);
        } else if (!falseBlock.firstCode()
                .equals(falseLabel))
            falseBlock.add(0, falseLabel);
        if (!(falseBlock.lastCodeInLinkedList() instanceof UnconditionalJump) && !(trueBlock.lastCodeInLinkedList() instanceof UnconditionalJump))
            falseBlock.addToTail(new UnconditionalJump(endLabel));
        if (falseBlock.flattenedSize() == 1 && falseBlock.firstCode() instanceof UnconditionalJump) {
            conditionLabelTACList.appendInInstructionListLinkedList(trueBlock);
            basicBlockWithBranch.instructionList = conditionLabelTACList;
            return conditionLabelTACList;
        }
        conditionLabelTACList
                .appendInInstructionListLinkedList(trueBlock)
                .appendInInstructionListLinkedList(falseBlock);
        basicBlockWithBranch.instructionList = conditionLabelTACList;
        return conditionLabelTACList;
    }

    @Override
    public InstructionList visit(NOP nop, SymbolTable symbolTable) {
        nop.instructionList = new InstructionList();
        return nop.instructionList;
    }
}

package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;


import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ThreeAddressCodesListConverter implements BasicBlockVisitor<ThreeAddressCodeList> {
    private final List<DecafException> errors;
    public Set<AbstractName> globalNames;

    private static class ThreeAddressCodesConverter implements Visitor<ThreeAddressCodeList> {
        HashMap<String, String> temporaryToStringLiteral;

        public ThreeAddressCodesConverter() {
            this.temporaryToStringLiteral = new HashMap<>();
        }

        @Override
        public ThreeAddressCodeList visit(IntLiteral intLiteral, SymbolTable symbolTable) {
            throw new IllegalStateException("unreachable AST node");
        }

        @Override
        public ThreeAddressCodeList visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation();
            resetStoreLocation();
            return new ThreeAddressCodeList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromBooleanLiteral(booleanLiteral), booleanLiteral, place + " = " + booleanLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation();
            resetStoreLocation();
            return new ThreeAddressCodeList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromIntLiteral(decimalLiteral), decimalLiteral, place + " = " + decimalLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation();
            resetStoreLocation();
            return new ThreeAddressCodeList(
                    place,
                    Collections.singletonList(new Assign(place, "=", ConstantName.fromIntLiteral(hexLiteral), hexLiteral, place + " = " + hexLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        }

        @Override
        public ThreeAddressCodeList visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
            throw new IllegalStateException("A method definition is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
            throw new IllegalStateException("An import statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(For forStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A for statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(Break breakStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A break statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(Continue continueStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A continue statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(While whileStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("A while statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(Program program, SymbolTable symbolTable) {
            throw new IllegalStateException("A program is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
            final var operandTACList = unaryOpExpression.operand.accept(this, symbolTable);
            final var retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            retTACList.add(operandTACList);
            var place = resolveStoreLocation();
            resetStoreLocation();
            retTACList.addCode(new Triple(place, unaryOpExpression.op.getSourceCode(), operandTACList.place, unaryOpExpression));
            retTACList.place = place;
            return retTACList;
        }

        @Override
        public ThreeAddressCodeList visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
            final var leftTACList = binaryOpExpression.lhs.accept(this, symbolTable);
            final var rightTACList = binaryOpExpression.rhs.accept(this, symbolTable);

            final var binOpExpressionTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            binOpExpressionTACList.add(leftTACList);
            binOpExpressionTACList.add(rightTACList);
            var place = resolveStoreLocation();
            resetStoreLocation();
            binOpExpressionTACList.addCode(
                    new Quadruple(
                            place, leftTACList.place, binaryOpExpression.op.getSourceCode(), rightTACList.place, binaryOpExpression.getSourceCode(), binaryOpExpression
                    ));
            binOpExpressionTACList.place = place;
            return binOpExpressionTACList;
        }

        @Override
        public ThreeAddressCodeList visit(Block block, SymbolTable symbolTable) {
            final ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
                threeAddressCodeList.add(fieldDeclaration.accept(this, symbolTable));
            for (Statement statement : block.statementList)
                statement.accept(this, symbolTable);
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
            return parenthesizedExpression.expression.accept(this, symbolTable);
        }


        private void addArrayAccessBoundsCheck(
                ThreeAddressCodeList threeAddressCodeList,
                ArrayAccess arrayAccess
        ) {

            final String boundsIndex = TemporaryNameGenerator.getNextBoundsCheckLabel();
            Label boundsBad = new Label("IndexIsLessThanArrayLengthCheckDone_" + boundsIndex, null);
            Label boundsGood = new Label("IndexIsNotNegativeCheckDone_" + boundsIndex, null);
            threeAddressCodeList.addCode(new ArrayBoundsCheck(null, arrayAccess, null, boundsBad, boundsGood));
        }

        @Override
        public ThreeAddressCodeList visit(LocationArray locationArray, SymbolTable symbolTable) {
            final var locationThreeAddressCodeList = locationArray.expression.accept(this, symbolTable);
            final var threeAddressCodeList = ThreeAddressCodeList.empty();
            threeAddressCodeList.add(locationThreeAddressCodeList);

            final var descriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
            final var arrayDescriptor = (ArrayDescriptor) descriptorFromValidScopes.orElseThrow(
                    () -> new IllegalStateException("expected to find array " + locationArray.name.id + " in scope")
            );

            final var arrayName = new ArrayName(locationArray.name.id, arrayDescriptor.size * Utils.WORD_SIZE);
            final var arrayAccess = new ArrayAccess(
                    locationArray, locationArray.getSourceCode(),
                    arrayName,
                    new ConstantName(arrayDescriptor.size),
                    locationThreeAddressCodeList.place);
            addArrayAccessBoundsCheck(threeAddressCodeList, arrayAccess);
            threeAddressCodeList.addCode(arrayAccess);
            threeAddressCodeList.place = arrayAccess.arrayName;
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
            ThreeAddressCodeList expressionParameterTACList = ThreeAddressCodeList.empty();

            if (expressionParameter.expression instanceof LocationVariable) {
                // no need for temporaries
                expressionParameterTACList.place = new VariableName(((Location) expressionParameter.expression).name.id);
            } else if (expressionParameter.expression instanceof IntLiteral) {
                expressionParameterTACList.place = new ConstantName(((IntLiteral) expressionParameter.expression).convertToLong());
            } else {
                TemporaryName temporaryVariable = TemporaryName.generateTemporaryName();
                ThreeAddressCodeList expressionTACList = expressionParameter.expression.accept(this, symbolTable);
                expressionParameterTACList.add(expressionTACList);
                expressionParameterTACList.addCode(new Assign(temporaryVariable, "=", expressionTACList.place, expressionParameter, temporaryVariable + " = " + expressionTACList.place));
                expressionParameterTACList.place = temporaryVariable;
            }
            return expressionParameterTACList;
        }

        @Override
        public ThreeAddressCodeList visit(If ifStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("An if statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(Return returnStatement, SymbolTable symbolTable) {
            final ThreeAddressCodeList retTACList;
            if (returnStatement.retExpression != null) {
                ThreeAddressCodeList retExpressionTACList = returnStatement.retExpression.accept(this, symbolTable);
                retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
                retTACList.add(retExpressionTACList);
                retTACList.place = retExpressionTACList.place;
                retTACList.addCode(new MethodReturn(returnStatement, (AssignableName) retTACList.place));
            } else {
                retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
                retTACList.addCode(new MethodReturn(returnStatement));
            }
            return retTACList;
        }

        @Override
        public ThreeAddressCodeList visit(Array array, SymbolTable symbolTable) {
            return null;
        }

        private void flattenMethodCallArguments(ThreeAddressCodeList threeAddressCodeList,
                                                List<MethodCallParameter> methodCallParameterList,
                                                SymbolTable symbolTable) {
            var paramNames = new ArrayList<AbstractName>();
            for (MethodCallParameter methodCallParameter : methodCallParameterList) {
                var paramTACList = methodCallParameter.accept(this, symbolTable);
                threeAddressCodeList.add(paramTACList);
                paramNames.add((paramTACList.place));
            }

            getPushParameterCode(threeAddressCodeList, paramNames, methodCallParameterList);
        }

        private void getPushParameterCode(ThreeAddressCodeList threeAddressCodeList,
                                          List<AbstractName> newParamNames,
                                          List<? extends AST> methodCallOrDefinitionArguments) {
            for (int i = newParamNames.size() - 1; i >= 0; i--) {
                final var pushParameter = new PushParameter(newParamNames.get(i), i, methodCallOrDefinitionArguments.get(i));
                threeAddressCodeList.addCode(pushParameter);
                pushParameter.setComment("#     index of parameter = " + i);
            }
        }


        private AssignableName resolveStoreLocation() {
            return Objects.requireNonNullElseGet(methodSetResultLocation, TemporaryName::generateTemporaryName);
        }

        private void resetStoreLocation() {
            methodSetResultLocation = null;
        }

        private void setStoreLocation(AssignableName place) {
            if (place != null && (!(place instanceof ArrayName)))
                methodSetResultLocation = place;
        }

        @Override
        public ThreeAddressCodeList visit(MethodCall methodCall, SymbolTable symbolTable) {
            final ThreeAddressCodeList threeAddressCodeList = ThreeAddressCodeList.empty();
            flattenMethodCallArguments(threeAddressCodeList, methodCall.methodCallParameterList, symbolTable);
            var place = resolveStoreLocation();
            resetStoreLocation();
            threeAddressCodeList.addCode(new MethodCallSetResult(methodCall, place, methodCall.getSourceCode()));
            threeAddressCodeList.place = place;
            return threeAddressCodeList;
        }


        @Override
        public ThreeAddressCodeList visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = ThreeAddressCodeList.empty();
            flattenMethodCallArguments(threeAddressCodeList, methodCallStatement.methodCall.methodCallParameterList, symbolTable);
            threeAddressCodeList.addCode(new MethodCallNoResult(methodCallStatement.methodCall, methodCallStatement.getSourceCode()));
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
            final ThreeAddressCodeList lhs = locationAssignExpr.location.accept(this, symbolTable);
            AssignableName locationVariable = (AssignableName) lhs.place;
            final ThreeAddressCodeList rhs = locationAssignExpr.assignExpr.accept(this, symbolTable);
            AbstractName valueVariable = rhs.place;
            lhs.add(rhs);
            lhs.addCode(new Assign(locationVariable, locationAssignExpr.getSourceCode(), valueVariable, locationAssignExpr, locationAssignExpr.assignExpr.getSourceCode()));
            return lhs;
        }

        @Override
        public ThreeAddressCodeList visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public ThreeAddressCodeList visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(new VariableName(methodDefinitionParameter.id.id));
        }

        @Override
        public ThreeAddressCodeList visit(Name name, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(new VariableName(name.id));
        }

        @Override
        public ThreeAddressCodeList visit(LocationVariable locationVariable, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(new VariableName(locationVariable.name.id));
        }

        @Override
        public ThreeAddressCodeList visit(Len len, SymbolTable symbolTable) {
            final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) symbolTable
                    .getDescriptorFromValidScopes(len.nameId.id)
                    .orElseThrow(() -> new IllegalStateException(len.nameId.id + " should be present"));
            return new ThreeAddressCodeList(new ConstantName(arrayDescriptor.size));
        }

        @Override
        public ThreeAddressCodeList visit(Increment increment, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public ThreeAddressCodeList visit(Decrement decrement, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public ThreeAddressCodeList visit(CharLiteral charLiteral, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(ConstantName.fromIntLiteral(charLiteral));
        }

        @Override
        public ThreeAddressCodeList visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
            StringLiteralStackAllocation label = literalStackAllocationHashMap.get(stringLiteral.literal);
            StringConstantName stringConstantName = new StringConstantName(label);
            return new ThreeAddressCodeList(stringConstantName);
        }

        @Override
        public ThreeAddressCodeList visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
            return compoundAssignOpExpr.expression.accept(this, curSymbolTable);
        }

        @Override
        public ThreeAddressCodeList visit(Initialization initialization, SymbolTable symbolTable) {
            ThreeAddressCodeList initIdThreeAddressList = initialization.initId.accept(this, symbolTable);
            setStoreLocation((AssignableName) initIdThreeAddressList.place);
            ThreeAddressCodeList initExpressionThreeAddressList = initialization.initExpression.accept(this, symbolTable);
            Assign copyInstruction = new Assign((AssignableName) initIdThreeAddressList.place, "=", initExpressionThreeAddressList.place, initialization, initialization.getSourceCode());

            ThreeAddressCodeList initializationThreeAddressCodeList = new ThreeAddressCodeList(initIdThreeAddressList.place);
            initializationThreeAddressCodeList.add(initIdThreeAddressList);
            initializationThreeAddressCodeList.add(initExpressionThreeAddressList);
            initializationThreeAddressCodeList.addCode(copyInstruction);
            return initializationThreeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(Assignment assignment, SymbolTable symbolTable) {
            ThreeAddressCodeList returnTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            ThreeAddressCodeList lhs = assignment.location.accept(this, symbolTable);
            ThreeAddressCodeList rhs;
            if (assignment.assignExpr.expression != null) {
                if (assignment.getOperator().equals("="))
                    setStoreLocation((AssignableName) lhs.place);
                rhs = assignment.assignExpr.expression.accept(this, symbolTable);
            } else {
                rhs = ThreeAddressCodeList.empty();
            }
            returnTACList.add(rhs);
            returnTACList.add(lhs);

            if (assignment.assignExpr instanceof AssignOpExpr) {
                returnTACList.addCode(new Assign((AssignableName) lhs.place, ((AssignOpExpr) assignment.assignExpr).assignOp.getSourceCode(), rhs.place, assignment, assignment.getSourceCode()));
            } else if (assignment.assignExpr instanceof CompoundAssignOpExpr) {
                returnTACList.addCode(new Assign((AssignableName) lhs.place, ((CompoundAssignOpExpr) assignment.assignExpr).compoundAssignOp.getSourceCode(), rhs.place, assignment, assignment.getSourceCode()));
            } else if (assignment.assignExpr instanceof Decrement || assignment.assignExpr instanceof Increment) {
                returnTACList.addCode(new Assign((AssignableName) lhs.place, assignment.assignExpr.getSourceCode(), lhs.place, assignment.assignExpr, assignment.getSourceCode()));
            } else {
                returnTACList.addCode(new Assign((AssignableName) lhs.place, DecafScanner.ASSIGN, rhs.place, assignment, assignment.getSourceCode()));
            }
            returnTACList.place = lhs.place;
            return returnTACList;
        }
    }

    final private static HashMap<String, StringLiteralStackAllocation> literalStackAllocationHashMap = new HashMap<>();

    ThreeAddressCodesConverter visitor;
    Label endLabelGlobal;
    Set<BasicBlock> visited = new HashSet<>();
    HashMap<BasicBlock, Label> blockToLabelHashMap = new HashMap<>();
    HashMap<BasicBlock, ThreeAddressCodeList> blockToCodeHashMap = new HashMap<>();
    static AssignableName methodSetResultLocation;
    public HashMap<String, SymbolTable> cfgSymbolTables;


    private MethodBegin convertMethodDefinition(MethodDefinition methodDefinition,
                                                BasicBlock methodStart,
                                                SymbolTable symbolTable) {

        TemporaryNameGenerator.reset();
        endLabelGlobal = new Label("exit_" + methodDefinition.methodName.id, methodStart);

        var threeAddressCodeList = ThreeAddressCodeList.empty();
        var methodBegin = new MethodBegin(methodDefinition);
        threeAddressCodeList.addCode(methodBegin);

        threeAddressCodeList.add(
                errors.stream()
                        .map(error -> new RuntimeException(error.getMessage(), -2, error))
                        .collect(Collectors.toList()));

        flattenMethodDefinitionArguments(threeAddressCodeList, methodDefinition.methodDefinitionParameterList);

        final var firstBasicBlockTacList = methodStart.accept(this, symbolTable);
        firstBasicBlockTacList.prependAll(threeAddressCodeList);

        firstBasicBlockTacList.setNext(ThreeAddressCodeList.of(endLabelGlobal));

        firstBasicBlockTacList.setNext(ThreeAddressCodeList.of(new MethodEnd(methodDefinition)));
        methodStart.threeAddressCodeList = firstBasicBlockTacList;
        methodBegin.unoptimized = firstBasicBlockTacList.flatten();
        methodBegin.entryBlock = methodStart;
        return methodBegin;
    }


    public void flattenMethodDefinitionArguments(ThreeAddressCodeList threeAddressCodeList,
                                                 List<MethodDefinitionParameter> methodDefinitionParameterList) {

        for (int i = 0; i < methodDefinitionParameterList.size(); i++) {
            MethodDefinitionParameter parameter = methodDefinitionParameterList.get(i);
            threeAddressCodeList.addCode(new PopParameter(
                    new VariableName(parameter.id.id),
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

    private ThreeAddressCodeList fillOutGlobals(List<FieldDeclaration> fieldDeclarationList) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList) {
            for (Name name : fieldDeclaration.names) {
                threeAddressCodeList.addCode(new GlobalAllocation(name, "# " + name.getSourceCode(), new VariableName(name.id), fieldDeclaration.builtinType.getFieldSize(), fieldDeclaration.builtinType));
            }
            for (Array array : fieldDeclaration.arrays) {
                long size = (fieldDeclaration.builtinType.getFieldSize() * array.size.convertToLong());
                threeAddressCodeList.addCode(new GlobalAllocation(array, "# " + array.getSourceCode(),
                        new ArrayName(array.id.id,
                                size), size, fieldDeclaration.builtinType));
            }
        }
        getGlobals(threeAddressCodeList);
        return threeAddressCodeList;
    }


    private ThreeAddressCodeList initProgram(Program program) {
        ThreeAddressCodeList threeAddressCodeList = fillOutGlobals(program.fieldDeclarationList);
        Set<String> stringLiteralList = findAllStringLiterals(program);
        for (String stringLiteral : stringLiteralList) {
            final StringLiteralStackAllocation literalStackAllocation = new StringLiteralStackAllocation(stringLiteral);
            threeAddressCodeList.addCode(literalStackAllocation);
            literalStackAllocationHashMap.put(stringLiteral, literalStackAllocation);
        }
        return threeAddressCodeList;
    }

    private void getGlobals(ThreeAddressCodeList threeAddressCodeList) {
        globalNames = new HashSet<>();
        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            for (AbstractName name : threeAddressCode.getNames()) {
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

    public Pair<ThreeAddressCodeList, List<MethodBegin>> fill(iCFGVisitor visitor,
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

    public ThreeAddressCodesListConverter(CFGGenerator cfgGenerator) {
        this.errors = cfgGenerator.errors;
        this.visitor = new ThreeAddressCodesConverter();
        SymbolTableFlattener symbolTableFlattener = new SymbolTableFlattener(cfgGenerator.globalDescriptor);
        this.cfgSymbolTables = symbolTableFlattener.createCFGSymbolTables();
    }

    @Override
    public ThreeAddressCodeList visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable) {
        visited.add(basicBlockBranchLess);
        ThreeAddressCodeList universalThreeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        for (AST line : basicBlockBranchLess.lines) {
            universalThreeAddressCodeList.add(line.accept(visitor, symbolTable));
        }
        blockToCodeHashMap.put(basicBlockBranchLess, universalThreeAddressCodeList);
        if (!(basicBlockBranchLess instanceof NOP) && !(basicBlockBranchLess.autoChild instanceof NOP)) {
            if (visited.contains(basicBlockBranchLess.autoChild)) {
                assert blockToLabelHashMap.containsKey(basicBlockBranchLess.autoChild);
                universalThreeAddressCodeList.setNext(ThreeAddressCodeList.of(new UnconditionalJump(blockToLabelHashMap.get(basicBlockBranchLess.autoChild))));
            } else {
                universalThreeAddressCodeList.setNext(basicBlockBranchLess.autoChild.accept(this, symbolTable));
            }
        } else {
            universalThreeAddressCodeList.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabelGlobal)));
        }
        basicBlockBranchLess.threeAddressCodeList = universalThreeAddressCodeList;
        return universalThreeAddressCodeList;
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

    private ThreeAddressCodeList getConditionTACList(Expression condition, SymbolTable symbolTable) {
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

    private ThreeAddressCodeList getConditionalChildBlock(
            BasicBlock child,
            Label conditionalLabel,
            SymbolTable symbolTable) {

        ThreeAddressCodeList codeList;
        ThreeAddressCodeList trueBlock;

        if (!(child instanceof NOP)) {
            if (visited.contains(child)) {
                codeList = blockToCodeHashMap.get(child);
                Label label;
                if (codeList.get(0) instanceof Label)
                    label = (Label) codeList.get(0);
                else {
                    label = getLabel(child, conditionalLabel);
                    codeList.prepend(label);
                }
                trueBlock = ThreeAddressCodeList.of(new UnconditionalJump(label));
            } else
                trueBlock = child.accept(this, symbolTable);
        } else {
            trueBlock = ThreeAddressCodeList.of(new UnconditionalJump(endLabelGlobal));
        }
        return trueBlock;
    }

    @Override
    public ThreeAddressCodeList visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable) {
        visited.add(basicBlockWithBranch);

        final Expression condition = basicBlockWithBranch.condition;
        final ThreeAddressCodeList testConditionThreeAddressList = getConditionTACList(condition, symbolTable);
        final Label conditionLabel = getLabel(basicBlockWithBranch, null);

        final ThreeAddressCodeList conditionLabelTACList = ThreeAddressCodeList.of(conditionLabel);
        conditionLabelTACList.add(testConditionThreeAddressList);

        blockToCodeHashMap.put(basicBlockWithBranch, conditionLabelTACList);

        final ThreeAddressCodeList trueBlock = getConditionalChildBlock(basicBlockWithBranch.trueChild, conditionLabel, symbolTable);
        final ThreeAddressCodeList falseBlock = getConditionalChildBlock(basicBlockWithBranch.falseChild, conditionLabel, symbolTable);

        final Label falseLabel = getLabel(basicBlockWithBranch.falseChild, conditionLabel);
        final Label endLabel = new Label(conditionLabel.label + "end", null);

        ConditionalJump jumpIfFalse =
                new ConditionalJump(condition,
                        testConditionThreeAddressList.place,
                        falseLabel, "if !(" + basicBlockWithBranch.condition.getSourceCode() + ")");
        conditionLabelTACList.addCode(jumpIfFalse);
        if (!(trueBlock.last() instanceof UnconditionalJump))
            trueBlock.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabel)));

        // no need for consecutive similar labels
        if (!falseBlock.isEmpty() && falseBlock.first() instanceof UnconditionalJump) {
            UnconditionalJump unconditionalJump = (UnconditionalJump) falseBlock.first();
            if (!unconditionalJump.goToLabel.label.equals(falseLabel.label))
                falseBlock.prepend(falseLabel);
        } else if (!falseBlock.first()
                .equals(falseLabel))
            falseBlock.prepend(falseLabel);
        if (!(falseBlock.last() instanceof UnconditionalJump) && !(trueBlock.last() instanceof UnconditionalJump))
            falseBlock.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabel)));
        if (falseBlock.flattenedSize() == 1 && falseBlock.first() instanceof UnconditionalJump) {
            conditionLabelTACList.setNext(trueBlock);
            basicBlockWithBranch.threeAddressCodeList = conditionLabelTACList;
            return conditionLabelTACList;
        }
        conditionLabelTACList
                .setNext(trueBlock)
                .setNext(falseBlock);
        basicBlockWithBranch.threeAddressCodeList = conditionLabelTACList;
        return conditionLabelTACList;
    }

    @Override
    public ThreeAddressCodeList visit(NOP nop, SymbolTable symbolTable) {
        nop.threeAddressCodeList = ThreeAddressCodeList.empty();
        return nop.threeAddressCodeList;
    }
}

package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.codegen.*;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;


import java.util.*;
import java.util.function.BiFunction;

public class ThreeAddressListFillerVisitor implements CFGVisitor<ThreeAddressCodeList> {
    public static class ThreeAddressCodesVisitor implements Visitor<ThreeAddressCodeList> {
        HashMap<String, String> temporaryToStringLiteral;

        public ThreeAddressCodesVisitor() {
            this.temporaryToStringLiteral = new HashMap<>();
        }

        @Override
        public ThreeAddressCodeList visit(IntLiteral intLiteral, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(intLiteral.literal, temporaryVariable, intLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(booleanLiteral.literal, temporaryVariable, booleanLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(decimalLiteral.literal, temporaryVariable, decimalLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(hexLiteral.convertToLong().toString(), temporaryVariable, hexLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        }

        public List<PushParameter> flattenMethodDefinitionArguments(ThreeAddressCodeList threeAddressCodeList,
                                                                    List<MethodDefinitionParameter> methodDefinitionParameterList,
                                                                    SymbolTable symbolTable) {
            List<String> newParamNames = new ArrayList<>();
            for (MethodDefinitionParameter methodDefinitionParameter : methodDefinitionParameterList) {
                threeAddressCodeList.add(methodDefinitionParameter.accept(this, symbolTable));
                newParamNames.add(((CopyInstruction) threeAddressCodeList.getFromLast(0)).dst);
            }
            return getPushParameterCode(threeAddressCodeList, newParamNames, methodDefinitionParameterList);
        }

        private List<PushParameter> getPushParameterCode(ThreeAddressCodeList threeAddressCodeList,
                                                         List<String> newParamNames,
                                                         List<? extends AST> methodCallOrDefinitionArguments) {
            List<PushParameter> pushParams = new ArrayList<>();
            for (int i = 0; i < newParamNames.size(); i++) {
                final PushParameter pushParameter = new PushParameter(newParamNames.get(i), methodCallOrDefinitionArguments.get(i));
                threeAddressCodeList.addCode(pushParameter);
                pushParams.add(pushParameter);
            }
            return pushParams;
        }

        private List<PushParameter> flattenMethodCallArguments(ThreeAddressCodeList threeAddressCodeList,
                                                               List<MethodCallParameter> methodCallParameterList,
                                                               SymbolTable symbolTable) {
            List<String> newParamNames = new ArrayList<>();
            for (MethodCallParameter methodCallParameter : methodCallParameterList) {
                threeAddressCodeList.add(methodCallParameter.accept(this, symbolTable));
                newParamNames.add(((AbstractAssignment) threeAddressCodeList.getFromLast(0)).dst);
            }

            return getPushParameterCode(threeAddressCodeList, newParamNames, methodCallParameterList);
        }

        @Override
        public ThreeAddressCodeList visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
            throw new IllegalStateException("A method definition is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
            return null;
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
            final ThreeAddressCodeList operandTACList = unaryOpExpression.operand.accept(this, symbolTable);
            final ThreeAddressCodeList retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            retTACList.add(operandTACList);
            String result = TemporaryNameGenerator.getNextTemporaryVariable();
            retTACList.addCode(new OneOperandAssignCode(unaryOpExpression, result, operandTACList.place, unaryOpExpression.op.getSourceCode()));
            retTACList.place = result;
            return retTACList;
        }

        @Override
        public ThreeAddressCodeList visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
            final ThreeAddressCodeList leftTACList = binaryOpExpression.lhs.accept(this, symbolTable);
            final ThreeAddressCodeList rightTACList = binaryOpExpression.rhs.accept(this, symbolTable);

            final ThreeAddressCodeList binOpExpressionTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            binOpExpressionTACList.add(leftTACList);
            binOpExpressionTACList.add(rightTACList);
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            binOpExpressionTACList.addCode(
                    new TwoOperandAssignCode(
                            binaryOpExpression,
                            temporaryVariable,
                            leftTACList.place,
                            binaryOpExpression.op.getSourceCode(),
                            rightTACList.place,
                            binaryOpExpression.getSourceCode()));
            binOpExpressionTACList.place = temporaryVariable;
            return binOpExpressionTACList;
        }

        @Override
        public ThreeAddressCodeList visit(Block block, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
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

        @Override
        public ThreeAddressCodeList visit(LocationArray locationArray, SymbolTable symbolTable) {
            ThreeAddressCodeList locationThreeAddressCodeList = locationArray.expression.accept(this, symbolTable);
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            threeAddressCodeList.add(locationThreeAddressCodeList);
            String widthOfField = TemporaryNameGenerator.getNextTemporaryVariable();

            Optional<Descriptor> descriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
            if (descriptorFromValidScopes.isEmpty()) {
                throw new IllegalStateException("expected to find array " + locationArray.name.id + " in scope");
            } else {
                ArrayDescriptor arrayDescriptor = (ArrayDescriptor) descriptorFromValidScopes.get();
                threeAddressCodeList.addCode(new CopyInstruction(String.valueOf(arrayDescriptor.type.getFieldSize()), widthOfField, locationArray));
                String offsetResult = TemporaryNameGenerator.getNextTemporaryVariable();
                threeAddressCodeList.addCode(new TwoOperandAssignCode(null, offsetResult, locationThreeAddressCodeList.place, DecafScanner.MULTIPLY, widthOfField, "offset"));
                String locationResult = TemporaryNameGenerator.getNextTemporaryVariable();
                threeAddressCodeList.addCode(new TwoOperandAssignCode(null, locationResult, locationArray.name.id, DecafScanner.PLUS, offsetResult, "array location"));
                threeAddressCodeList.place = locationResult;
                return threeAddressCodeList;
            }
        }

        @Override
        public ThreeAddressCodeList visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
            ThreeAddressCodeList expressionTACList = expressionParameter.expression.accept(this, symbolTable);
            ThreeAddressCodeList expressionParameterTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            expressionParameterTACList.add(expressionTACList);
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            expressionParameterTACList.addCode(new CopyInstruction(expressionTACList.place, temporaryVariable, expressionParameter));
            expressionParameterTACList.place = temporaryVariable;
            return expressionParameterTACList;
        }

        @Override
        public ThreeAddressCodeList visit(If ifStatement, SymbolTable symbolTable) {
            throw new IllegalStateException("An if statement is illegal");
        }

        @Override
        public ThreeAddressCodeList visit(Return returnStatement, SymbolTable symbolTable) {
            ThreeAddressCodeList retTACList;
            if (returnStatement.retExpression != null) {
                ThreeAddressCodeList retExpressionTACList = returnStatement.retExpression.accept(this, symbolTable);
                retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
                retTACList.add(retExpressionTACList);
                retTACList.place = retExpressionTACList.place;
                retTACList.addCode(new MethodReturn(returnStatement, retTACList.place));
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

        @Override
        public ThreeAddressCodeList visit(MethodCall methodCall, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            List<PushParameter> pushParameterList = flattenMethodCallArguments(threeAddressCodeList, methodCall.methodCallParameterList, symbolTable);
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            threeAddressCodeList.addCode(new edu.mit.compilers.codegen.MethodCall(methodCall, temporaryVariable, methodCall.getSourceCode()));
            for (int i = pushParameterList.size() - 1; i >= 0; i--)
                threeAddressCodeList.addCode(new PopParameter(pushParameterList.get(i).which, methodCall.methodCallParameterList.get(i)));
            threeAddressCodeList.place = temporaryVariable;
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            List<PushParameter> pushParameterList = flattenMethodCallArguments(threeAddressCodeList, methodCallStatement.methodCall.methodCallParameterList, symbolTable);
            threeAddressCodeList.addCode(new edu.mit.compilers.codegen.MethodCall(methodCallStatement.methodCall, methodCallStatement.getSourceCode()));
            for (int i = pushParameterList.size() - 1; i >= 0; i--)
                threeAddressCodeList.addCode(new PopParameter(pushParameterList.get(i).which, methodCallStatement.methodCall.methodCallParameterList.get(i)));
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
            ThreeAddressCodeList lhs = locationAssignExpr.location.accept(this, symbolTable);
            String locationVariable = lhs.place;
            ThreeAddressCodeList rhs = locationAssignExpr.assignExpr.accept(this, symbolTable);
            String valueVariable = rhs.place;
            lhs.add(rhs);
            lhs.addCode(new CopyInstruction(valueVariable, locationVariable, locationAssignExpr, locationAssignExpr.getSourceCode()));
            return lhs;
        }

        @Override
        public ThreeAddressCodeList visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
            throw new IllegalStateException("not allowed");
        }

        @Override
        public ThreeAddressCodeList visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(temporaryVariable,
                    Collections.singletonList(new CopyInstruction(methodDefinitionParameter.id.id, temporaryVariable, methodDefinitionParameter)));
        }

        @Override
        public ThreeAddressCodeList visit(Name name, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            return new ThreeAddressCodeList(temporaryVariable, Collections.singletonList(new CopyInstruction(name.id, temporaryVariable, name)));
        }

        @Override
        public ThreeAddressCodeList visit(LocationVariable locationVariable, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(locationVariable.name.id);
        }

        @Override
        public ThreeAddressCodeList visit(Len len, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            String temporaryNumberOfElemsVariable = TemporaryNameGenerator.getNextTemporaryVariable();
            Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(len.nameId.id);
            if (optionalDescriptor.isEmpty())
                throw new IllegalStateException(len.nameId.id + " should be present");
            else {
                ArrayDescriptor arrayDescriptor = (ArrayDescriptor) optionalDescriptor.get();
                threeAddressCodeList.addCode(new CopyInstruction(arrayDescriptor.size.toString(), temporaryNumberOfElemsVariable, len));
                threeAddressCodeList.place = temporaryNumberOfElemsVariable;
                return threeAddressCodeList;
            }
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
            throw new IllegalStateException("not allowed");
        }

        @Override
        public ThreeAddressCodeList visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
            String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();

            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(
                            new CopyInstruction(literalStackAllocationHashMap.get(stringLiteral.literal).label, temporaryVariable, stringLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
            return compoundAssignOpExpr.expression.accept(this, curSymbolTable);
        }

        @Override
        public ThreeAddressCodeList visit(Initialization initialization, SymbolTable symbolTable) {
            ThreeAddressCodeList initIdThreeAddressList = initialization.initId.accept(this, symbolTable);
            ThreeAddressCodeList initExpressionThreeAddressList = initialization.initExpression.accept(this, symbolTable);
            CopyInstruction copyInstruction = new CopyInstruction(initExpressionThreeAddressList.place, initIdThreeAddressList.place, initialization);

            ThreeAddressCodeList initializationThreeAddressCodeList = new ThreeAddressCodeList(initIdThreeAddressList.place);
            initializationThreeAddressCodeList.add(initIdThreeAddressList);
            initializationThreeAddressCodeList.add(initExpressionThreeAddressList);
            initializationThreeAddressCodeList.addCode(copyInstruction);
            return initializationThreeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(Assignment assignment, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            if (assignment.assignExpr instanceof AssignOpExpr) {
                AssignOpExpr assignOpExpr = (AssignOpExpr) assignment.assignExpr;
                if (!assignOpExpr.assignOp.op.equals(DecafScanner.ASSIGN)) {
                    // simplify
                    return convertToBinaryExpression(
                            assignment.location,
                            mapCompoundAssignOperator(assignOpExpr.assignOp),
                            assignOpExpr.expression,
                            symbolTable,
                            assignment.location.name.id,
                            assignment
                    );
                }
            } else if (assignment.assignExpr instanceof Decrement) {
                Decrement decrement = (Decrement) assignment.assignExpr;
                return convertToBinaryExpression(
                        assignment.location,
                        new ArithmeticOperator(decrement.tokenPosition, DecafScanner.MINUS),
                        new DecimalLiteral(decrement.tokenPosition, "1"),
                        symbolTable,
                        assignment.location.name.id,
                        assignment
                );
            } else if (assignment.assignExpr instanceof Increment) {
                    Increment decrement = (Increment) assignment.assignExpr;
                    return convertToBinaryExpression(
                            assignment.location,
                            new ArithmeticOperator(decrement.tokenPosition, DecafScanner.PLUS),
                            new DecimalLiteral(decrement.tokenPosition, "1"),
                            symbolTable,
                            assignment.location.name.id,
                            assignment
                    );
                }

            ThreeAddressCodeList rhs = assignment.assignExpr.expression.accept(this, symbolTable);
            ThreeAddressCodeList lhs = assignment.location.accept(this, symbolTable);
            threeAddressCodeList.add(rhs);
            threeAddressCodeList.add(lhs);
            threeAddressCodeList.addCode(new CopyInstruction(rhs.place, lhs.place, assignment));
            threeAddressCodeList.place = lhs.place;
            return threeAddressCodeList;
        }

        private BinOperator mapCompoundAssignOperator(AST augmentedAssignOperator) {
            String op;
            TokenPosition tokenPosition;
            if (augmentedAssignOperator instanceof CompoundAssignOperator) {
                op = ((CompoundAssignOperator) augmentedAssignOperator).op;
                tokenPosition = ((CompoundAssignOperator) augmentedAssignOperator).tokenPosition;
            } else if (augmentedAssignOperator instanceof AssignOperator) {
                op = ((AssignOperator) augmentedAssignOperator).op;
                tokenPosition = ((AssignOperator) augmentedAssignOperator).tokenPosition;
            } else
                throw new IllegalStateException(augmentedAssignOperator.getClass().getSimpleName() + " not cannot be map");
            BinOperator binOperator;
            switch (op) {
                case DecafScanner.ADD_ASSIGN:
                    binOperator = new ArithmeticOperator(tokenPosition, DecafScanner.PLUS);
                    break;
                case DecafScanner.MINUS_ASSIGN:
                    binOperator = new ArithmeticOperator(tokenPosition, DecafScanner.MINUS);
                    break;
                case DecafScanner.MULTIPLY_ASSIGN:
                    binOperator = new ArithmeticOperator(tokenPosition, DecafScanner.MULTIPLY);
                    break;
                default:
                    throw new IllegalStateException(op + " not recognized");
            }
            return binOperator;
        }


        private ThreeAddressCodeList convertToBinaryExpression(
                Expression lhs,
                BinOperator binOperator,
                Expression rhs,
                SymbolTable symbolTable,
                String locationId,
                AST source
        ) {
            ThreeAddressCodeList updateExprTAC;
            BinaryOpExpression binaryOpExpression = new BinaryOpExpression(lhs, binOperator, rhs);
            updateExprTAC = binaryOpExpression.accept(this, symbolTable);
            CopyInstruction copyInstruction = new CopyInstruction(updateExprTAC.place, locationId, source);
            updateExprTAC.addCode(copyInstruction);
            return updateExprTAC;
        }

        @Override
        public ThreeAddressCodeList visit(Update update, SymbolTable symbolTable) {
            ThreeAddressCodeList updateExprTAC;
            if (update.updateAssignExpr instanceof CompoundAssignOpExpr) {
                CompoundAssignOpExpr compoundAssignOpExpr = (CompoundAssignOpExpr) update.updateAssignExpr;
                return convertToBinaryExpression(
                        update.updateLocation,
                        mapCompoundAssignOperator(compoundAssignOpExpr.compoundAssignOp),
                        compoundAssignOpExpr.expression,
                        symbolTable,
                        update.updateLocation.name.id,
                        update
                );
            } else if (update.updateAssignExpr instanceof Decrement) {
                Decrement decrement = (Decrement) update.updateAssignExpr;
                return convertToBinaryExpression(
                        update.updateLocation,
                        new ArithmeticOperator(decrement.tokenPosition, DecafScanner.MINUS),
                        new DecimalLiteral(decrement.tokenPosition, "1"),
                        symbolTable,
                        update.updateLocation.name.id,
                        update
                );
            } else if (update.updateAssignExpr instanceof Increment) {
                Increment decrement = (Increment) update.updateAssignExpr;
                return convertToBinaryExpression(
                        update.updateLocation,
                        new ArithmeticOperator(decrement.tokenPosition, DecafScanner.PLUS),
                        new DecimalLiteral(decrement.tokenPosition, "1"),
                        symbolTable,
                        update.updateLocation.name.id,
                        update
                );
            } else {
                ThreeAddressCodeList updateLocationTAC = update.updateLocation.accept(this, symbolTable);
                updateExprTAC = update.updateAssignExpr.accept(this, symbolTable);
                CopyInstruction copyInstruction = new CopyInstruction(updateExprTAC.place, updateLocationTAC.place, update);

                ThreeAddressCodeList tac = new ThreeAddressCodeList(updateLocationTAC.place);
                tac.add(updateLocationTAC);
                tac.add(updateExprTAC);
                tac.addCode(copyInstruction);
                return tac;
            }
        }
    }


    ThreeAddressCodesVisitor visitor;
    Set<CFGBlock> visited = new HashSet<>();
    HashMap<CFGBlock, Label> blockToLabelHashMap = new HashMap<>();
    public HashMap<String, SymbolTable> cfgSymbolTables;
    final private static HashMap<String, StringLiteralStackAllocation> literalStackAllocationHashMap = new HashMap<>();

    public ThreeAddressCodeList dispatch(CFGBlock cfgBlock, SymbolTable symbolTable) {
        if (cfgBlock instanceof CFGNonConditional)
            return visit((CFGNonConditional) cfgBlock, symbolTable);
        else
            return visit((CFGConditional) cfgBlock, symbolTable);
    }


    private ThreeAddressCodeList convertMethodDefinition(MethodDefinition methodDefinition,
                                                         CFGBlock methodStart,
                                                         SymbolTable symbolTable) {
        TemporaryNameGenerator.reset();

        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        threeAddressCodeList.addCode(new MethodBegin(methodDefinition));

        List<PushParameter> pushParams = visitor.flattenMethodDefinitionArguments(threeAddressCodeList, methodDefinition.methodDefinitionParameterList, symbolTable);

        threeAddressCodeList.add(dispatch(methodStart, symbolTable));

        for (int i = pushParams.size() - 1; i >= 0; i--)
            threeAddressCodeList.addCode(new PopParameter(pushParams.get(i).which, methodDefinition.methodDefinitionParameterList.get(i)));
        threeAddressCodeList.addCode(new MethodEnd(methodDefinition));
        return threeAddressCodeList;
    }

    private MethodDefinition getMethodDefinitionFromProgram(String name, Program program) {
        for (MethodDefinition methodDefinition : program.methodDefinitionList) {
            if (methodDefinition.methodName.id.equals(name)) {
                return methodDefinition;
            }
        }
        throw new IllegalStateException("expected to find method " + name);
    }

    private ThreeAddressCodeList initProgram(Program program, CFGNonConditional initialGlobalBlock) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        threeAddressCodeList.addCode(new ProgramBegin(initialGlobalBlock));
        Set<String> stringLiteralList = findAllStringLiterals(program);
        for (String stringLiteral : stringLiteralList) {
            final StringLiteralStackAllocation literalStackAllocation = new StringLiteralStackAllocation(stringLiteral);
            threeAddressCodeList.addCode(literalStackAllocation);
            literalStackAllocationHashMap.put(stringLiteral, literalStackAllocation);
        }
        return threeAddressCodeList;
    }

    private Set<String> findAllStringLiterals(Program program) {
        Set<String> literalList = new HashSet<>();
        Stack<AST> toExplore = new Stack<>();
        toExplore.addAll(program.methodDefinitionList);
        while (!toExplore.isEmpty()) {
            AST node = toExplore.pop();
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

    public ThreeAddressCodeList fill(iCFGVisitor visitor,
                                     Program program) {
        ThreeAddressCodeList threeAddressCodeList = initProgram(program, visitor.initialGlobalBlock);
        threeAddressCodeList.add(convertMethodDefinition(getMethodDefinitionFromProgram("main", program), visitor.methodCFGBlocks.get("main"), cfgSymbolTables.get("main")));
        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (!k.equals("main")) {
                threeAddressCodeList.add(convertMethodDefinition(getMethodDefinitionFromProgram(k, program), v, cfgSymbolTables.get(k)));
            }
        });
        return threeAddressCodeList;
    }

    public ThreeAddressListFillerVisitor(GlobalDescriptor globalDescriptor) {
        this.visitor = new ThreeAddressCodesVisitor();
        CFGSymbolTableConverter cfgSymbolTableConverter = new CFGSymbolTableConverter(globalDescriptor);
        this.cfgSymbolTables = cfgSymbolTableConverter.createCFGSymbolTables();
    }

    @Override
    public ThreeAddressCodeList visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable) {
        visited.add(cfgNonConditional);
        ThreeAddressCodeList universalThreeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        for (CFGLine line : cfgNonConditional.lines) {
            universalThreeAddressCodeList.add(line.ast.accept(visitor, symbolTable));
        }
        if (cfgNonConditional.autoChild != null) {
            if (visited.contains(cfgNonConditional.autoChild)) {
                assert blockToLabelHashMap.containsKey(cfgNonConditional.autoChild);
                universalThreeAddressCodeList.addCode(new UnconditionalJump(blockToLabelHashMap.get(cfgNonConditional.autoChild)));
            } else {
                universalThreeAddressCodeList.add(cfgNonConditional.autoChild.accept(this, symbolTable));
            }
        }
        return universalThreeAddressCodeList;
    }

    private Label getLabel(CFGBlock cfgBlock, Boolean isConditional, Label from) {
        BiFunction<CFGBlock, Label, Label> function = (cfgBlock1, label) -> {
            if (label == null) {
                return new Label(TemporaryNameGenerator.getNextLabel(), cfgBlock1);
            } else if (isConditional) {
                return label;
            } else {
                label.aliasLabels.add(from.label + "_False");
            }
            return label;
        };
        return blockToLabelHashMap.compute(cfgBlock, function);
    }

    @Override
    public ThreeAddressCodeList visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        visited.add(cfgConditional);
        Expression condition = (Expression) (cfgConditional.condition).ast;
        ThreeAddressCodeList universalThreeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        ThreeAddressCodeList testConditionThreeAddressList;

        if (condition instanceof BinaryOpExpression)
            testConditionThreeAddressList = visitor.visit((BinaryOpExpression) condition, symbolTable);
        else if (condition instanceof UnaryOpExpression)
            testConditionThreeAddressList = visitor.visit((UnaryOpExpression) condition, symbolTable);
        else if (condition instanceof MethodCall)
            testConditionThreeAddressList = visitor.visit((MethodCall) condition, symbolTable);
        else if (condition instanceof LocationVariable)
            testConditionThreeAddressList = visitor.visit((LocationVariable) condition, symbolTable);
        else if (condition instanceof ParenthesizedExpression)
            testConditionThreeAddressList = visitor.visit((ParenthesizedExpression) condition, symbolTable);
        else throw new IllegalStateException("an expression of type " + condition + " is not allowed");

        final Label conditionalLabel = getLabel(cfgConditional, true, null);
        final Label falseLabel = getLabel(cfgConditional.falseChild, false, conditionalLabel);

        universalThreeAddressCodeList.addCode(conditionalLabel);
        universalThreeAddressCodeList.add(testConditionThreeAddressList);

        ThreeAddressCodeList falseBlock = null, trueBlock = null;
        if (cfgConditional.trueChild != null && !visited.contains(cfgConditional.trueChild))
            trueBlock = cfgConditional.trueChild.accept(this, symbolTable);
        if (cfgConditional.falseChild != null && !visited.contains(cfgConditional.falseChild))
            falseBlock = cfgConditional.falseChild.accept(this, symbolTable);

        Label endLabel = new Label(conditionalLabel.label + "_End", cfgConditional);

        universalThreeAddressCodeList.addCode(
                new JumpIfFalse(cfgConditional.condition.ast, testConditionThreeAddressList.place, falseLabel, "if !(" + cfgConditional.condition.ast.getSourceCode() + ")"));
        boolean endLabelAdded = false;

        if (trueBlock != null) {
            universalThreeAddressCodeList.add(trueBlock);
            if (falseBlock != null) {
                universalThreeAddressCodeList.addCode(new UnconditionalJump(endLabel));
                endLabelAdded = true;
            }
        }
        if (falseBlock != null) {
            // no need for consecutive similar labels
            if (!falseBlock.isEmpty() && falseBlock.get(0) instanceof Label) {
                String label = ((Label) falseBlock.get(0)).label;
                if (!label.equals(falseLabel.label))
                    universalThreeAddressCodeList.addCode(falseLabel);
            } else {
                universalThreeAddressCodeList.addCode(falseLabel);
            }
            universalThreeAddressCodeList.add(falseBlock);
        }
        if (endLabelAdded)
            universalThreeAddressCodeList.addCode(endLabel);
        return universalThreeAddressCodeList;
    }

    @Override
    public ThreeAddressCodeList visit(NOP nop, SymbolTable symbolTable) {
        throw new IllegalStateException("There should be no NOPs at this point, call NopVisitor to deal with it");
    }
}

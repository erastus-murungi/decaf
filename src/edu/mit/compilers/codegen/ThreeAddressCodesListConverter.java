package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.codegen.codes.*;
import edu.mit.compilers.codegen.names.*;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;


import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class ThreeAddressCodesListConverter implements CFGVisitor<ThreeAddressCodeList> {
    private Set<String> globals;

    private static class ThreeAddressCodesConverter implements Visitor<ThreeAddressCodeList> {
        HashMap<String, String> temporaryToStringLiteral;

        public ThreeAddressCodesConverter() {
            this.temporaryToStringLiteral = new HashMap<>();
        }

        @Override
        public ThreeAddressCodeList visit(IntLiteral intLiteral, SymbolTable symbolTable) {
            return new ThreeAddressCodeList(ConstantName.fromIntLiteral(intLiteral));
        }

        @Override
        public ThreeAddressCodeList visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(BuiltinType.Bool.getFieldSize());
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(ConstantName.fromBooleanLiteral(booleanLiteral), temporaryVariable, booleanLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(BuiltinType.Int.getFieldSize());
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(ConstantName.fromIntLiteral(decimalLiteral), temporaryVariable, decimalLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(BuiltinType.Int.getFieldSize());
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(ConstantName.fromIntLiteral(hexLiteral), temporaryVariable, hexLiteral)));
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
            final ThreeAddressCodeList operandTACList = unaryOpExpression.operand.accept(this, symbolTable);
            final ThreeAddressCodeList retTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            retTACList.add(operandTACList);
            TemporaryName result = TemporaryName.generateTemporaryName(unaryOpExpression.builtinType.getFieldSize());
            retTACList.addCode(new OneOperandAssign(unaryOpExpression, result, operandTACList.place, unaryOpExpression.op.getSourceCode()));
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
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(binaryOpExpression.builtinType.getFieldSize());
            binOpExpressionTACList.addCode(
                    new TwoOperandAssign(
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


        private void addArrayAccessBoundsCheck(
                ThreeAddressCodeList threeAddressCodeList,
                AbstractName arrayIndex,
                ConstantName arraySize,
                ArrayAccess arrayAccess
        ) {
            final String boundsIndex = TemporaryNameGenerator.getNextBoundsCheckLabel();
            Label boundsBad = new Label("LTZero" + boundsIndex, null);
            Label boundsGood = new Label("LTEArraySize" + boundsIndex, null);
            threeAddressCodeList.addCode(new ArrayBoundsCheck(null, arrayAccess, null, arrayIndex, arraySize, boundsBad, boundsGood));
        }

        @Override
        public ThreeAddressCodeList visit(LocationArray locationArray, SymbolTable symbolTable) {
            ThreeAddressCodeList locationThreeAddressCodeList = locationArray.expression.accept(this, symbolTable);
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            threeAddressCodeList.add(locationThreeAddressCodeList);

            Optional<Descriptor> descriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
            if (descriptorFromValidScopes.isEmpty()) {
                throw new IllegalStateException("expected to find array " + locationArray.name.id + " in scope");
            } else {
                ArrayDescriptor arrayDescriptor = (ArrayDescriptor) descriptorFromValidScopes.get();
                ArrayAccess arrayAccess = new ArrayAccess(locationArray, locationArray.getSourceCode(), new ArrayName(locationArray.name.id, arrayDescriptor.size * Utils.WORD_SIZE), new ConstantName(arrayDescriptor.size), locationThreeAddressCodeList.place);
                addArrayAccessBoundsCheck(threeAddressCodeList, arrayAccess.accessIndex, arrayAccess.arrayLength, arrayAccess);
                threeAddressCodeList.addCode(arrayAccess);
                threeAddressCodeList.place = arrayAccess.arrayName;
                return threeAddressCodeList;
            }
        }

        @Override
        public ThreeAddressCodeList visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
            ThreeAddressCodeList expressionTACList = expressionParameter.expression.accept(this, symbolTable);
            ThreeAddressCodeList expressionParameterTACList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            expressionParameterTACList.add(expressionTACList);
            if (expressionParameter.expression instanceof LocationVariable) {
                // no need for temporaries
                expressionParameterTACList.place = new VariableName(((Location) expressionParameter.expression).name.id);
            } else if (expressionParameter.expression instanceof IntLiteral) {
                expressionParameterTACList.place = new ConstantName(((IntLiteral) expressionParameter.expression).convertToLong());
            } else {
                TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(expressionParameter.expression.builtinType.getFieldSize());
                expressionParameterTACList.addCode(new CopyInstruction(expressionTACList.place, temporaryVariable, expressionParameter));
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
            ThreeAddressCodeList retTACList;
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
            List<AbstractName> newParamNames = new ArrayList<>();
            for (MethodCallParameter methodCallParameter : methodCallParameterList) {
                ThreeAddressCodeList paramTACList = methodCallParameter.accept(this, symbolTable);
                threeAddressCodeList.add(paramTACList);
                newParamNames.add((paramTACList.place));
            }

            getPushParameterCode(threeAddressCodeList, newParamNames, methodCallParameterList);
        }

        private void getPushParameterCode(ThreeAddressCodeList threeAddressCodeList,
                                          List<AbstractName> newParamNames,
                                          List<? extends AST> methodCallOrDefinitionArguments) {
            for (int i = newParamNames.size() - 1; i >= 0; i--) {
                final PushParameter pushParameter = new PushParameter(newParamNames.get(i), i, methodCallOrDefinitionArguments.get(i));
                threeAddressCodeList.addCode(pushParameter);
                pushParameter.setComment("# index param = " + i);
            }
        }

        @Override
        public ThreeAddressCodeList visit(MethodCall methodCall, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            flattenMethodCallArguments(threeAddressCodeList, methodCall.methodCallParameterList, symbolTable);
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(methodCall.builtinType.getFieldSize());

            threeAddressCodeList.addCode(new edu.mit.compilers.codegen.codes.MethodCall(methodCall, temporaryVariable, methodCall.getSourceCode()));
            threeAddressCodeList.place = temporaryVariable;
            return threeAddressCodeList;
        }


        @Override
        public ThreeAddressCodeList visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            flattenMethodCallArguments(threeAddressCodeList, methodCallStatement.methodCall.methodCallParameterList, symbolTable);
            threeAddressCodeList.addCode(new edu.mit.compilers.codegen.codes.MethodCall(methodCallStatement.methodCall, methodCallStatement.getSourceCode()));
            return threeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
            ThreeAddressCodeList lhs = locationAssignExpr.location.accept(this, symbolTable);
            AssignableName locationVariable = (AssignableName) lhs.place;
            ThreeAddressCodeList rhs = locationAssignExpr.assignExpr.accept(this, symbolTable);
            AbstractName valueVariable = rhs.place;
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
            ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(len.nameId.id);
            if (optionalDescriptor.isEmpty())
                throw new IllegalStateException(len.nameId.id + " should be present");
            else {
                ArrayDescriptor arrayDescriptor = (ArrayDescriptor) optionalDescriptor.get();
                TemporaryName temporaryNumberOfElemsVariable = TemporaryName.generateTemporaryName(arrayDescriptor.type.getFieldSize());
                threeAddressCodeList.addCode(new CopyInstruction(new ConstantName(arrayDescriptor.size), temporaryNumberOfElemsVariable, len));
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
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(BuiltinType.Int.getFieldSize());
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(new CopyInstruction(ConstantName.fromIntLiteral(charLiteral), temporaryVariable, charLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
            StringLiteralStackAllocation label = literalStackAllocationHashMap.get(stringLiteral.literal);
            StringConstantName stringConstantName = new StringConstantName(label);
            TemporaryName temporaryVariable = TemporaryName.generateTemporaryName(stringConstantName.size);
            return new ThreeAddressCodeList(
                    temporaryVariable,
                    Collections.singletonList(
                            new CopyInstruction(stringConstantName, temporaryVariable, stringLiteral)));
        }

        @Override
        public ThreeAddressCodeList visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
            return compoundAssignOpExpr.expression.accept(this, curSymbolTable);
        }

        @Override
        public ThreeAddressCodeList visit(Initialization initialization, SymbolTable symbolTable) {
            ThreeAddressCodeList initIdThreeAddressList = initialization.initId.accept(this, symbolTable);
            ThreeAddressCodeList initExpressionThreeAddressList = initialization.initExpression.accept(this, symbolTable);
            CopyInstruction copyInstruction = new CopyInstruction(initExpressionThreeAddressList.place, (AssignableName) initIdThreeAddressList.place, initialization);

            ThreeAddressCodeList initializationThreeAddressCodeList = new ThreeAddressCodeList(initIdThreeAddressList.place);
            initializationThreeAddressCodeList.add(initIdThreeAddressList);
            initializationThreeAddressCodeList.add(initExpressionThreeAddressList);
            initializationThreeAddressCodeList.addCode(copyInstruction);
            return initializationThreeAddressCodeList;
        }

        @Override
        public ThreeAddressCodeList visit(Assignment assignment, SymbolTable symbolTable) {
            ThreeAddressCodeList updateExprTAC = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            ThreeAddressCodeList lhs = assignment.location.accept(this, symbolTable);
            ThreeAddressCodeList rhs = assignment.assignExpr.expression.accept(this, symbolTable);
            updateExprTAC.add(rhs);
            updateExprTAC.add(lhs);

            if (assignment.assignExpr instanceof AssignOpExpr) {
                updateExprTAC.addCode(new OneOperandAssign(assignment, (AssignableName) lhs.place, rhs.place, ((AssignOpExpr) assignment.assignExpr).assignOp.getSourceCode()));
            } else if (assignment.assignExpr instanceof Decrement || assignment.assignExpr instanceof Increment) {
                updateExprTAC.addCode(new OneOperandAssign(assignment.assignExpr, (AssignableName) lhs.place, lhs.place, assignment.assignExpr.getSourceCode()));
            } else {
                updateExprTAC.addCode(new CopyInstruction(rhs.place, (AssignableName) lhs.place, assignment));
            }
            updateExprTAC.place = lhs.place;
            return updateExprTAC;
        }

        public ThreeAddressCodeList visit(Update update, SymbolTable symbolTable) {
            ThreeAddressCodeList updateExprTAC = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            ThreeAddressCodeList lhs = update.updateLocation.accept(this, symbolTable);

            ThreeAddressCodeList rhs;
            if (update.updateAssignExpr.expression != null) {
                rhs = update.updateAssignExpr.expression.accept(this, symbolTable);
            } else {
                rhs = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
            }
            updateExprTAC.add(rhs);
            updateExprTAC.add(lhs);

            if (update.updateAssignExpr instanceof CompoundAssignOpExpr) {
                updateExprTAC.addCode(new OneOperandAssign(update, (AssignableName) lhs.place, rhs.place, ((CompoundAssignOpExpr) update.updateAssignExpr).compoundAssignOp.getSourceCode()));
            } else if (update.updateAssignExpr instanceof Decrement || update.updateAssignExpr instanceof Increment) {
                updateExprTAC.addCode(new OneOperandAssign(update.updateAssignExpr, (AssignableName) lhs.place, lhs.place, update.updateAssignExpr.getSourceCode()));
            } else {
                updateExprTAC.addCode(new CopyInstruction(rhs.place, (AssignableName) lhs.place, update));
            }
            updateExprTAC.place = lhs.place;
            return updateExprTAC;
        }
    }


    ThreeAddressCodesConverter visitor;
    Label endLabelGlobal;
    Set<CFGBlock> visited = new HashSet<>();
    HashMap<CFGBlock, Label> blockToLabelHashMap = new HashMap<>();
    HashMap<CFGBlock, ThreeAddressCodeList> blockToCodeHashMap = new HashMap<>();
    public HashMap<String, SymbolTable> cfgSymbolTables;
    final private static HashMap<String, StringLiteralStackAllocation> literalStackAllocationHashMap = new HashMap<>();

    public ThreeAddressCodeList dispatch(CFGBlock cfgBlock, SymbolTable symbolTable) {
        if (cfgBlock instanceof CFGNonConditional)
            return visit((CFGNonConditional) cfgBlock, symbolTable);
        else
            return visit((CFGConditional) cfgBlock, symbolTable);
    }

    private List<AbstractName> getLocals(ThreeAddressCodeList threeAddressCodeList) {
        Set<AbstractName> uniqueNames = new HashSet<>();

        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            for (AbstractName name : threeAddressCode.getNames()) {
                if (name instanceof ArrayName && !globals.contains(name.toString())) {
                    uniqueNames.add(name);
                }
            }
        }

        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            for (AbstractName name : threeAddressCode.getNames()) {
                if (!(name instanceof ArrayName) && !globals.contains(name.toString())) {
                    uniqueNames.add(name);
                }
            }
        }
        return uniqueNames
                .stream()
                .filter((name -> ((name instanceof AssignableName))))
                .distinct()
                .sorted(Comparator.comparing(Object::toString))
                .collect(Collectors.toList());
    }

    private ThreeAddressCodeList convertMethodDefinition(MethodDefinition methodDefinition,
                                                         CFGBlock methodStart,
                                                         SymbolTable symbolTable) {

        TemporaryNameGenerator.reset();
        endLabelGlobal = new Label("LExit_" + methodDefinition.methodName.id, methodStart);

        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
        MethodBegin methodBegin = new MethodBegin(methodDefinition);
        threeAddressCodeList.addCode(methodBegin);

        flattenMethodDefinitionArguments(threeAddressCodeList, methodDefinition.methodDefinitionParameterList);

        threeAddressCodeList.setNext(dispatch(methodStart, symbolTable));
        threeAddressCodeList = threeAddressCodeList.flatten();

        threeAddressCodeList.addCode(endLabelGlobal);

        threeAddressCodeList.addCode(new MethodEnd(methodDefinition));
        methodBegin.setLocals(getLocals(threeAddressCodeList));

        return threeAddressCodeList;
    }


    public void flattenMethodDefinitionArguments(ThreeAddressCodeList threeAddressCodeList,
                                                 List<MethodDefinitionParameter> methodDefinitionParameterList) {

        for (int i = 0; i < methodDefinitionParameterList.size(); i++) {
            MethodDefinitionParameter parameter = methodDefinitionParameterList.get(i);
            threeAddressCodeList.addCode(new PopParameter(
                    new VariableName(parameter.id.id),
                    parameter,
                    i,
                    "# index param = " + i
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
                threeAddressCodeList.addCode(new DataSectionAllocation(name, "# " + name.getSourceCode(), new VariableName(name.id), fieldDeclaration.builtinType.getFieldSize(), fieldDeclaration.builtinType));
            }
            for (Array array : fieldDeclaration.arrays) {
                long size = (fieldDeclaration.builtinType.getFieldSize() * array.size.convertToLong());
                threeAddressCodeList.addCode(new DataSectionAllocation(array, "# " + array.getSourceCode(),
                        new ArrayName(array.id.id,
                                size), size, fieldDeclaration.builtinType));
            }
        }
        return threeAddressCodeList;
    }


    private ThreeAddressCodeList initProgram(Program program) {
        ThreeAddressCodeList threeAddressCodeList = fillOutGlobals(program.fieldDeclarationList);
        this.globals = getGlobals(threeAddressCodeList);
        Set<String> stringLiteralList = findAllStringLiterals(program);
        for (String stringLiteral : stringLiteralList) {
            final StringLiteralStackAllocation literalStackAllocation = new StringLiteralStackAllocation(stringLiteral);
            threeAddressCodeList.addCode(literalStackAllocation);
            literalStackAllocationHashMap.put(stringLiteral, literalStackAllocation);
        }
        return threeAddressCodeList;
    }

    private Set<String> getGlobals(ThreeAddressCodeList threeAddressCodeList) {
        HashSet<String> set = new HashSet<>();
        for (ThreeAddressCode threeAddressCode : threeAddressCodeList) {
            for (AbstractName name : threeAddressCode.getNames()) {
                if (name instanceof VariableName)
                    set.add(name.toString());
            }
        }
        return set;
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
        ThreeAddressCodeList threeAddressCodeList = initProgram(program);
        threeAddressCodeList.add(
                convertMethodDefinition(
                        getMethodDefinitionFromProgram("main", program),
                        visitor.methodCFGBlocks.get("main"),
                        cfgSymbolTables.get("main"))
        );
        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (!k.equals("main")) {
                threeAddressCodeList.add(convertMethodDefinition(getMethodDefinitionFromProgram(k, program), v, cfgSymbolTables.get(k)));
            }
        });
        return threeAddressCodeList;
    }

    public ThreeAddressCodesListConverter(GlobalDescriptor globalDescriptor) {
        this.visitor = new ThreeAddressCodesConverter();
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
        blockToCodeHashMap.put(cfgNonConditional, universalThreeAddressCodeList);
        if (cfgNonConditional.autoChild != null) {
            if (visited.contains(cfgNonConditional.autoChild)) {
                assert blockToLabelHashMap.containsKey(cfgNonConditional.autoChild);
                universalThreeAddressCodeList.setNext(ThreeAddressCodeList.of(new UnconditionalJump(blockToLabelHashMap.get(cfgNonConditional.autoChild))));
            } else {
                universalThreeAddressCodeList.setNext(cfgNonConditional.autoChild.accept(this, symbolTable));
            }
        } else {
            universalThreeAddressCodeList.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabelGlobal)));
        }
        return universalThreeAddressCodeList;
    }

    private Label getLabel(CFGBlock cfgBlock, Label from) {
        if (cfgBlock == null) {
            return endLabelGlobal;
        }
        BiFunction<CFGBlock, Label, Label> function = (cfgBlock1, label) -> {
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
            CFGBlock child,
            Label conditionalLabel,
            SymbolTable symbolTable) {

        ThreeAddressCodeList codeList;
        ThreeAddressCodeList trueBlock;

        if (child != null) {
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
    public ThreeAddressCodeList visit(CFGConditional cfgConditional, SymbolTable symbolTable) {
        visited.add(cfgConditional);

        Expression condition = (Expression) (cfgConditional.condition).ast;

        ThreeAddressCodeList testConditionThreeAddressList = getConditionTACList(condition, symbolTable);

        final Label conditionLabel = getLabel(cfgConditional, null);

        final ThreeAddressCodeList conditionLabelTACList = ThreeAddressCodeList.of(conditionLabel);
        conditionLabelTACList.add(testConditionThreeAddressList);

        blockToCodeHashMap.put(cfgConditional, conditionLabelTACList);

        final ThreeAddressCodeList trueBlock = getConditionalChildBlock(cfgConditional.trueChild, conditionLabel, symbolTable);
        final ThreeAddressCodeList falseBlock = getConditionalChildBlock(cfgConditional.falseChild, conditionLabel, symbolTable);


        Label falseLabel = getLabel(cfgConditional.falseChild, conditionLabel);
        Label endLabel = new Label(conditionLabel.label + "end", null);

        JumpIfFalse jumpIfFalse =
                new JumpIfFalse(condition,
                        testConditionThreeAddressList.place,
                        falseLabel, "if !(" + cfgConditional.condition.ast.getSourceCode() + ")");
        conditionLabelTACList.addCode(jumpIfFalse);
        if (!(trueBlock.last() instanceof UnconditionalJump))
            trueBlock.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabel)));

        // no need for consecutive similar labels
        if (!falseBlock.isEmpty() && falseBlock.first() instanceof UnconditionalJump) {
            UnconditionalJump unconditionalJump = (UnconditionalJump) falseBlock.first();
            if (!unconditionalJump.goToLabel.label.equals(falseLabel.label))
                falseBlock.prepend(falseLabel);
        } else {
            falseBlock.prepend(falseLabel);
        }
        if (!(falseBlock.last() instanceof UnconditionalJump) && !(trueBlock.last() instanceof UnconditionalJump))
            falseBlock.setNext(ThreeAddressCodeList.of(new UnconditionalJump(endLabel)));
        if (falseBlock.flattenedSize() == 1 && falseBlock.first() instanceof UnconditionalJump) {
            conditionLabelTACList.setNext(trueBlock);
            return conditionLabelTACList;
        }
        conditionLabelTACList
                .setNext(trueBlock)
                .setNext(falseBlock);
        return conditionLabelTACList;
    }

    @Override
    public ThreeAddressCodeList visit(NOP nop, SymbolTable symbolTable) {
        return new ThreeAddressCodeList(ThreeAddressCodeList.UNDEFINED);
    }
}

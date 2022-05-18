package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.AssignOpExpr;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Break;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.CharLiteral;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.Continue;
import edu.mit.compilers.ast.DecimalLiteral;
import edu.mit.compilers.ast.Decrement;
import edu.mit.compilers.ast.Expression;
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
import edu.mit.compilers.ast.Location;
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
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.ast.While;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockVisitor;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.cfg.CFGGenerator;
import edu.mit.compilers.cfg.CFGVisitor;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.SymbolTableFlattener;
import edu.mit.compilers.codegen.codes.ArrayBoundsCheck;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.ConditionalJump;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.MethodReturn;
import edu.mit.compilers.codegen.codes.PopParameter;
import edu.mit.compilers.codegen.codes.PushArgument;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringLiteralStackAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.codegen.names.StringConstantName;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.codegen.names.VariableName;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Operators;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.ProgramIr;
import edu.mit.compilers.utils.Utils;

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
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, ConstantName.fromBooleanLiteral(booleanLiteral), booleanLiteral, place + " = " + booleanLiteral)));
        }

        @Override
        public InstructionList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation(BuiltinType.Int);
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, ConstantName.fromIntLiteral(decimalLiteral), decimalLiteral, place + " = " + decimalLiteral)));
        }

        @Override
        public InstructionList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
            var place = resolveStoreLocation(BuiltinType.Int);
            return new InstructionList(
                    place,
                    Collections.singletonList(new Assign(place, ConstantName.fromIntLiteral(hexLiteral), hexLiteral, place + " = " + hexLiteral)));
        }

        @Override
        public InstructionList visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
            InstructionList instructionList = new InstructionList();
            for (Name name: fieldDeclaration.names) {
                if (fieldDeclaration.builtinType.equals(BuiltinType.Int) || fieldDeclaration.builtinType.equals(BuiltinType.Bool)) {
                    instructionList.add(new Assign(new VariableName(name.id, Utils.WORD_SIZE, BuiltinType.Int), ConstantName.zero(), name, name.id + " = " + 0));
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
            retTACList.add(new UnaryInstruction(place, unaryOpExpression.op.getSourceCode(), operandTACList.place, unaryOpExpression));
            retTACList.place = place;
            return retTACList;
        }

        @Override
        public InstructionList visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
            final var leftTACList = binaryOpExpression.lhs.accept(this, symbolTable);
            final var rightTACList = binaryOpExpression.rhs.accept(this, symbolTable);

            final var binOpExpressionTACList = new InstructionList();
            binOpExpressionTACList.addAll(leftTACList);
            binOpExpressionTACList.addAll(rightTACList);

            AssignableName place;
            place = resolveStoreLocation(binaryOpExpression.builtinType);

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


        private void addArrayAccessBoundsCheck(InstructionList instructionList, GetAddress getAddress) {
            final int boundsIndex = TemporaryNameIndexGenerator.getNextBoundsCheckLabel();
            instructionList.add(new ArrayBoundsCheck(getAddress, boundsIndex));
        }

        @Override
        public InstructionList visit(LocationArray locationArray, SymbolTable symbolTable) {
            final var indexInstructionList = locationArray.expression.accept(this, symbolTable);
            final var locationArrayInstructionList = new InstructionList();
            locationArrayInstructionList.addAll(indexInstructionList);

            final var descriptorFromValidScopes = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
            final var arrayDescriptor = (ArrayDescriptor) descriptorFromValidScopes.orElseThrow(
                    () -> new IllegalStateException("expected to find array " + locationArray.name.id + " in scope")
            );
            final var arrayName = new VariableName(locationArray.name.id, arrayDescriptor.size * Utils.WORD_SIZE, arrayDescriptor.type);
            final var getAddressInstruction = new GetAddress(locationArray, arrayName, indexInstructionList.place, generateAddressName(arrayDescriptor.type), new ConstantName(arrayDescriptor.size, BuiltinType.Int));
            locationArrayInstructionList.add(getAddressInstruction);

            if (!(indexInstructionList.place instanceof ConstantName))
                addArrayAccessBoundsCheck(locationArrayInstructionList, getAddressInstruction);
            locationArrayInstructionList.place = getAddressInstruction.store;
            return locationArrayInstructionList;
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
                expressionParameterTACList.add(new Assign(temporaryVariable, expressionTACList.place, expressionParameter, temporaryVariable + " = " + expressionTACList.place));
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
                final var pushParameter = new PushArgument(newParamNames.get(i), i, methodCallOrDefinitionArguments.get(i));
                instructionList.add(pushParameter);
            }
        }

        private AssignableName resolveStoreLocation(BuiltinType builtinType) {
            return TemporaryName.generateTemporaryName(builtinType);
        }

        private AssignableName generateAddressName(BuiltinType builtinType) {
            return new MemoryAddressName(TemporaryNameIndexGenerator.getNextTemporaryVariable(), Utils.WORD_SIZE, builtinType);
        }

        @Override
        public InstructionList visit(MethodCall methodCall, SymbolTable symbolTable) {
            final InstructionList instructionList = new InstructionList();
            flattenMethodCallArguments(instructionList, methodCall.methodCallParameterList, symbolTable);
            var place = resolveStoreLocation(methodCall.builtinType);
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
            lhs.add(flattenCompoundAssign(locationVariable, valueVariable, locationAssignExpr.getSourceCode(), locationAssignExpr));
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
            InstructionList initExpressionThreeAddressList = initialization.initExpression.accept(this, symbolTable);
            Assign copyInstruction = new Assign((AssignableName) initIdThreeAddressList.place, initExpressionThreeAddressList.place, initialization, initialization.getSourceCode());

            InstructionList initializationInstructionList = new InstructionList(initIdThreeAddressList.place);
            initializationInstructionList.addAll(initIdThreeAddressList);
            initializationInstructionList.addAll(initExpressionThreeAddressList);
            initializationInstructionList.add(copyInstruction);
            return initializationInstructionList;
        }

        private Instruction flattenCompoundAssign(AssignableName lhs, AbstractName rhs, String op, AST assignment) {
            final var constant = new ConstantName(1L, BuiltinType.Int);
            switch (op) {
                case Operators.ADD_ASSIGN:
                    return new BinaryInstruction(lhs, lhs, Operators.PLUS, rhs, assignment.getSourceCode(), assignment);
                case Operators.MULTIPLY_ASSIGN:
                    return new BinaryInstruction(lhs, lhs, Operators.MULTIPLY, rhs, assignment.getSourceCode(), assignment);
                case Operators.MINUS_ASSIGN:
                    return new BinaryInstruction(lhs, lhs, Operators.MINUS, rhs, assignment.getSourceCode(), assignment);
                case Operators.DECREMENT:
                    return new BinaryInstruction(lhs, lhs, Operators.MINUS, constant, assignment.getSourceCode(), assignment);
                case Operators.INCREMENT:
                    return new BinaryInstruction(lhs, lhs, Operators.PLUS, constant, assignment.getSourceCode(), assignment);
                default:
                    return new Assign(lhs, rhs, assignment, assignment.getSourceCode());
            }
        }

        @Override
        public InstructionList visit(Assignment assignment, SymbolTable symbolTable) {
            var instructionList = new InstructionList();
            var lhsInstructionList = assignment.location.accept(this, symbolTable);
            InstructionList rhsInstructionList;
            if (assignment.assignExpr.expression != null) {
                rhsInstructionList = assignment.assignExpr.expression.accept(this, symbolTable);
            } else {
                rhsInstructionList = new InstructionList();
            }
            instructionList.addAll(rhsInstructionList);
            instructionList.addAll(lhsInstructionList);

            instructionList.add(flattenCompoundAssign((AssignableName) lhsInstructionList.place, rhsInstructionList.place, assignment.getOperator(), assignment));
            instructionList.place = lhsInstructionList.place;
            return instructionList;
        }
    }

    final private static HashMap<String, StringLiteralStackAllocation> literalStackAllocationHashMap = new HashMap<>();

    InstructionConverter visitor;
    Set<BasicBlock> visited = new HashSet<>();
    public HashMap<String, SymbolTable> cfgSymbolTables;

    NOP nop;

    private MethodBegin convertMethodDefinition(MethodDefinition methodDefinition,
                                                BasicBlock methodStart,
                                                SymbolTable symbolTable) {

        Label methodExitLabel = new Label("exit_" + methodDefinition.methodName.id);

        var instructions = new InstructionList();
        var methodBegin = new MethodBegin(methodDefinition);
        instructions.add(methodBegin);

        instructions.addAll(
                errors.stream()
                        .map(error -> new RuntimeException(error.getMessage(), -2, error))
                        .collect(Collectors.toList()));
        // if the errors list is non-empty, set hasRuntimeException to true
        methodBegin.setHasRuntimeException(!errors.isEmpty());

        flattenMethodDefinitionArguments(instructions, methodDefinition.methodDefinitionParameterList);

        final var firstBasicBlockTacList = methodStart.accept(this, symbolTable);
        firstBasicBlockTacList.addAll(0, instructions);

        nop.setLabel(methodExitLabel);
        nop.instructionList.add(methodExitLabel);
        nop.instructionList.add(new MethodEnd(methodDefinition));

        methodStart.instructionList = firstBasicBlockTacList;
        methodBegin.entryBlock = methodStart;
        methodBegin.exitBlock = nop;
        methodBegin.unoptimized = TraceScheduler.flattenIr(methodBegin);
        return methodBegin;
    }


    public void flattenMethodDefinitionArguments(InstructionList instructionList,
                                                 List<MethodDefinitionParameter> methodDefinitionParameterList) {

        for (int i = 0; i < methodDefinitionParameterList.size(); i++) {
            MethodDefinitionParameter parameter = methodDefinitionParameterList.get(i);
            instructionList.add(new PopParameter(
                    new VariableName(parameter.id.id, Utils.WORD_SIZE, parameter.builtinType),
                    parameter,
                    i
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
                        new VariableName(array.id.id,
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
                if (name instanceof VariableName || name instanceof MemoryAddressName)
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

    public ProgramIr fill(CFGVisitor visitor,
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
        return new ProgramIr(programStartTacList, methodsTacLists);
    }

    public InstructionListConverter(CFGGenerator cfgGenerator) {
        this.errors = cfgGenerator.errors;
        this.visitor = new InstructionConverter();
        SymbolTableFlattener symbolTableFlattener = new SymbolTableFlattener(cfgGenerator.globalDescriptor);
        this.cfgSymbolTables = symbolTableFlattener.createCFGSymbolTables();
    }

    @Override
    public InstructionList visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable) {
        if (visited.contains(basicBlockBranchLess))
            return basicBlockBranchLess.instructionList;
        visited.add(basicBlockBranchLess);
        InstructionList instructionList = new InstructionList();
        instructionList.add(basicBlockBranchLess.getLabel());
        for (var line : basicBlockBranchLess.lines)
            instructionList.addAll(line.accept(visitor, symbolTable));
        basicBlockBranchLess.autoChild.accept(this, symbolTable);
        basicBlockBranchLess.instructionList = instructionList;
        return instructionList;
    }

    private InstructionList getConditionTACList(Expression condition, SymbolTable symbolTable) {
        return condition.accept(this.visitor, symbolTable);
    }
    private Label getNewLabel() {
        return new Label(TemporaryNameIndexGenerator.getNextLabel());
    }

    @Override
    public InstructionList visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable) {
        if (visited.contains(basicBlockWithBranch))
            return basicBlockWithBranch.instructionList;

        visited.add(basicBlockWithBranch);

        final Expression condition = basicBlockWithBranch.condition;
        InstructionList conditionInstructionList = getConditionTACList(condition, symbolTable);
        conditionInstructionList.add(0, basicBlockWithBranch.getLabel());

        basicBlockWithBranch.trueChild.accept(this, symbolTable);
        basicBlockWithBranch.falseChild.accept(this, symbolTable);

        ConditionalJump jumpIfFalse =
                new ConditionalJump(condition,
                        conditionInstructionList.place,
                        basicBlockWithBranch.falseChild.getLabel(), "if !(" + basicBlockWithBranch.condition.getSourceCode() + ")");
        conditionInstructionList.add(jumpIfFalse);
        basicBlockWithBranch.instructionList = conditionInstructionList;
        return conditionInstructionList;
    }

    @Override
    public InstructionList visit(NOP nop, SymbolTable symbolTable) {
        this.nop = nop;
        visited.add(nop);
        nop.instructionList = new InstructionList();
        return nop.instructionList;
    }
}

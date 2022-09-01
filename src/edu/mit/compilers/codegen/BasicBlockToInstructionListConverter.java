package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.BasicBlockBranchLess;
import edu.mit.compilers.cfg.BasicBlockVisitor;
import edu.mit.compilers.cfg.BasicBlockWithBranch;
import edu.mit.compilers.cfg.CFGVisitor;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.SymbolTableFlattener;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Temporary;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.ProgramIr;

public class BasicBlockToInstructionListConverter implements BasicBlockVisitor<InstructionList> {
    private final List<DecafException> cfgGenerationErrors;
    private final Set<LValue> globalNames = new HashSet<>();
    private final Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
    private final HashMap<String, SymbolTable> perMethodSymbolTables;
    private final ProgramIr programIr;
    private final HashMap<String, StringLiteralAllocation> stringToStringLiteralAllocationMapping = new HashMap<>();

    private AstToInstructionListConverter currentAstToInstructionListConverter;
    private NOP currentMethodExitNop;

    public HashMap<String, SymbolTable> getPerMethodSymbolTables() {
        return perMethodSymbolTables;
    }

    public Set<LValue> getGlobalNames() {
        return globalNames;
    }

    private Method generateMethodInstructionList(MethodDefinition methodDefinition,
                                                 BasicBlock methodStart,
                                                 SymbolTable currentSymbolTable) {
        currentAstToInstructionListConverter = new AstToInstructionListConverter(currentSymbolTable, stringToStringLiteralAllocationMapping);

        var methodExitLabel = new Label("exit_" + methodDefinition.methodName.getLabel());

        var methodInstructionList = new InstructionList();
        var method = new Method(methodDefinition);
        methodInstructionList.add(method);
        methodInstructionList.addAll(
                method.methodDefinition.parameterList
                        .stream()
                        .map(
                                parameterName -> new CopyInstruction(new Variable(parameterName.getName(), parameterName.getType()), new Variable(parameterName.getName() + "_arg", parameterName.getType()), null, null)
                        )
                        .toList()
        );

        methodInstructionList.addAll(
                cfgGenerationErrors.stream()
                                   .map(error -> new RuntimeException(error.getMessage(), -2, error))
                                   .toList());
        // if the errors list is non-empty, set hasRuntimeException to true
        method.setHasRuntimeException(!cfgGenerationErrors.isEmpty());

        final var entryBasicBlockInstructionList = methodStart.accept(this);
        entryBasicBlockInstructionList.addAll(0, methodInstructionList);

        currentMethodExitNop.setLabel(methodExitLabel);
        currentMethodExitNop.getInstructionList()
                            .add(methodExitLabel);
        currentMethodExitNop.getInstructionList()
                            .add(new MethodEnd(methodDefinition));

        methodStart.setInstructionList(entryBasicBlockInstructionList);
        method.entryBlock = methodStart;
        method.exitBlock = currentMethodExitNop;
        method.unoptimizedInstructionList = TraceScheduler.flattenIr(method);
        return method;
    }


    private InstructionList getProgramHeaderInstructionList(Program program) {
        var programHeaderInstructionList = new InstructionList();
        for (var fieldDeclaration : program.fieldDeclarationList) {
            for (var name : fieldDeclaration.names) {
                programHeaderInstructionList.add(new GlobalAllocation(name, "# " + name.getSourceCode(),
                        new Variable(name.getLabel(), fieldDeclaration.getType()), fieldDeclaration.getType()
                                                                                                   .getFieldSize(), fieldDeclaration.getType()));
            }
            for (var array : fieldDeclaration.arrays) {
                var size = (fieldDeclaration.getType()
                                            .getFieldSize() * array.getSize()
                                                                   .convertToLong());
                programHeaderInstructionList.add(new GlobalAllocation(array, "# " + array.getSourceCode(),
                        new Variable(array.getId()
                                          .getLabel(), fieldDeclaration.getType()), size, fieldDeclaration.getType()));
            }
        }
        globalNames.addAll(programHeaderInstructionList.stream()
                                                       .flatMap(instruction -> instruction.getAllNames()
                                                                                          .stream())
                                                       .filter(abstractName -> abstractName instanceof LValue)
                                                       .map(abstractName -> (LValue) abstractName)
                                                       .collect(Collectors.toUnmodifiableSet()));

        for (String stringLiteral : findAllStringLiterals(program)) {
            final var stringLiteralAllocation = new StringLiteralAllocation(stringLiteral);
            programHeaderInstructionList.add(stringLiteralAllocation);
            stringToStringLiteralAllocationMapping.put(stringLiteral, stringLiteralAllocation);
        }
        return programHeaderInstructionList;
    }

    private Set<String> findAllStringLiterals(Program program) {
        var literalList = new HashSet<String>();
        var toExplore = new Stack<AST>();

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

    public BasicBlockToInstructionListConverter(GlobalDescriptor globalDescriptor,
                                                List<DecafException> cfgGenerationErrors,
                                                CFGVisitor cfgVisitor,
                                                Program program) {
        SymbolTableFlattener symbolTableFlattener = new SymbolTableFlattener(globalDescriptor);
        this.perMethodSymbolTables = symbolTableFlattener.createCFGSymbolTables();
        this.cfgGenerationErrors = cfgGenerationErrors;
        var programHeaderInstructionList = getProgramHeaderInstructionList(program);
        var methodInstructionLists = new ArrayList<Method>();
        cfgVisitor.methodCFGBlocks.forEach(
                (methodName, methodEntryBasicBlock) -> methodInstructionLists
                        .add(generateMethodInstructionList(
                                program.methodDefinitionList
                                        .stream()
                                        .filter(methodDefinition -> methodDefinition.methodName.getLabel()
                                                                                               .equals(methodName))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("expected to find method " + methodName))
                                , methodEntryBasicBlock, perMethodSymbolTables.get(methodName))));
        this.programIr = new ProgramIr(programHeaderInstructionList, methodInstructionLists);
    }

    public ProgramIr getProgramIr() {
        return programIr;
    }

    @Override
    public InstructionList visit(BasicBlockBranchLess basicBlockBranchLess) {
        if (visitedBasicBlocks.contains(basicBlockBranchLess))
            return basicBlockBranchLess.getInstructionList();
        visitedBasicBlocks.add(basicBlockBranchLess);
        InstructionList instructionList = new InstructionList();
        instructionList.add(basicBlockBranchLess.getLabel());
        for (var line : basicBlockBranchLess.getAstNodes())
            instructionList.addAll(line.accept(currentAstToInstructionListConverter, null));
        basicBlockBranchLess.getSuccessor()
                            .accept(this);
        basicBlockBranchLess.setInstructionList(instructionList);
        return instructionList;
    }


    @Override
    public InstructionList visit(BasicBlockWithBranch basicBlockWithBranch) {
        if (visitedBasicBlocks.contains(basicBlockWithBranch))
            return basicBlockWithBranch.getInstructionList();

        visitedBasicBlocks.add(basicBlockWithBranch);

        var condition = basicBlockWithBranch.getBranchCondition();
        var conditionInstructionList = condition.accept(currentAstToInstructionListConverter, Temporary.generateTemporaryName(Type.Bool));
        conditionInstructionList.add(0, basicBlockWithBranch.getLabel());

        basicBlockWithBranch.getTrueTarget()
                            .accept(this);
        basicBlockWithBranch.getFalseTarget()
                            .accept(this);

        var branchCondition =
                new ConditionalBranch(condition,
                        conditionInstructionList.place,
                        basicBlockWithBranch.getFalseTarget()
                                            .getLabel(), "if !(" + basicBlockWithBranch.getBranchCondition()
                                                                                       .getSourceCode() + ")");
        conditionInstructionList.add(branchCondition);
        basicBlockWithBranch.setInstructionList(conditionInstructionList);
        return conditionInstructionList;
    }

    @Override
    public InstructionList visit(NOP nop) {
        this.currentMethodExitNop = nop;
        visitedBasicBlocks.add(nop);
        return nop.getInstructionList();
    }
}

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
import edu.mit.compilers.cfg.ControlFlowGraphVisitor;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.SymbolTableFlattener;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.RuntimeException;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.ProgramIr;

public class BasicBlockToInstructionListConverter {
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
                cfgGenerationErrors.stream()
                                   .map(error -> new RuntimeException(error.getMessage(), -2, error))
                                   .toList());
        // if the errors list is non-empty, set hasRuntimeException to true
        method.setHasRuntimeException(!cfgGenerationErrors.isEmpty());

        final var entryBasicBlockInstructionList = visit(methodStart);
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
                                                       .flatMap(instruction -> instruction.getAllValues()
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
                                                ControlFlowGraphVisitor cfgVisitor,
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


    public InstructionList visit(BasicBlock basicBlock) {
        return switch (basicBlock.getBasicBlockType()) {
            case NO_BRANCH -> visitBasicBlockBranchLess(basicBlock);
            case BRANCH -> visitBasicBlockWithBranch(basicBlock);
            default -> visitNOP((NOP) basicBlock);
        };
    }

    public InstructionList visitBasicBlockBranchLess(BasicBlock basicBlockBranchLess) {
        if (visitedBasicBlocks.contains(basicBlockBranchLess))
            return basicBlockBranchLess.getInstructionList();
        visitedBasicBlocks.add(basicBlockBranchLess);
        InstructionList instructionList = new InstructionList();
        instructionList.add(basicBlockBranchLess.getLabel());
        for (var line : basicBlockBranchLess.getAstNodes())
            instructionList.addAll(line.accept(currentAstToInstructionListConverter, null));
        visit(basicBlockBranchLess.getSuccessor());
        basicBlockBranchLess.setInstructionList(instructionList);
        return instructionList;
    }


    public InstructionList visitBasicBlockWithBranch(BasicBlock basicBlockWithBranch) {
        if (visitedBasicBlocks.contains(basicBlockWithBranch))
            return basicBlockWithBranch.getInstructionList();

        visitedBasicBlocks.add(basicBlockWithBranch);

        var condition = basicBlockWithBranch.getBranchCondition().orElseThrow();
        var conditionInstructionList = condition.accept(currentAstToInstructionListConverter, Variable.genTemp(Type.Bool));
        conditionInstructionList.add(0, basicBlockWithBranch.getLabel());

        visit(basicBlockWithBranch.getTrueTarget());
        visit(basicBlockWithBranch.getFalseTarget());

        var branchCondition =
                new ConditionalBranch(condition,
                        conditionInstructionList.place,
                        basicBlockWithBranch.getFalseTarget()
                                            .getLabel(), "if !(" + basicBlockWithBranch.getBranchCondition().orElseThrow()
                                                                                       .getSourceCode() + ")");
        conditionInstructionList.add(branchCondition);
        basicBlockWithBranch.setInstructionList(conditionInstructionList);
        return conditionInstructionList;
    }

    public InstructionList visitNOP(NOP nop) {
        this.currentMethodExitNop = nop;
        visitedBasicBlocks.add(nop);
        return nop.getInstructionList();
    }
}

package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.cfg.ControlFlowGraph;
import edu.mit.compilers.cfg.NOP;
import edu.mit.compilers.cfg.SymbolTableFlattener;
import edu.mit.compilers.codegen.codes.ConditionalBranch;
import edu.mit.compilers.codegen.codes.GlobalAllocation;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.StringConstantAllocation;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.StringConstant;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.ProgramIr;

public class BasicBlockToInstructionListConverter {
    private final Set<LValue> globalNames = new HashSet<>();
    private final Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
    private final HashMap<String, SymbolTable> perMethodSymbolTables;
    private final ProgramIr programIr;
    private final HashMap<String, StringConstant> stringConstantsMap = new HashMap<>();

    private AstToInstructionListConverter currentAstToInstructionListConverter;
    private NOP currentMethodExitNop;

    public BasicBlockToInstructionListConverter(
            ControlFlowGraph controlFlowGraph) {
        var symbolTableFlattener = new SymbolTableFlattener(controlFlowGraph.getGlobalDescriptor());
        this.perMethodSymbolTables = symbolTableFlattener.createCFGSymbolTables();
        var prologue = getPrologue(controlFlowGraph.getProgram());
        var methods = new ArrayList<Method>();
        controlFlowGraph.getMethodNameToEntryBlock().forEach(
                (methodName, entryBlock) -> methods
                        .add(generateMethodInstructionList(
                                controlFlowGraph.getProgram().methodDefinitionList
                                        .stream()
                                        .filter(methodDefinition -> methodDefinition.methodName.getLabel()
                                                .equals(methodName))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException("expected to find method " + methodName))
                                , entryBlock.getSuccessor(), perMethodSymbolTables.get(methodName))));
        this.programIr = new ProgramIr(prologue, methods);
        this.programIr.renumberLabels();
    }

    public HashMap<String, SymbolTable> getPerMethodSymbolTables() {
        return perMethodSymbolTables;
    }

    public Set<LValue> getGlobalNames() {
        return globalNames;
    }

    private Method generateMethodInstructionList(MethodDefinition methodDefinition,
                                                 BasicBlock methodStart,
                                                 SymbolTable currentSymbolTable) {
        currentAstToInstructionListConverter = new AstToInstructionListConverter(currentSymbolTable, stringConstantsMap);

//
//        methodInstructionList.addAll(
//                cfgGenerationErrors.stream()
//                                   .map(error -> new RuntimeException(error.getMessage(), -2, error))
//                                   .toList());
//        // if the errors list is non-empty, set hasRuntimeException to true
//        method.setHasRuntimeException(!cfgGenerationErrors.isEmpty());

        final var entryBasicBlockInstructionList = visit(methodStart);
        entryBasicBlockInstructionList.setEntry();
        var method = new Method(methodDefinition);
        method.entryBlock = methodStart;
        method.exitBlock = currentMethodExitNop;
        entryBasicBlockInstructionList.add(0, method);

        var methodExitLabel = "exit_" + methodDefinition.methodName.getLabel();
        currentMethodExitNop.getInstructionList().setLabel(methodExitLabel);
        currentMethodExitNop.getInstructionList()
                .add(new MethodEnd(methodDefinition));

        methodStart.setInstructionList(entryBasicBlockInstructionList);
        method.unoptimizedInstructionList = TraceScheduler.flattenIr(method);
        return method;
    }

    private InstructionList getPrologue(Program program) {
        var prologue = new InstructionList();
        for (var fieldDeclaration : program.fieldDeclarationList) {
            for (var name : fieldDeclaration.names) {
                prologue.add(new GlobalAllocation(name, "# " + name.getSourceCode(),
                        new Variable(name.getLabel(), fieldDeclaration.getType()), fieldDeclaration.getType()
                        .getFieldSize(), fieldDeclaration.getType()));
            }
            for (var array : fieldDeclaration.arrays) {
                var size = (fieldDeclaration.getType()
                        .getFieldSize() * array.getSize()
                        .convertToLong());
                prologue.add(new GlobalAllocation(array, "# " + array.getSourceCode(),
                        new Variable(array.getId()
                                .getLabel(), fieldDeclaration.getType()), size, fieldDeclaration.getType()));
            }
        }
        globalNames.addAll(prologue.stream()
                .flatMap(instruction -> instruction.getAllValues()
                        .stream())
                .filter(abstractName -> abstractName instanceof LValue)
                .map(abstractName -> (LValue) abstractName)
                .collect(Collectors.toUnmodifiableSet()));

        for (String stringLiteral : findAllStringLiterals(program)) {
            final var stringConstant = new StringConstant(stringLiteral);
            prologue.add(new StringConstantAllocation(stringConstant));
            stringConstantsMap.put(stringLiteral, stringConstant);
        }
        return prologue;
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
        for (var line : basicBlockBranchLess.getAstNodes())
            instructionList.addAll(line.accept(currentAstToInstructionListConverter, null));
        assert basicBlockBranchLess.getSuccessor() != null;
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

        visit(basicBlockWithBranch.getTrueTarget());
        visit(basicBlockWithBranch.getFalseTarget());

        var branchCondition =
                new ConditionalBranch(condition,
                        conditionInstructionList.place,
                        basicBlockWithBranch.getFalseTarget(), "if !(" + basicBlockWithBranch.getBranchCondition().orElseThrow()
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

package decaf.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.codegen.codes.ConditionalBranch;
import decaf.codegen.names.IrGlobalArray;
import decaf.codegen.names.IrGlobalScalar;
import decaf.ast.AST;
import decaf.ast.MethodDefinition;
import decaf.ast.Program;
import decaf.ast.StringLiteral;
import decaf.ast.Type;
import decaf.cfg.BasicBlock;
import decaf.cfg.ControlFlowGraph;
import decaf.cfg.NOP;
import decaf.cfg.SymbolTableFlattener;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.GlobalAllocation;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.codes.StringConstantAllocation;
import decaf.codegen.names.IrGlobal;
import decaf.codegen.names.IrIntegerConstant;
import decaf.codegen.names.IrStringConstant;
import decaf.codegen.names.IrRegister;
import decaf.symboltable.SymbolTable;
import decaf.common.Pair;
import decaf.common.ProgramIr;

public class BasicBlockToInstructionListConverter {
  private final Set<IrGlobal> globalNames = new HashSet<>();
  private final Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
  private final HashMap<String, SymbolTable> perMethodSymbolTables;
  private final ProgramIr programIr;
  private final HashMap<String, IrStringConstant> stringConstantsMap = new HashMap<>();

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

  public Set<IrGlobal> getGlobalNames() {
    return globalNames;
  }

  private Method generateMethodInstructionList(
      MethodDefinition methodDefinition,
      BasicBlock methodStart,
      SymbolTable currentSymbolTable
  ) {
    currentAstToInstructionListConverter = new AstToInstructionListConverter(currentSymbolTable,
        stringConstantsMap,
        globalNames);

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
    method.setEntryBlock(methodStart);
    method.setExitBlock(currentMethodExitNop);
    entryBasicBlockInstructionList.add(0,
        method);

    var methodExitLabel = "exit_" + methodDefinition.methodName.getLabel();
    currentMethodExitNop.getInstructionList()
                        .setLabel(methodExitLabel);
    currentMethodExitNop.getInstructionList()
                        .add(new MethodEnd(methodDefinition));

    methodStart.setInstructionList(entryBasicBlockInstructionList);
    var instructions = new ArrayList<Instruction>();
    for (var local : ProgramIr.getNonParamLocals(method,
        globalNames)) {
      instructions.add(CopyInstruction.noMetaData(local.copy(),
          IrIntegerConstant.zero()));
    }
    entryBasicBlockInstructionList.addAll(1,
        instructions);
    method.setUnoptimizedInstructionList(TraceScheduler.flattenIr(method));
    return method;
  }

  private InstructionList getPrologue(Program program) {
    var prologue = new InstructionList();
    for (var fieldDeclaration : program.fieldDeclarationList) {
      for (var name : fieldDeclaration.names) {
        prologue.add(new GlobalAllocation(new IrGlobalScalar(name.getLabel(),
                                                             fieldDeclaration.getType()),
            fieldDeclaration.getType()
                            .getFieldSize(),
            fieldDeclaration.getType(),
            name,
            "# " + name.getSourceCode()));
      }
      for (var array : fieldDeclaration.arrays) {
        var size = (fieldDeclaration.getType()
                                    .getFieldSize() * array.getSize()
                                                           .convertToLong());
        prologue.add(new GlobalAllocation(new IrGlobalArray(array.getId()
                                                                 .getLabel(),
                                                            fieldDeclaration.getType()),
            size,
            fieldDeclaration.getType(),
            array,
            "# " + array.getSourceCode()));
      }
    }
    globalNames.addAll(prologue.stream()
                               .flatMap(instruction -> instruction.getAllValues()
                                                                  .stream())
                               .filter(abstractName -> abstractName instanceof IrGlobal)
                               .map(abstractName -> (IrGlobal) abstractName)
                               .collect(Collectors.toUnmodifiableSet()));

    for (String stringLiteral : findAllStringLiterals(program)) {
      final var stringConstant = new IrStringConstant(stringLiteral);
      prologue.add(new StringConstantAllocation(stringConstant));
      stringConstantsMap.put(stringLiteral,
          stringConstant);
    }
    return prologue;
  }

  private Set<String> findAllStringLiterals(Program program) {
    var literalList = new HashSet<String>();
    var toExplore = new Stack<AST>();

    toExplore.addAll(program.methodDefinitionList);
    while (!toExplore.isEmpty()) {
      final AST node = toExplore.pop();
      if (node instanceof StringLiteral) literalList.add(((StringLiteral) node).literal);
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
    if (visitedBasicBlocks.contains(basicBlockBranchLess)) return basicBlockBranchLess.getInstructionList();
    visitedBasicBlocks.add(basicBlockBranchLess);
    InstructionList instructionList = new InstructionList();
    for (var line : basicBlockBranchLess.getAstNodes())
      instructionList.addAll(line.accept(currentAstToInstructionListConverter,
          null));
    assert basicBlockBranchLess.getSuccessor() != null;
    visit(basicBlockBranchLess.getSuccessor());
    basicBlockBranchLess.setInstructionList(instructionList);
    return instructionList;
  }


  public InstructionList visitBasicBlockWithBranch(BasicBlock basicBlockWithBranch) {
    if (visitedBasicBlocks.contains(basicBlockWithBranch)) return basicBlockWithBranch.getInstructionList();

    visitedBasicBlocks.add(basicBlockWithBranch);

    var condition = basicBlockWithBranch.getBranchCondition()
                                        .orElseThrow();
    var conditionInstructionList = condition.accept(currentAstToInstructionListConverter,
        IrRegister.gen(Type.Bool));

    visit(basicBlockWithBranch.getTrueTarget());
    visit(basicBlockWithBranch.getFalseTarget());

    var branchCondition = new ConditionalBranch(conditionInstructionList.getPlace(),
                                                basicBlockWithBranch.getFalseTarget(),
                                                condition,
                                                "if !(" + basicBlockWithBranch.getBranchCondition()
                                      .orElseThrow()
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

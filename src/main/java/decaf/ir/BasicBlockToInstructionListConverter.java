package decaf.ir;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.Type;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.cfg.ControlFlowGraph;
import decaf.ir.cfg.NOP;
import decaf.ir.cfg.SymbolTableFlattener;
import decaf.ir.codes.ConditionalBranch;
import decaf.ir.codes.CopyInstruction;
import decaf.ir.codes.GlobalAllocation;
import decaf.ir.codes.Instruction;
import decaf.ir.codes.Method;
import decaf.ir.codes.MethodEnd;
import decaf.ir.codes.StringConstantAllocation;
import decaf.ir.names.IrGlobalArray;
import decaf.ir.names.IrGlobalScalar;
import decaf.ir.names.IrIntegerConstant;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrStringConstant;
import decaf.ir.names.IrValue;
import decaf.ir.names.IrValuePredicates;
import decaf.shared.Pair;
import decaf.shared.ProgramIr;
import decaf.shared.env.Scope;

public class BasicBlockToInstructionListConverter {
  private final Set<IrValue> globalNames = new HashSet<>();
  private final Set<BasicBlock> visitedBasicBlocks = new HashSet<>();
  private final HashMap<String, Scope> perMethodSymbolTables;
  private final ProgramIr programIr;
  private final HashMap<String, IrStringConstant> stringConstantsMap = new HashMap<>();

  private AstToInstructionListConverter currentAstToInstructionListConverter;
  private NOP currentMethodExitNop;

  public BasicBlockToInstructionListConverter(
      ControlFlowGraph controlFlowGraph
  ) {
    var symbolTableFlattener = new SymbolTableFlattener(controlFlowGraph.getGlobalDescriptor());
    perMethodSymbolTables = symbolTableFlattener.createCFGSymbolTables();
    var prologue = genPrologue(controlFlowGraph.getProgram());
    var methods = new ArrayList<Method>();
    controlFlowGraph.getMethodNameToEntryBlockMapping()
                    .forEach((methodName, entryBlock) -> methods.add(generateMethodInstructionList(
                        controlFlowGraph.getProgram()
                                        .getMethodDefinitions()
                                        .stream()
                                        .filter(methodDefinition -> methodDefinition.getMethodName()
                                                                                    .getLabel()
                                                                                    .equals(methodName))
                                        .findFirst()
                                        .orElseThrow(() -> new IllegalStateException(
                                            "expected to find method " + methodName)),
                        entryBlock.getSuccessor(),
                        perMethodSymbolTables.get(methodName)
                    )));
    this.programIr = new ProgramIr(
        prologue,
        methods
    );
    this.programIr.renumberLabels();
  }

  public HashMap<String, Scope> getPerMethodSymbolTables() {
    return perMethodSymbolTables;
  }

  public Set<IrValue> getGlobalNames() {
    return globalNames;
  }

  private Method generateMethodInstructionList(
      MethodDefinition methodDefinition,
      BasicBlock methodStart,
      Scope currentScope
  ) {
    currentAstToInstructionListConverter = new AstToInstructionListConverter(
        currentScope,
        stringConstantsMap,
        globalNames
    );

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
    entryBasicBlockInstructionList.add(
        0,
        method
    );

    var methodExitLabel = "exit_" + methodDefinition.getMethodName()
                                                    .getLabel();
    currentMethodExitNop.getInstructionList()
                        .setLabel(methodExitLabel);
    currentMethodExitNop.getInstructionList()
                        .add(new MethodEnd(methodDefinition));

    methodStart.setInstructionList(entryBasicBlockInstructionList);
    var instructions = new ArrayList<Instruction>();
    for (var local : ProgramIr.getLocals(method)) {
      if (local instanceof IrSsaRegister irSsaRegister) instructions.add(CopyInstruction.noMetaData(
          irSsaRegister.copy(),
          IrIntegerConstant.zero()
      ));
    }
    entryBasicBlockInstructionList.addAll(
        1,
        instructions
    );
    method.setUnoptimizedInstructionList(TraceScheduler.flattenIr(method));
    return method;
  }

  private InstructionList genPrologue(Program program) {
    var prologue = new InstructionList();
    for (var fieldDeclaration : program.getFieldDeclaration()) {
      for (var name : fieldDeclaration.vars) {
        prologue.add(new GlobalAllocation(
            new IrGlobalScalar(
                name.getLabel(),
                fieldDeclaration.getType()
            ),
            name,
            "# " + name.getSourceCode()
        ));
      }
      for (var array : fieldDeclaration.arrays) {
        var size = (fieldDeclaration.getType()
                                    .getFieldSize() * array.getSize()
                                                           .convertToLong());
        prologue.add(new GlobalAllocation(
            new IrGlobalArray(
                array.getLabel(),
                fieldDeclaration.getType(),
                size
            ),
            array,
            "# " + array.getSourceCode()
        ));
      }
    }
    globalNames.addAll(prologue.stream()
                               .flatMap(instruction -> instruction.genIrValuesFiltered(IrValuePredicates.isGlobal())
                                                                  .stream())
                               .collect(Collectors.toUnmodifiableSet()));

    for (var stringLiteral : findAllStringLiterals(program)) {
      final var stringConstant = new IrStringConstant(stringLiteral);
      prologue.add(new StringConstantAllocation(stringConstant));
      stringConstantsMap.put(
          stringLiteral,
          stringConstant
      );
    }
    return prologue;
  }

  private Set<String> findAllStringLiterals(Program program) {
    var literalList = new HashSet<String>();
    var toExplore = new Stack<AST>();

    toExplore.addAll(program.getMethodDefinitions());
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
      instructionList.addAll(line.accept(
          currentAstToInstructionListConverter,
          null
      ));
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
    var conditionInstructionList = condition.accept(
        currentAstToInstructionListConverter,
        IrSsaRegister.gen(Type.Bool)
    );

    visit(basicBlockWithBranch.getTrueTarget());
    visit(basicBlockWithBranch.getFalseTarget());

    var branchCondition = new ConditionalBranch(
        conditionInstructionList.getPlace(),
        basicBlockWithBranch.getFalseTarget(),
        condition,
        "if !(" + basicBlockWithBranch.getBranchCondition()
                                      .orElseThrow()
                                      .getSourceCode() + ")"
    );
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

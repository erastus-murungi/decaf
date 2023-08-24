package decaf.ir.cfg;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.AssignOpExpr;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.Break;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.Continue;
import decaf.analysis.syntax.ast.Decrement;
import decaf.analysis.syntax.ast.Expression;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.For;
import decaf.analysis.syntax.ast.If;
import decaf.analysis.syntax.ast.ImportDeclaration;
import decaf.analysis.syntax.ast.Increment;
import decaf.analysis.syntax.ast.LocationAssignExpr;
import decaf.analysis.syntax.ast.ActualArgument;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.MethodDefinition;
import decaf.analysis.syntax.ast.FormalArgument;
import decaf.analysis.syntax.ast.Program;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.Statement;
import decaf.analysis.syntax.ast.Type;
import decaf.analysis.syntax.ast.While;
import decaf.ir.dataflow.passes.BranchFoldingPass;
import decaf.shared.Utils;
import decaf.shared.env.TypingContext;
import decaf.shared.descriptors.MethodDescriptor;
import decaf.shared.env.Scope;
import decaf.shared.errors.SemanticError;

public class ControlFlowGraph {
  private final Program program;
  private final TypingContext typingContext;
  private final List<SemanticError> errors = new ArrayList<>();
  private final BasicBlock prologue = BasicBlock.noBranch();
  private final HashMap<String, NOP> methodNameToExitNop = new HashMap<>();

  private final Stack<List<BasicBlock>> loopToBreak = new Stack<>(); // a bunch of break blocks to point to the right place
  private final Stack<BasicBlock> continueBlocks = new Stack<>(); // a bunch of continue blocks to point to the right place

  private HashMap<String, BasicBlock> methodNameToEntryBlock = new HashMap<>();
  private NOP exitNop;

  public ControlFlowGraph(
      Program program,
      TypingContext typingContext
  ) {
    this.program = program;
    this.typingContext = typingContext;
  }

  public static void removeNOPs(
      BasicBlock basicBlock,
      NOP methodExitNop
  ) {
    var seen = new HashSet<BasicBlock>();
    visit(
        basicBlock,
        seen,
        methodExitNop
    );
  }

  public static void visit(
      BasicBlock basicBlock,
      Set<BasicBlock> seen,
      NOP methodExitNop
  ) {
    switch (basicBlock.getBasicBlockType()) {
      case NO_BRANCH -> visitBasicBlockBranchLess(
          basicBlock,
          seen,
          methodExitNop
      );
      case BRANCH -> visitBasicBlockWithBranch(
          basicBlock,
          seen,
          methodExitNop
      );
      default -> visitNOP(
          (NOP) basicBlock,
          seen,
          methodExitNop
      );
    }
  }


  /**
   * @param basicBlockBranchLess The branch-less basic block to visit
   * @param seen                 the set of basic blocks that have been seen
   *                             (to prevent infinite recursion)
   * @param methodExitNop        the NOP that represents the end of the method
   *                             (not used by this method, but used by others)
   */

  public static void visitBasicBlockBranchLess(
      BasicBlock basicBlockBranchLess,
      Set<BasicBlock> seen,
      NOP methodExitNop
  ) {
    if (!seen.contains(basicBlockBranchLess)) {
      seen.add(basicBlockBranchLess);
      if (basicBlockBranchLess.getSuccessor() != null) {
        visit(
            basicBlockBranchLess.getSuccessor(),
            seen,
            methodExitNop
        );
      }
    }
  }

  public static void visitBasicBlockWithBranch(
      BasicBlock basicBlockWithBranch,
      Set<BasicBlock> seen,
      NOP methodExitNop
  ) {
    if (!seen.contains(basicBlockWithBranch)) {
      seen.add(basicBlockWithBranch);
      if (basicBlockWithBranch.getTrueTarget() != null) {
        visit(
            basicBlockWithBranch.getTrueTarget(),
            seen,
            methodExitNop
        );
      }
      if (basicBlockWithBranch.getFalseTarget() != null) {
        visit(
            basicBlockWithBranch.getFalseTarget(),
            seen,
            methodExitNop
        );
      }
    }
  }

  public static void visitNOP(
      NOP nop,
      Set<BasicBlock> seen,
      NOP methodExitNop
  ) {
    if (!seen.contains(nop)) {
      List<BasicBlock> parentsCopy = new ArrayList<>(nop.getPredecessors());
      seen.add(nop);
      BasicBlock endBlock;
      if (nop == methodExitNop) {
        nop.setSuccessor(null);
        return;
      }
      if (nop.getSuccessor() != null) {
        visit(
            nop.getSuccessor(),
            seen,
            methodExitNop
        );
        endBlock = nop.getSuccessor();
      } else {
        endBlock = methodExitNop;
      }
      for (BasicBlock parent : parentsCopy) {
        // connecting parents to child
        if (parent.hasBranch()) {
          if (parent.getTrueTarget() == nop) {
            parent.setTrueTarget(endBlock);
          } else if (parent.getFalseTarget() == nop) {
            parent.setFalseTargetUnchecked(endBlock);
          }
        } else {
          if (parent.getSuccessor() == nop) {
            parent.setSuccessor(endBlock);
          }
        }
        endBlock.removePredecessor(nop);
        endBlock.addPredecessor(parent);
      }
    }
  }

  private static String getOpString(LocationAssignExpr locationAssignExpr) {
    String op;
    if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
      op = assignOpExpr.assignOp.label;
    } else if (locationAssignExpr.assignExpr instanceof final CompoundAssignOpExpr assignOpExpr) {
      op = assignOpExpr.compoundAssignOp.label;
    } else if (locationAssignExpr.assignExpr instanceof Decrement) {
      op = Scanner.DECREMENT;
    } else if (locationAssignExpr.assignExpr instanceof Increment) {
      op = Scanner.INCREMENT;
    } else {
      throw new IllegalStateException("unrecognized AST node " + locationAssignExpr.assignExpr);
    }
    return op;
  }

  public HashMap<String, BasicBlock> getMethodNameToEntryBlockMapping() {
    return methodNameToEntryBlock;
  }

  public TypingContext getGlobalDescriptor() {
    return typingContext;
  }

  public Program getProgram() {
    return program;
  }

  public BasicBlock getPrologueBasicBlock() {
    return prologue;
  }

  private List<BasicBlock> getReturningPaths() {
    return exitNop.getPredecessors()
                  .stream()
                  .filter(cfgBlock -> (!cfgBlock.getAstNodes()
                                                .isEmpty() && (cfgBlock.lastAstLine() instanceof Return)))
                  .toList();
  }

  private List<BasicBlock> getNonReturningPaths() {
    return exitNop.getPredecessors()
                  .stream()
                  .filter(basicBlock -> (basicBlock.getAstNodes()
                                                   .isEmpty() || (!(basicBlock.lastAstLine() instanceof Return))))
                  .toList();
  }

  private void correctExitNopPredecessors() {
    exitNop.getPredecessors()
           .removeIf(block -> !(block.getSuccessors()
                                     .contains(exitNop)));
  }

  private void catchFalloutError(MethodDescriptor methodDescriptor) {
    var methodDefinition = methodDescriptor.methodDefinition;
    if (methodDescriptor.typeIs(Type.Void)) return;

    correctExitNopPredecessors();
    var returningPaths = getReturningPaths();
    var nonReturningPaths = getNonReturningPaths();

    if (returningPaths.size() != nonReturningPaths.size()) {
      errors.addAll(nonReturningPaths.stream()
                                     .map(basicBlock -> new SemanticError(
                                         methodDefinition.getTokenPosition(),
                                         SemanticError.SemanticErrorType.MISSING_RETURN_STATEMENT,
                                         methodDefinition.getMethodName()
                                                         .getLabel() + "'s execution path ends with" +
                                             (basicBlock.getAstNodes()
                                                        .isEmpty() ? "": (basicBlock.lastAstLine()
                                                                                    .getSourceCode())) +
                                             " instead of a return statement"
                                     ))
                                     .toList());
    }
    if (returningPaths.isEmpty()) {
      errors.add(new SemanticError(
          methodDefinition.getTokenPosition(),
          SemanticError.SemanticErrorType.MISMATCHING_RETURN_TYPE,
          methodDefinition.getMethodName()
                          .getLabel() + " method does not return expected type " +
              methodDefinition.getReturnType()
      ));
    }
  }

  public List<BasicBlock> getMethodEntryBlocks() {
    return List.copyOf(methodNameToEntryBlock.values());
  }

  public void build() {
    final MaximalBasicBlocksUtil maximalVisitor = new MaximalBasicBlocksUtil();

    visitProgram(
        program,
        typingContext.globalScope
    );


    methodNameToEntryBlock.forEach((k, v) -> removeNOPs(
        v.getSuccessor(),
        methodNameToExitNop.get(k)
    ));

    methodNameToEntryBlock.forEach((k, v) -> {
      maximalVisitor.setExitNOP(methodNameToExitNop.get(k));
      checkNotNull(v.getSuccessor());
      maximalVisitor.visit(v.getSuccessor());
      catchFalloutError((MethodDescriptor) typingContext.globalScope.lookup(k)
                                                                    .orElseThrow());
    });
    removeNOPs(
        prologue,
        (NOP) prologue.getSuccessor()
    );
    HashMap<String, BasicBlock> methodBlocksCFG = new HashMap<>();
    methodNameToEntryBlock.forEach((var k, var v) -> {
      if (v.getLinesOfCodeString()
           .isBlank()) {
        if (v.getSuccessor() != null) {
          v.getSuccessor()
           .removePredecessor(v);
          v = v.getSuccessor();
        }
      }
      methodBlocksCFG.put(
          k,
          v
      );
    });

    methodNameToEntryBlock = methodBlocksCFG;
    maximalVisitor.setExitNOP((NOP) prologue.getSuccessor());
    maximalVisitor.visit(prologue);
    BranchFoldingPass.run(methodBlocksCFG.values());
  }

  private BasicBlocksPair dispatch(
      AST ast,
      Scope scope
  ) {
    if (ast instanceof MethodDefinition methodDefinition) {
      return visitMethodDefinition(
          methodDefinition,
          scope
      );
    } else if (ast instanceof FieldDeclaration fieldDeclaration) {
      return visitFieldDeclaration(fieldDeclaration);
    } else if (ast instanceof ImportDeclaration importDeclaration) {
      return visitImportDeclaration(importDeclaration);
    } else if (ast instanceof For forStatement) {
      return visitFor(
          forStatement,
          scope
      );
    } else if (ast instanceof While whileLoop) {
      return visitWhile(
          whileLoop,
          scope
      );
    } else if (ast instanceof FormalArgument formalArgument) {
      return visitMethodDefinitionParameter(formalArgument);
    } else if (ast instanceof Block block) {
      return visitBlock(
          block,
          scope
      );
    } else if (ast instanceof Return returnStatement) {
      return visitReturn(returnStatement);
    } else if (ast instanceof If ifStatement) {
      return visitIf(
          ifStatement,
          scope
      );
    } else if (ast instanceof MethodCallStatement methodCallStatement) {
      return visitMethodCallStatement(methodCallStatement);
    } else if (ast instanceof LocationAssignExpr locationAssignExpr) {
      return visitLocationAssignExpr(locationAssignExpr);
    } else {
      throw new IllegalStateException(ast.getClass()
                                         .getSimpleName());
    }
  }

  public BasicBlocksPair visitFieldDeclaration(FieldDeclaration fieldDeclaration) {
    // multiple fields can be declared in same line, handle/flatten later
    BasicBlock fieldDecl = BasicBlock.noBranch();
    fieldDecl.getAstNodes()
             .add(fieldDeclaration);
    return new BasicBlocksPair(
        fieldDecl,
        fieldDecl
    );
  }

  public BasicBlocksPair visitMethodDefinition(
      MethodDefinition methodDefinition,
      Scope scope
  ) {
    var methodEntryNop = new NOP(
        methodDefinition.getMethodName()
                        .getLabel(),
        NOP.NOPType.METHOD_ENTRY
    );
    var currentPair = new BasicBlocksPair(
        methodEntryNop,
        new NOP()
    );
    methodEntryNop.setSuccessor(currentPair.endBlock);
    currentPair.startBlock.setSuccessor(currentPair.endBlock);
    for (var param : methodDefinition.getFormalArguments()) {
      var placeholder = dispatch(
          param,
          scope
      );
      currentPair.endBlock.setSuccessor(placeholder.startBlock);
      placeholder.startBlock.addPredecessor(currentPair.endBlock);
      currentPair = placeholder;
    }
    var methodBody = dispatch(
        methodDefinition.getBody(),
        scope
    );
    currentPair.endBlock.setSuccessor(methodBody.startBlock);
    methodBody.startBlock.addPredecessor(currentPair.endBlock);
    return new BasicBlocksPair(
        methodEntryNop,
        methodBody.endBlock
    );
  }

  public BasicBlocksPair visitImportDeclaration(ImportDeclaration importDeclaration) {
    var importDeclarationBlock = BasicBlock.noBranch();
    importDeclarationBlock.addAstNode(importDeclaration);
    return new BasicBlocksPair(
        importDeclarationBlock,
        importDeclarationBlock
    );
  }

  public BasicBlocksPair visitFor(
      For forStatement,
      Scope scope
  ) {
    loopToBreak.push(new ArrayList<>());
    // If false, end with NOP, also end of for_statement
    NOP falseBlock = new NOP(
        "For Loop (false) " + forStatement.terminatingCondition.getSourceCode(),
        NOP.NOPType.NORMAL
    );
    NOP exit = new NOP(
        "exit_for",
        NOP.NOPType.NORMAL
    );
    falseBlock.setSuccessor(exit);
    exit.addPredecessor(falseBlock);

    // For the block, the child of that CFGBlock should be a block with the increment line
    BasicBlock incrementBlock = BasicBlock.noBranch();
    incrementBlock.addAstNode(forStatement.update);

    // Evaluate the condition
    final Expression condition = Utils.rotateBinaryOpExpression(forStatement.terminatingCondition);
    var evaluateBlock = ShortCircuitProcessor.shortCircuit(BasicBlock.branch(
        condition,
        exitNop,
        exitNop
    ));
    incrementBlock.setSuccessor(evaluateBlock);
    evaluateBlock.addPredecessor(incrementBlock);

    // In for loops, continue should point to an incrementBlock
    continueBlocks.push(incrementBlock);

    // If true, run the block.
    BasicBlocksPair truePair = dispatch(
        forStatement.block,
        scope
    );

    evaluateBlock.setFalseTargetUnchecked(falseBlock);
    evaluateBlock.getFalseTarget()
                 .addPredecessor(evaluateBlock);

    evaluateBlock.setTrueTarget(truePair.startBlock);
    truePair.startBlock.addPredecessor(evaluateBlock);

    if (truePair.endBlock != exitNop) {
      truePair.endBlock.setSuccessor(incrementBlock);
      incrementBlock.addPredecessor(truePair.endBlock);
    }
    // Initialize the condition irAssignableValue
    BasicBlock initializeBlock = BasicBlock.noBranch();
    initializeBlock.addAstNode(forStatement.initialization);

    // child of initialization block is evaluation
    initializeBlock.setSuccessor(evaluateBlock);
    evaluateBlock.addPredecessor(initializeBlock);

    // Child of that increment block should be the evaluation
    incrementBlock.setSuccessor(evaluateBlock);
    evaluateBlock.addPredecessor(incrementBlock);

    handleBreaksInLoops(falseBlock);
    continueBlocks.pop();
    return new BasicBlocksPair(
        initializeBlock,
        exit,
        false
    );
  }

  private void handleBreaksInLoops(BasicBlock cfgBlock) {
    List<BasicBlock> toRemove = new ArrayList<>();
    List<BasicBlock> breakBlocks = loopToBreak.pop();
    if (!breakBlocks.isEmpty()) {
      for (BasicBlock breakBlock : breakBlocks) {
        breakBlock.setSuccessor(cfgBlock);
        toRemove.add(breakBlock);
        cfgBlock.addPredecessor(breakBlock);
      }
    }
    for (BasicBlock breakBlock : toRemove)
      breakBlocks.remove(breakBlock);
  }

  public BasicBlocksPair visitWhile(
      While whileStatement,
      Scope scope
  ) {
    loopToBreak.push(new ArrayList<>());
    // If false, end with NOP, also end of while
    NOP falseBlock = new NOP();

    // Evaluate the condition
    Expression test = Utils.rotateBinaryOpExpression(whileStatement.test);
    BasicBlock conditionExpr = BasicBlock.branch(
        test,
        exitNop,
        falseBlock
    );
    falseBlock.addPredecessor(conditionExpr);

    // In for loops, continue should point to the evaluation expression
    continueBlocks.push(conditionExpr);

    // If true, run the block.
    var truePair = dispatch(
        whileStatement.body,
        scope
    );

    conditionExpr.setTrueTarget(truePair.startBlock);
    conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
    if (truePair.endBlock != null) {
      truePair.endBlock.setSuccessor(conditionExpr);
      conditionExpr.addPredecessor(truePair.endBlock);
    }

    handleBreaksInLoops(falseBlock);
    continueBlocks.pop();
    return new BasicBlocksPair(
        conditionExpr,
        falseBlock
    );
  }

  public void visitProgram(
      Program program,
      Scope scope
  ) {
    var curPair = new BasicBlocksPair(
        prologue,
        new NOP(
            "global NOP",
            NOP.NOPType.NORMAL
        )
    );
    prologue.setSuccessor(curPair.endBlock);
    for (var import_ : program.getImportDeclaration()) {
      BasicBlocksPair placeholder = dispatch(
          import_,
          scope
      );
      curPair.endBlock.setSuccessor(placeholder.startBlock);
      placeholder.startBlock.addPredecessor(curPair.endBlock);
      curPair = placeholder;
    }
    for (var field : program.getFieldDeclaration()) {
      BasicBlocksPair placeholder = dispatch(
          field,
          scope
      );
      curPair.endBlock.setSuccessor(placeholder.startBlock);
      placeholder.startBlock.addPredecessor(curPair.endBlock);
      curPair = placeholder;
    }
    for (var method : program.getMethodDefinitions()) {
      exitNop = new NOP(
          method.getMethodName()
                .getLabel(),
          NOP.NOPType.METHOD_EXIT
      );
      methodNameToEntryBlock.put(
          method.getMethodName()
                .getLabel(),
          dispatch(
              method,
              scope
          ).startBlock
      );
      methodNameToExitNop.put(
          method.getMethodName()
                .getLabel(),
          exitNop
      );
    }
  }

  public BasicBlocksPair visitBlock(
      Block block,
      Scope scope
  ) {
    NOP initial = new NOP();
    NOP exit = new NOP();
    BasicBlocksPair curPair = new BasicBlocksPair(
        initial,
        new NOP()
    );
    initial.setSuccessor(curPair.endBlock);

    for (FieldDeclaration field : block.getFieldDeclarations()) {
      BasicBlocksPair placeholder = dispatch(
          field,
          scope
      );
      curPair.endBlock.setSuccessor(placeholder.startBlock);
      placeholder.startBlock.addPredecessor(curPair.endBlock);
      curPair = placeholder;
    }

    for (Statement statement : block.getStatements()) {
      if (statement instanceof Continue) {
        // will return a NOP() for sure because Continue blocks should be pointers back to the evaluation block
        BasicBlock continueCfg = new NOP();
        BasicBlock nextBlock = continueBlocks.peek();
        continueCfg.setSuccessor(nextBlock);
        nextBlock.addPredecessor(continueCfg);
        continueCfg.addPredecessor(curPair.endBlock);
        curPair.endBlock.setSuccessor(continueCfg);
        return new BasicBlocksPair(
            initial,
            continueCfg
        );
      }
      if (statement instanceof Break) {
        // a break is not a real block either
        BasicBlock breakCfg = new NOP(
            "Break",
            NOP.NOPType.NORMAL
        );
        loopToBreak.peek()
                   .add(breakCfg);
        breakCfg.addPredecessor(curPair.endBlock);
        curPair.endBlock.setSuccessor(breakCfg);
        return new BasicBlocksPair(
            initial,
            breakCfg,
            false
        );
      }
      if (statement instanceof Return returnStatement) {
        BasicBlocksPair returnPair = dispatch(
            returnStatement,
            scope
        );
        curPair.endBlock.setSuccessor(returnPair.startBlock);
        returnPair.startBlock.addPredecessor(curPair.endBlock);
        return new BasicBlocksPair(
            initial,
            returnPair.endBlock,
            false
        );
      }
      // recurse normally for other cases
      else {
        BasicBlocksPair placeholder = dispatch(
            statement,
            scope
        );
        curPair.endBlock.setSuccessor(placeholder.startBlock);
        placeholder.startBlock.addPredecessor(curPair.endBlock);
        curPair = placeholder;
      }
    }
    curPair.endBlock.setSuccessor(exit);
    exit.addPredecessor(curPair.endBlock);
    return new BasicBlocksPair(
        initial,
        exit,
        false
    );
  }

  public BasicBlocksPair visitIf(
      If ifStatement,
      Scope scope
  ) {
    // always end with nop
    final NOP exit = new NOP();

    // If true, run the block.
    BasicBlocksPair truePair = dispatch(
        ifStatement.ifBlock,
        scope
    );
    if (truePair.endBlock.getSuccessor() == null) {
      // handling the cases when we have a "Continue" statement
      truePair.endBlock.setSuccessor(exit);
      exit.addPredecessor(truePair.endBlock);
    }

    // Evaluate the condition
    final Expression condition = Utils.rotateBinaryOpExpression(ifStatement.test);

    BasicBlock conditionExpr;
    if (ifStatement.elseBlock != null) {
      BasicBlocksPair falsePair = dispatch(
          ifStatement.elseBlock,
          scope
      );
      if (falsePair.endBlock.getSuccessor() == null) {
        // handling the cases when we have a "Continue" statement
        falsePair.endBlock.setSuccessor(exit);
        exit.addPredecessor(falsePair.endBlock);
      }
      conditionExpr = BasicBlock.branch(
          condition,
          truePair.startBlock,
          falsePair.startBlock
      );
      conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
      falsePair.startBlock.addPredecessor(conditionExpr);
    } else {
      conditionExpr = BasicBlock.branch(
          condition,
          truePair.startBlock,
          exit
      );
      conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
    }
    truePair.startBlock.addPredecessor(conditionExpr);
    return new BasicBlocksPair(
        ShortCircuitProcessor.shortCircuit(conditionExpr),
        exit
    );
  }

  public BasicBlocksPair visitReturn(Return returnStatement) {
    BasicBlock returnBlock = BasicBlock.noBranch();
    returnStatement.retExpression = Utils.rotateBinaryOpExpression(returnStatement.retExpression);
    returnBlock.addAstNode(returnStatement);
    returnBlock.setSuccessor(exitNop);
    return new BasicBlocksPair(
        returnBlock,
        exitNop
    );
  }

  public BasicBlocksPair visitMethodCallStatement(MethodCallStatement methodCallStatement) {
    BasicBlock methodCallExpr = BasicBlock.noBranch();
    for (int i = 0; i < methodCallStatement.methodCall.actualArgumentList.size(); i++) {
      ActualArgument param = methodCallStatement.methodCall.actualArgumentList.get(i);
      if (param instanceof ExpressionParameter expressionParameter) {
        expressionParameter.expression = Utils.rotateBinaryOpExpression(expressionParameter.expression);
        methodCallStatement.methodCall.actualArgumentList.set(
            i,
            param
        );
      }
    }
    methodCallExpr.addAstNode(methodCallStatement);
    return new BasicBlocksPair(
        methodCallExpr,
        methodCallExpr
    );
  }

  public BasicBlocksPair visitLocationAssignExpr(LocationAssignExpr locationAssignExpr) {
    final var assignment = BasicBlock.noBranch();
    locationAssignExpr.assignExpr.expression = Utils.rotateBinaryOpExpression(locationAssignExpr.assignExpr.expression);

    String op = getOpString(locationAssignExpr);

    assignment.addAstNode(new Assignment(
        locationAssignExpr.location,
        locationAssignExpr.assignExpr,
        op
    ));
    return new BasicBlocksPair(
        assignment,
        assignment
    );
  }

  public BasicBlocksPair visitMethodDefinitionParameter(FormalArgument formalArgument) {
    var methodParam = BasicBlock.noBranch();
    methodParam.setSuccessor(methodParam);
    methodParam.addAstNode(formalArgument);
    return new BasicBlocksPair(
        methodParam,
        methodParam
    );
  }

}

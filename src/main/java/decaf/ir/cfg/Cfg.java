package decaf.ir.cfg;

import decaf.analysis.syntax.ast.*;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

public class Cfg {
    @NotNull
    private final CompilationContext context;
    @NotNull
    private final CfgBlock entryBlock;
    @NotNull
    private final String methodName;
    @Nullable
    private CfgBlock breakJumpTarget;
    @Nullable
    private CfgBlock continueJumpTarget;

    private Cfg(@NotNull MethodDefinition methodDefinition, @NotNull CompilationContext context) {
        this.context = context;
        entryBlock = CfgBlock.empty();
        methodName = methodDefinition.getName();
        context.addEntryCfgBlockFor(methodDefinition.getName(), entryBlock);
        visitMethodDefinition(methodDefinition, entryBlock);
        context.addExitCfgBlockFor(methodDefinition.getName(), getExitBlock());
        validateCfg();
    }

    private @NotNull CfgBlock visit(@NotNull AST ast, @NotNull CfgBlock currentBlock) {
        if (ast instanceof Statement statement) {
            if (statement instanceof If ifStatement) {
                return visitIfStatement(ifStatement, currentBlock);
            } else if (statement instanceof For forStatement) {
                return visitForStatement(forStatement, currentBlock);
            } else if (statement instanceof While whileStatement) {
                return visitWhileStatement(whileStatement, currentBlock);
            } else if (statement instanceof Return returnStatement) {
                return visitReturnStatement(returnStatement, currentBlock);
            } else if (statement instanceof Continue) {
                return visitContinueStatement(currentBlock);
            } else if (statement instanceof Break) {
                return visitBreakStatement(currentBlock);
            } else if (statement instanceof Block block) {
                return visitBlockStatement(block, currentBlock);
            } else {
                return currentBlock.addUserToEnd(statement);
            }
        } else {
            throw new IllegalStateException("Cannot visit non-statement AST node");
        }
    }

    public static @NotNull CfgBlock createGlobalCfgBlock(@NotNull Program program) {
        var globalCfgBlock = CfgBlock.empty();
        for (var fieldDeclaration : program.getFieldDeclarations()) {
            globalCfgBlock.addUserToEnd(fieldDeclaration);
        }
        for (var methodDefinition : program.getMethodDefinitions()) {
            globalCfgBlock.addUserToEnd(methodDefinition);
        }
        return globalCfgBlock;
    }

    public static void build(@NotNull CompilationContext compilationContext) {
        var globalBlock = createGlobalCfgBlock(compilationContext.getProgram());
        compilationContext.setGlobalEntryBlock(globalBlock);
        for (var methodDefinition : compilationContext.getProgram().getMethodDefinitions()) {
            var cfg = new Cfg(methodDefinition, compilationContext);
            compilationContext.setCfg(methodDefinition.getName(), cfg);
            globalBlock.addSuccessor(cfg.getEntryBlock());
        }
    }

    public @NotNull CfgBlock getEntryBlock() {
        return entryBlock;
    }

    public void visitMethodDefinition(@NotNull MethodDefinition methodDefinition, @NotNull CfgBlock currentBlock) {
        currentBlock.addUserToEnd(methodDefinition.getFormalArguments());
        visitBlockStatement(methodDefinition.getBody(), currentBlock);
    }

    public @NotNull CfgBlock visitForStatement(@NotNull For forStatement, @NotNull CfgBlock currentBlock) {
        var loopSuccessor = CfgBlock.empty();
        var forBodyCfgBlock = CfgBlock.empty();

        var saveTargets = new SaveTargets(loopSuccessor, forBodyCfgBlock);
        var bodyExit = visitBlockStatement(forStatement.getBody(), forBodyCfgBlock);

        bodyExit.addUserToEnd(forStatement.getUpdate());
        bodyExit.addSuccessor(currentBlock);

        currentBlock.addUserToEnd(forStatement.getInitialization());
        currentBlock.addUserToEnd(forStatement.getTerminatingCondition().toEvalCondition());
        currentBlock.addBranchTargets(forBodyCfgBlock, loopSuccessor);

        saveTargets.restore();

        return loopSuccessor;
    }

    public @NotNull CfgBlock visitBreakStatement(@NotNull CfgBlock currentBlock) {
        // no need to add break as a user of the current block
        if (breakJumpTarget == null) {
            throw new MalformedSourceLevelCfg("break statement outside of a loop");
        } else {
            currentBlock.addSuccessor(breakJumpTarget);
            return CfgBlock.empty();
        }
    }

    public @NotNull CfgBlock visitContinueStatement(@NotNull CfgBlock currentBlock) {
        // no need to add continue as a user of the current block
        if (continueJumpTarget == null) {
            throw new MalformedSourceLevelCfg("continue statement outside of a loop");
        } else {
            currentBlock.addSuccessor(continueJumpTarget);
            return CfgBlock.empty();
        }
    }

    public @NotNull CfgBlock visitWhileStatement(@NotNull While whileStatement, @NotNull CfgBlock currentBlock) {
        var loopSuccessor = CfgBlock.empty();
        var whileBodyCfgBlock = CfgBlock.empty();

        // save the current break and continue targets
        var saveTargets = new SaveTargets(loopSuccessor, whileBodyCfgBlock);

        var bodyExit = visitBlockStatement(whileStatement.getBody(), whileBodyCfgBlock);
        bodyExit.addSuccessor(currentBlock);

        currentBlock.addUserToEnd(whileStatement.getTest().toEvalCondition());
        currentBlock.addBranchTargets(whileBodyCfgBlock, loopSuccessor);

        saveTargets.restore();

        return loopSuccessor;
    }

    public @NotNull CfgBlock visitBlockStatement(@NotNull Block block, @NotNull CfgBlock currentBlock) {
        currentBlock.addUsers(block.getFieldDeclarations());
        var lastBlock = currentBlock;
        for (var statement : block.getStatements()) {
            lastBlock = visit(statement, lastBlock);
        }
        return lastBlock;
    }

    public @NotNull CfgBlock visitIfStatement(@NotNull If ifStatement, @NotNull CfgBlock currentBlock) {
        // The block we were processing is now finished.
        // Make it the successor block.
        // Process the false branch.

        var nextBlock = CfgBlock.empty();
        if (ifStatement.getElseBlock().isPresent()) {
            // Create a new block for the false branch.
            // Process the false branch.
            var elseBlockEntry = CfgBlock.empty();
            var thenBlockEntry = CfgBlock.empty();

            var elseBlockExit = visit(ifStatement.getElseBlock().get(), elseBlockEntry);
            var thenBlockExit = visit(ifStatement.getThenBlock(), thenBlockEntry);

            thenBlockExit.addSuccessor(nextBlock);
            elseBlockExit.addSuccessor(nextBlock);

            currentBlock.addBranchTargets(thenBlockEntry, elseBlockEntry);
        } else {
            var thenBlockEntry = CfgBlock.empty();
            var thenBlockExit = visit(ifStatement.getThenBlock(), thenBlockEntry);

            thenBlockExit.addSuccessor(nextBlock);

            currentBlock.addBranchTargets(thenBlockEntry, nextBlock);
        }
        // TOD0: add short-circuiting for && and ||
        currentBlock.addUserToEnd(ifStatement.getCondition().toEvalCondition());
        return nextBlock;
    }

    private @NotNull CfgBlock getExitBlock() {
        var exitBlocks = new ArrayList<CfgBlock>();
        var workList = new Stack<CfgBlock>();
        var seen = new HashSet<CfgBlock>();
        workList.push(entryBlock);
        while (!workList.isEmpty()) {
            var current = workList.pop();
            if (seen.contains(current)) {
                continue;
            } else {
                seen.add(current);
            }
            if (current.getSuccessors().isEmpty()) {
                exitBlocks.add(current);
            } else {
                workList.addAll(current.getSuccessors());
            }
        }
        if (exitBlocks.isEmpty()) {
            throw new MalformedSourceLevelCfg("no exit block found");
        } else if (exitBlocks.size() > 1) {
            // merge the exit blocks
            var exitBlock = CfgBlock.empty();
            for (var block : exitBlocks) {
                block.addSuccessor(exitBlock);
            }
            return exitBlock;
        } else {
            return exitBlocks.get(0);
        }
    }

    public @NotNull CfgBlock visitReturnStatement(@NotNull Return returnStatement, @NotNull CfgBlock currentBlock) {
        currentBlock.addUserToEnd(returnStatement);
        return CfgBlock.empty();
    }

    private class SaveTargets {
        @Nullable
        private final CfgBlock savedBreakJumpTarget;
        @Nullable
        private final CfgBlock savedContinueJumpTarget;

        public SaveTargets(@Nullable CfgBlock newBreakJumpTarget, @Nullable CfgBlock newContinueJumpTarget) {
            savedBreakJumpTarget = breakJumpTarget;
            savedContinueJumpTarget = continueJumpTarget;
            breakJumpTarget = newBreakJumpTarget;
            continueJumpTarget = newContinueJumpTarget;
        }

        public void restore() {
            breakJumpTarget = savedBreakJumpTarget;
            continueJumpTarget = savedContinueJumpTarget;
        }
    }

    public void validateCfgBlock(@NotNull CfgBlock cfgBlock) {
        checkState(cfgBlock.getSuccessors().size() <= 2, "a block cannot have more than two successors");
        // if this is a branch, then it must have two successors and the last instruction must be an eval condition
        if (cfgBlock.hasBranch()) {
            checkState(cfgBlock.getTerminator().isPresent(), "branch block must have a terminator");
            checkState(cfgBlock.getTerminator().get() instanceof Branch, "branch block must have a branch as its last instruction");
            checkState(cfgBlock.getSuccessors().size() == 2, "branch block must have two successors");
        }
        else {
            if (cfgBlock.getSuccessors().isEmpty()) {
                checkState(context.getExitCfgBlock(methodName).orElseThrow() == cfgBlock, "non-branch block must be the exit block");
            } else {
                checkState(!cfgBlock.getSuccessors().isEmpty(), "a non-branching with no successors must be the exit block");
                checkState(cfgBlock.getAlternateSuccessor().isEmpty(),
                           "non-branch block cannot have an alternate successor"
                          );
            }
        }
    }

    private void validateCfg() {
        var seen = new HashSet<CfgBlock>();
        var workList = new Stack<CfgBlock>();
        workList.add(entryBlock);
        while (!workList.isEmpty()) {
            var current = workList.pop();
            if (seen.contains(current)) {
                continue;
            } else {
                seen.add(current);
            }
            validateCfgBlock(current);
            workList.addAll(current.getSuccessors());
        }
    }

    private void removeEmptyBlock(@NotNull CfgBlock emptyCfgBlock) {
        checkArgument(emptyCfgBlock.isEmpty(), "Cannot remove non-empty block");
        for (var predecessor : emptyCfgBlock.getPredecessors()) {
            predecessor.removeSuccessor(emptyCfgBlock);
        }
    }

    public void pruneEmptyBasicBlocks() {

    }
}

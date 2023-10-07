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
    private final String methodName;
    @Nullable
    private CfgBlock breakJumpTarget;
    @Nullable
    private CfgBlock continueJumpTarget;

    private Cfg(@NotNull MethodDefinition methodDefinition, @NotNull CompilationContext compilationContext) {
        context = compilationContext;
        methodName = methodDefinition.getName();
        final var entryBlock = CfgBlock.createEntryBlock(methodName, compilationContext);
        visitMethodDefinition(methodDefinition, entryBlock);
        context.addExitCfgBlockFor(methodDefinition.getName(), findExitBlock());
        cleanUpAndValidateCfg();
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
        compilationContext.setGlobalEntryBlock(createGlobalCfgBlock(compilationContext.getProgram()));
        for (var methodDefinition : compilationContext.getProgram().getMethodDefinitions()) {
            var cfg = new Cfg(methodDefinition, compilationContext);
            compilationContext.setCfg(methodDefinition.getName(), cfg);
        }
    }

    private void cleanUpAndValidateCfg() {
        pruneEmptyBasicBlocks();
        validateCfg();
    }

    private @NotNull CfgBlock visit(@NotNull CfgBlock currentBlock, @NotNull AST ast) {
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
        bodyExit.linkToSuccessor(currentBlock);

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
            currentBlock.linkToSuccessor(breakJumpTarget);
            return CfgBlock.empty();
        }
    }

    public @NotNull CfgBlock visitContinueStatement(@NotNull CfgBlock currentBlock) {
        // no need to add continue as a user of the current block
        if (continueJumpTarget == null) {
            throw new MalformedSourceLevelCfg("continue statement outside of a loop");
        } else {
            currentBlock.linkToSuccessor(continueJumpTarget);
            return CfgBlock.empty();
        }
    }

    public @NotNull CfgBlock visitWhileStatement(@NotNull While whileStatement, @NotNull CfgBlock currentBlock) {
        var loopSuccessor = CfgBlock.empty();
        var whileBodyCfgBlock = CfgBlock.empty();

        // save the current break and continue targets
        var saveTargets = new SaveTargets(loopSuccessor, whileBodyCfgBlock);

        var bodyExit = visitBlockStatement(whileStatement.getBody(), whileBodyCfgBlock);
        bodyExit.linkToSuccessor(currentBlock);

        currentBlock.addUserToEnd(whileStatement.getTest().toEvalCondition());
        currentBlock.addBranchTargets(whileBodyCfgBlock, loopSuccessor);

        saveTargets.restore();

        return loopSuccessor;
    }

    public @NotNull CfgBlock visitBlockStatement(@NotNull Block block, @NotNull CfgBlock currentBlock) {
        currentBlock.addUsers(block.getFieldDeclarations());
        var lastBlock = currentBlock;
        for (var statement : block.getStatements()) {
            lastBlock = visit(lastBlock, statement);
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

            var elseBlockExit = visit(elseBlockEntry, ifStatement.getElseBlock().get());
            var thenBlockExit = visit(thenBlockEntry, ifStatement.getThenBlock());

            thenBlockExit.linkToSuccessor(nextBlock);
            elseBlockExit.linkToSuccessor(nextBlock);

            currentBlock.addBranchTargets(thenBlockEntry, elseBlockEntry);
        } else {
            var thenBlockEntry = CfgBlock.empty();
            var thenBlockExit = visit(thenBlockEntry, ifStatement.getThenBlock());

            thenBlockExit.linkToSuccessor(nextBlock);

            currentBlock.addBranchTargets(thenBlockEntry, nextBlock);
        }
        // TOD0: add short-circuiting for && and ||
        currentBlock.addUserToEnd(ifStatement.getCondition().toEvalCondition());
        ShortCircuitUtil.shortCircuit(currentBlock);
        return nextBlock;
    }

    private @NotNull CfgBlock findExitBlock() {
        var exitBlocks = new ArrayList<CfgBlock>();
        var workList = new Stack<CfgBlock>();
        var seen = new HashSet<CfgBlock>();
        workList.push(getEntryBlock());
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
                block.linkToSuccessor(exitBlock);
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

    public void validateCfgBlock(@NotNull CfgBlock cfgBlock) {
        checkState(cfgBlock.getSuccessors().size() <= 2, "a block cannot have more than two successors");
        // if this is a branch, then it must have two successors and the last instruction must be an eval condition
        if (cfgBlock.hasBranch()) {
            checkState(cfgBlock.getTerminator().isPresent(), "branch block must have a terminator");
            checkState(cfgBlock.getTerminator().get() instanceof Branch,
                       "branch block must have a branch as its last instruction"
                      );
            checkState(cfgBlock.getSuccessors().size() == 2, "branch block must have two successors");
        } else {
            if (cfgBlock.getSuccessors().isEmpty()) {
                checkState(context.getExitCfgBlock(methodName).orElseThrow() == cfgBlock,
                           "non-branch block must be the exit block"
                          );
            } else {
                checkState(!cfgBlock.getSuccessors().isEmpty(),
                           "a non-branching with no successors must be the exit block"
                          );
                checkState(cfgBlock.getAlternateSuccessor().isEmpty(),
                           "non-branch block cannot have an alternate successor"
                          );
            }
        }
    }

    private @NotNull CfgBlock getEntryBlock() {
        return context.getEntryCfgBlock(methodName).orElseThrow(() -> new IllegalStateException("entry block set"));
    }

    private void validateCfg() {
        var seen = new HashSet<CfgBlock>();
        var workList = new Stack<CfgBlock>();
        workList.add(getEntryBlock());
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
        // ensure this is actually an empty block
        checkArgument(emptyCfgBlock.isEmpty(), "Cannot remove non-empty block");

        // we always want to have one unique exit block and one unique entry block
        // we never remove the exit block if it has more than one predecessor
        // we also never want to remove the exit block out of a loop;
        // else the branching block becomes a non-branching block
        if (context.isExitBlock(emptyCfgBlock) &&
            (emptyCfgBlock.hasMoreThanOnePredecessor() ||
             emptyCfgBlock.hasOnlyOnePredecessor() && emptyCfgBlock.getSolePredecessor().hasBranch())) {
            return;
        }

        if (context.isEntryBlock(emptyCfgBlock) || context.isExitBlock(emptyCfgBlock)) {
            if (context.isEntryBlock(emptyCfgBlock) && !emptyCfgBlock.hasBranch()) {
                // confirm no predecessors exist
                checkState(emptyCfgBlock.getPredecessors().isEmpty(), "entry block must have no predecessor");
                // make sure only once successor exists
                var successor = emptyCfgBlock.getSuccessor()
                                             .orElseThrow(() -> new IllegalStateException(
                                                     "alternate successor should not exist"));
                emptyCfgBlock.unlinkFromSuccessor(successor);
            }
            if (context.isExitBlock(emptyCfgBlock) && emptyCfgBlock.hasOnlyOnePredecessor()) {
                // confirm no successors exist
                checkState(emptyCfgBlock.getSuccessors().isEmpty(), "exit block must have no successors");
                // make sure only once predecessor exists
                var predecessor = emptyCfgBlock.getPredecessors().iterator().next();
                predecessor.unlinkFromSuccessor(emptyCfgBlock);
            }
        } else {
            final var successor = emptyCfgBlock.getSoleSuccessor();
            // remove the block from the predecessors
            for (var predecessor : emptyCfgBlock.getPredecessors()) {
                // grab the sole successor
                predecessor.unlinkFromSuccessor(emptyCfgBlock);
                predecessor.linkToSuccessor(successor);
            }
            emptyCfgBlock.unlinkFromSuccessor(successor);
        }
    }

    public void pruneEmptyBasicBlocks() {
        var seen = new HashSet<CfgBlock>();
        var workList = new Stack<CfgBlock>();
        workList.add(getEntryBlock());
        while (!workList.isEmpty()) {
            var current = workList.pop();
            if (seen.contains(current)) {
                continue;
            } else {
                seen.add(current);
            }
            // ensure the successors will be looked at by adding them
            // to the work list before possibly unlinking them from the current block
            workList.addAll(current.getSuccessors());
            if (current.isEmpty()) {
                removeEmptyBlock(current);
            }
        }
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
}

package decaf.ir.cfg;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Branch;
import decaf.analysis.syntax.ast.Expression;
import decaf.analysis.syntax.ast.Statement;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.*;

public class CfgBlock extends LinkedList<Statement> {
    private static int blockIdCounter = 0;
    @NotNull
    private List<CfgBlock> predecessors;
    private final int blockId;
    @Nullable
    private CfgBlock successor;
    @Nullable
    private CfgBlock alternateSuccessor;

    private CfgBlock(@Nullable CfgBlock successor,
                     @Nullable CfgBlock alternateSuccessor,
                     @Nullable List<CfgBlock> predecessors) {
        this.successor = successor;
        this.alternateSuccessor = alternateSuccessor;
        this.predecessors = Objects.requireNonNullElseGet(predecessors, ArrayList::new);
        this.blockId = createBlockId();
    }

    private static int createBlockId() {
        return blockIdCounter++;
    }

    public static CfgBlock empty() {
        return new CfgBlock(null, null, null);
    }

    public static CfgBlock createEntryBlock(String methodName, CompilationContext compilationContext) {
        var cfgBlock = CfgBlock.empty();
        compilationContext.addEntryCfgBlockFor(methodName, cfgBlock);
        return cfgBlock;
    }

    public static CfgBlock withBranch(@NotNull Branch branch, @NotNull CfgBlock successor, @NotNull CfgBlock alternateSuccessor) {
        var cfgBlock = CfgBlock.empty();
        cfgBlock.add(branch);
        cfgBlock.addBranchTargets(successor, alternateSuccessor);
        return cfgBlock;
    }

    public int getBlockId() {
        return blockId;
    }

    public Optional<CfgBlock> getSuccessor() {
        return Optional.ofNullable(successor);
    }

    public void setSuccessor(CfgBlock successor) {
        checkArgument(successor != null, "the successor of a block cannot be null");
        checkArgument(successor != this, "the successor of a block cannot be itself");
        this.successor = successor;
    }

    public Optional<CfgBlock> getAlternateSuccessor() {
        return Optional.ofNullable(alternateSuccessor);
    }

    public void setAlternateSuccessor(CfgBlock alternateSuccessor) {
        checkArgument(alternateSuccessor != this, "the alternate successor of a block cannot be itself");
        this.alternateSuccessor = alternateSuccessor;
    }

    public List<CfgBlock> getPredecessors() {
        return List.copyOf(predecessors);
    }

    public @NotNull CfgBlock addUserToEnd(@NotNull Statement user) {
        add(user);
        return this;
    }

    public void addUsers(@NotNull List<? extends Statement> users) {
        addAll(users);
    }

    public void addPredecessor(@NotNull CfgBlock predecessor) {
        checkNotNull(predecessor, "a predecessor cannot be null");
        checkState(!predecessors.contains(predecessor), "a block cannot have the same predecessor twice");
        predecessors.add(predecessor);
    }

    public Optional<AST> getTerminator() {
        return Optional.ofNullable(peekLast());
    }

    public Optional<AST> getLeader() {
        return Optional.ofNullable(peekFirst());
    }

    public void addSuccessor(@NotNull CfgBlock successor) {
        if (successor == this.successor || successor == this.alternateSuccessor) {
            Logger.getAnonymousLogger().info(String.format("%s already added", successor));
            return;
        }
        checkState(this.successor == null || this.alternateSuccessor == null,
                   "a block cannot have more than two successors"
                  );

        if (this.successor == null) {
            this.successor = successor;
        } else {
            this.alternateSuccessor = successor;
        }
    }

    public void linkToSuccessor(@NotNull CfgBlock successor) {
        addSuccessor(successor);
        successor.addPredecessor(this);
    }

    public void addBranchTargets(@NotNull CfgBlock successor, @NotNull CfgBlock alternateSuccessor) {
        checkNotNull(successor, "a successor cannot be null");
        checkNotNull(alternateSuccessor, "an alternate successor cannot be null");
        checkArgument(successor != alternateSuccessor, "a successor cannot be the same as the alternate successor");
        checkArgument(successor != this, "a block cannot be its own successor");
        checkArgument(alternateSuccessor != this, "a block cannot be its own alternate successor");
        checkArgument(this.successor == null, "a block cannot have more than two successors");
        checkArgument(this.alternateSuccessor == null, "a block cannot have more than two successors");
        this.successor = successor;
        this.alternateSuccessor = alternateSuccessor;
        successor.addPredecessor(this);
        alternateSuccessor.addPredecessor(this);
    }

    public List<CfgBlock> getSuccessors() {
        var successors = new ArrayList<CfgBlock>();
        if (this.successor != null) {
            successors.add(this.successor);
        }
        if (this.alternateSuccessor != null) {
            successors.add(this.alternateSuccessor);
        }
        return successors;
    }

    public String getSourceCode() {
        var sb = new StringBuilder();
        sb.append(String.format("L%d:\n", blockId));
        for (var user : this) {
            sb.append(user.getSourceCode());
            sb.append("\n");
        }
        return sb.toString();
    }


    public void removePredecessor(@NotNull CfgBlock predecessor) {
        checkState(predecessors.contains(predecessor), "a block cannot remove a predecessor that it does not have");
        predecessors.remove(predecessor);
    }

    public void removeSuccessor(@NotNull CfgBlock successor) {
        if (this.successor == successor) {
            this.successor = alternateSuccessor;
            this.alternateSuccessor = null;
        } else if (this.alternateSuccessor == successor) {
            this.alternateSuccessor = null;
        } else {
            throw new IllegalStateException("a block cannot remove a successor that it does not have");
        }
    }

    public void unlinkFromSuccessor(@NotNull CfgBlock successor) {
        removeSuccessor(successor);
        successor.removePredecessor(this);
    }

    @Override
    public String toString() {
        return String.format("L%d", blockId);
    }

    @Override
    public int hashCode() {
        return blockId;
    }

    public boolean hasBranch() {
        return getSuccessor().isPresent() &&
               getAlternateSuccessor().isPresent() &&
               getTerminator().map(terminator -> terminator instanceof Branch)
                              .orElseThrow(() -> new IllegalStateException(
                                      "a branching block must have a branch statement as a terminator"));
    }

    @Override
    public boolean isEmpty() {
        if (super.isEmpty()) {
            checkState(!hasBranch(), "a branching block must have a branch terminating statement");
            return true;
        }
        return false;
    }

    public boolean hasMoreThanOnePredecessor() {
        return predecessors.size() > 1;
    }

    public boolean hasNoPredecessors() {
        return predecessors.isEmpty();
    }

    public boolean hasOnlyOnePredecessor() {
        return predecessors.size() == 1;
    }

    public @NotNull CfgBlock getSolePredecessor() {
        checkState(hasOnlyOnePredecessor(),
                   String.format("make sure this block has exactly 1 predecessor: it has %s", getPredecessors().size())
                  );
        return predecessors.get(0);
    }

    public @NotNull CfgBlock getSoleSuccessor() {
        checkState(this.successor != null, "the sole successor cannot be null");
        checkState(this.alternateSuccessor == null, "found 2 successors instead of one");
        return this.successor;
    }

    public @NotNull Expression getBranchCondition() {
        checkState(getTerminator().isPresent());
        checkState(hasBranch(), "this block does not have a branch");
        return ((Branch) getTerminator().get()).getCondition();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CfgBlock cfgBlock) {
            return cfgBlock.getBlockId() == getBlockId();
        }
        return false;
    }

    public @NotNull CfgBlock getSuccessorOrThrow() {
        return getSuccessor().orElseThrow(() -> new IllegalStateException("this block does not have a successor"));
    }

    public @NotNull CfgBlock getAlternateSuccessorOrThrow() {
        return getAlternateSuccessor().orElseThrow(() -> new IllegalStateException("this block does not have an alternate successor"));
    }

    public void replaceWith(@NotNull CfgBlock cfgBlock) {
        if (this.equals(cfgBlock)) {
            return;
        }
        this.predecessors = cfgBlock.predecessors;
        this.successor = cfgBlock.successor;
        this.alternateSuccessor = cfgBlock.alternateSuccessor;
        this.clear();
        this.addAll(cfgBlock);

        // clean up the old block
        for (var pred: cfgBlock.predecessors) {
            pred.unlinkFromSuccessor(cfgBlock);
            pred.linkToSuccessor(this);
        }

        cfgBlock.successor = null;
        cfgBlock.alternateSuccessor = null;
        cfgBlock.predecessors.clear();
        cfgBlock.clear();
    }
}

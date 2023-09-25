package decaf.ir.cfg;

import decaf.analysis.syntax.ast.AST;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.google.common.base.Preconditions.*;

public class CfgBlock extends LinkedList<AST> {
    @Nullable
    private CfgBlock successor;

    @Nullable
    private CfgBlock alternateSuccessor;

    @NotNull
    private final List<CfgBlock> predecessors;

    private static int blockIdCounter = 0;

    private final int blockId;

    private static int createBlockId() {
        return blockIdCounter++;
    }

    public int getBlockId() {
        return blockId;
    }

    private CfgBlock(@Nullable CfgBlock successor,
                     @Nullable CfgBlock alternateSuccessor,
                     @Nullable List<CfgBlock> predecessors) {
        this.successor = successor;
        this.alternateSuccessor = alternateSuccessor;
        this.predecessors = Objects.requireNonNullElseGet(predecessors, ArrayList::new);
        this.blockId = createBlockId();
    }

    public static CfgBlock withBothBranches(@NotNull CfgBlock successor, @NotNull CfgBlock alternateSuccessor) {
        return new CfgBlock(successor, alternateSuccessor, null);
    }

    public CfgBlock createExitBlock(String methodName, CompilationContext compilationContext) {
        var cfgBlock = CfgBlock.empty();
        compilationContext.addExitCfgBlockFor(methodName, cfgBlock);
        return cfgBlock;
    }

    public CfgBlock createEntryBlock(String methodName, CompilationContext compilationContext) {
        var cfgBlock = CfgBlock.empty();
        compilationContext.addEntryCfgBlockFor(methodName, cfgBlock);
        return cfgBlock;
    }

    public static CfgBlock empty() {
        return new CfgBlock(null, null, null);
    }

    public static CfgBlock withSuccessor(@Nullable CfgBlock successor) {
        return new CfgBlock(successor, null, null);
    }

    public Optional<CfgBlock> getSuccessor() {
        return Optional.ofNullable(successor);
    }

    public Optional<CfgBlock> getAlternateSuccessor() {
        return Optional.ofNullable(alternateSuccessor);
    }

    public List<CfgBlock> predecessors() {
        return List.copyOf(predecessors);
    }

    public void setSuccessor(CfgBlock successor) {
        checkArgument(successor != null, "the successor of a block cannot be null");
        checkArgument(successor != this, "the successor of a block cannot be itself");
        this.successor = successor;
    }

    public void setAlternateSuccessor(CfgBlock alternateSuccessor) {
        checkArgument(alternateSuccessor != this, "the alternate successor of a block cannot be itself");
        this.alternateSuccessor = alternateSuccessor;
    }

    public @NotNull CfgBlock addUserToEnd(@NotNull AST user) {
        add(user);
        return this;
    }

    public void addUsers(@NotNull List<? extends AST> users) {
        addAll(users);
    }

    public void addPredecessor(@NotNull CfgBlock predecessor) {
        checkNotNull(predecessor, "a predecessor cannot be null");
        checkState(!predecessors.contains(predecessor), "a block cannot have the same predecessor twice");
        checkArgument(predecessor != this, "a block cannot be its own predecessor");
        predecessors.add(predecessor);
    }

    public Optional<AST> getTerminator() {
        return Optional.ofNullable(peekLast());
    }

    public Optional<AST> getLeader() {
        return Optional.ofNullable(peekFirst());
    }

    public void addSuccessor(@NotNull CfgBlock successor) {
        if (this.successor == successor) {
            return;
        }
        if (this.alternateSuccessor == successor) {
            return;
        }
        if (this.successor == null) {
            this.successor = successor;
        } else if (this.alternateSuccessor == null) {
            this.alternateSuccessor = successor;
        } else {
            throw new IllegalStateException("a block cannot have more than two successors");
        }
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
        if (!predecessors.contains(predecessor)) {
            throw new IllegalStateException("a block cannot remove a predecessor that it does not have");
        }
        predecessors.remove(predecessor);
    }

    @Override
    public String toString() {
        return String.format("L%d", blockId);
    }

    @Override
    public int hashCode() {
        return blockId;
    }
}

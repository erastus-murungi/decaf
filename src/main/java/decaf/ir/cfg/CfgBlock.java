package decaf.ir.cfg;

import decaf.analysis.syntax.ast.AST;
import decaf.shared.CompilationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.google.common.base.Preconditions.*;

public class CfgBlock implements Iterable<AST> {
    private final LinkedList<AST> users;

    @Nullable
    private CfgBlock successor;

    @Nullable
    private CfgBlock alternateSuccessor;

    @NotNull
    private final List<CfgBlock> predecessors;

    private CfgBlock(@Nullable LinkedList<AST> users,
                     @Nullable CfgBlock successor,
                     @Nullable CfgBlock alternateSuccessor,
                     @Nullable List<CfgBlock> predecessors) {
        this.users = Objects.requireNonNullElseGet(users, LinkedList::new);
        this.predecessors = Objects.requireNonNullElseGet(predecessors, ArrayList::new);
        this.successor = successor;
        this.alternateSuccessor = alternateSuccessor;
    }

    public static CfgBlock withBothBranches(@NotNull CfgBlock successor, @NotNull CfgBlock alternateSuccessor) {
        return new CfgBlock(null, successor, alternateSuccessor, null);
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
        return new CfgBlock(null, null, null, null);
    }

    public static CfgBlock withSuccessor(@Nullable CfgBlock successor) {
        return new CfgBlock(null, successor, null, null);
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

    @NotNull
    @Override
    public Iterator<AST> iterator() {
        return users.iterator();
    }

    public void addUserToEnd(@NotNull AST user) {
        users.add(user);
    }

    public void addUserToFront(@NotNull AST user) {
        users.addFirst(user);
    }

    public void addUsers(@NotNull List<AST> users) {
        this.users.addAll(users);
    }

    public void addPredecessor(@NotNull CfgBlock predecessor) {
        checkNotNull(predecessor, "a predecessor cannot be null");
        checkState(!predecessors.contains(predecessor), "a block cannot have the same predecessor twice");
        checkArgument(predecessor != this, "a block cannot be its own predecessor");
        predecessors.add(predecessor);
    }

    public Optional<AST> getTerminator() {
        return Optional.ofNullable(users.peekLast());
    }

    public Optional<AST> getLeader() {
        return Optional.ofNullable(users.peekFirst());
    }

    @Override
    public String toString() {
        return "CfgBlock{" +
               "users=" + users +
               '}';
    }

    public void addSuccessor(CfgBlock successor) {
        if (this.successor == null) {
            this.successor = successor;
        } else if (this.alternateSuccessor == null) {
            this.alternateSuccessor = successor;
        } else {
            throw new IllegalStateException("a block cannot have more than two successors");
        }
    }

    public String getSourceCode() {
        var sb = new StringBuilder();
        for (var user : users) {
            sb.append(user.getSourceCode());
            sb.append("\n");
        }
        return sb.toString();
    }
}

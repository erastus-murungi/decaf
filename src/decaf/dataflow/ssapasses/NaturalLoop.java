package decaf.dataflow.ssapasses;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import decaf.cfg.BasicBlock;
import decaf.dataflow.dominator.DominatorTree;

public class NaturalLoop {
    /**
     * Single entry point to the loop that dominates all the other blocks in the loop
     */
    @NotNull
    private final BasicBlock header;
    /**
     * A latch is a loop node that has an edge to the header.
     */
    @NotNull
    private final BasicBlock latch;
    /**
     * This a set of all the blocks in the loop's body.
     * They must all be dominated by the header
     */
    @NotNull
    private final Set<BasicBlock> body = new HashSet<>();
    /**
     * Blocks whose immediate predecessors are in the loop body, but they themselves aren't
     */
    @NotNull
    private final Set<BasicBlock> exitBlocks = new HashSet<>();
    /**
     * A single block whose immediate successor is outside the loop
     */
    @NotNull
    private final BasicBlock preHeader = BasicBlock.noBranch();

    public NaturalLoop(@NotNull BasicBlock header, @NotNull BasicBlock latch, @NotNull DominatorTree dominatorTree) {
        checkArgument(latch.getSuccessors().contains(header));
        checkArgument(dominatorTree.dom(header, latch));
        this.header = header;
        this.latch = latch;
        buildBody(dominatorTree);
        checkState(getBody().stream().allMatch(basicBlock -> dominatorTree.dom(getHeader(), basicBlock)));
        findExitBlocks();
    }

    private void findExitBlocks() {
        for (BasicBlock basicBlock : getBody()) {
            for (BasicBlock successor : basicBlock.getSuccessors()) {
                if (!body.contains(successor)) {
                    exitBlocks.add(successor);
                }
            }
        }
    }

    public void buildBody(DominatorTree dominatorTree) {
        var body = new HashSet<BasicBlock>();
        var stack = new Stack<BasicBlock>();

        body.add(getHeader());
        stack.add(getLatch());

        while (!stack.isEmpty()) {
            var D = stack.pop();
            if (!body.contains(D) && dominatorTree.dom(getHeader(), D)) {
                body.add(D);
                stack.addAll(D.getPredecessors());
            }
        }
        this.body.addAll(body);
    }

    public @NotNull Set<BasicBlock> getBody() {
        return body;
    }

    public @NotNull BasicBlock getHeader() {
        return header;
    }

    public @NotNull BasicBlock getLatch() {
        return latch;
    }

    @Override
    public String toString() {
        return "NaturalLoop{" + "preHeader=" + preHeader + ", header=" + header + ", body=" + body + ", latch=" + latch + ", exitBlocks=" + exitBlocks + '}';
    }
}

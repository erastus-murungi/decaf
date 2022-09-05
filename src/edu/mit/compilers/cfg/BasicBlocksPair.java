package edu.mit.compilers.cfg;

public class BasicBlocksPair {
    public BasicBlock startBlock;
    public BasicBlock endBlock;

    public BasicBlocksPair(BasicBlock startBlock, BasicBlock endBlock) {
        this.startBlock = startBlock;
        if (startBlock != endBlock)
            endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }

    public BasicBlocksPair(BasicBlock startBlock, BasicBlock endBlock, boolean createLink) {
        this.startBlock = startBlock;
        if (createLink)
            if (startBlock != endBlock)
                endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }

}

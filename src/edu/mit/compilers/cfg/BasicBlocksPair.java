package edu.mit.compilers.cfg;

public class BasicBlocksPair {
    public BasicBlock startBlock;
    public BasicBlockBranchLess endBlock;

    public BasicBlocksPair(BasicBlock startBlock, BasicBlockBranchLess endBlock){
        this.startBlock = startBlock;
        if (startBlock != endBlock)
            endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }

    public BasicBlocksPair(BasicBlock startBlock, BasicBlockBranchLess endBlock, boolean createLink){
        this.startBlock = startBlock;
        if (createLink)
            if (startBlock != endBlock)
                endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }
    
}

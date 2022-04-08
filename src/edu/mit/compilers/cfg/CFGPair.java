package edu.mit.compilers.cfg;

public class CFGPair {
    public CFGBlock startBlock;
    public CFGNonConditional endBlock;

    public CFGPair(CFGBlock startBlock, CFGNonConditional endBlock){
        this.startBlock = startBlock;
        if (startBlock != endBlock)
            endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }

    public CFGPair(CFGBlock startBlock, CFGNonConditional endBlock, boolean createLink){
        this.startBlock = startBlock;
        if (createLink)
            if (startBlock != endBlock)
                endBlock.addPredecessor(startBlock);
        this.endBlock = endBlock;
    }
    
}

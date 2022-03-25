package edu.mit.compilers.cfg;

public class CFGPair {
    public CFGBlock startBlock;
    public CFGNonConditional endBlock;

    public CFGPair(CFGBlock startBlock, CFGNonConditional endBlock){
        this.startBlock = startBlock;
        if (startBlock != endBlock)
            endBlock.parents.add(startBlock);
        this.endBlock = endBlock;
    }
    
}

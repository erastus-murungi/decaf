package edu.mit.compilers.cfg;

public class CFGPair {

    public CFGBlock startBlock;
    public CFGBlock endBlock;


    public CFGPair(CFGBlock startBlock, CFGBlock endBlock){
        this.startBlock = startBlock;
        this.endBlock = endBlock;
    }
    
}

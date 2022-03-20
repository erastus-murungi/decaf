package edu.mit.compilers.cfg;

import java.util.HashMap;

public class CFG {

    public CFGBlock initialGlobalBlock;
    public HashMap<String, CFGBlock> methodCFGBlocks = new HashMap<>();

    public CFG() {

    }
}

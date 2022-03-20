package edu.mit.compilers.cfg;

import java.util.ArrayList;

public class CFGBlock {

    public ArrayList<CFGBlock> parents;
    public CFGBlock autoChild;
    public CFGBlock trueChild;
    public CFGBlock falseChild;
    public ArrayList<CFGLine> lines;

    public CFGBlock() {
        parents = new ArrayList<>();
        autoChild = new CFGBlock();
        trueChild = new CFGBlock();
        falseChild = new CFGBlock();
        lines = new ArrayList<>();
    }
}

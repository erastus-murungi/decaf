package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.MethodDefinition;

import java.util.List;

public class CFGBlock {
    List<CFGBlock> predecessors;
    List<CFGBlock> successors;

    MethodDefinition methodDefinition;
}

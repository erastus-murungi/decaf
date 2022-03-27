package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CFGGenerator {
    public AST rootNode;
    public GlobalDescriptor globalDescriptor;

    public CFGGenerator(AST ast, GlobalDescriptor globalDescriptor) {
        this.rootNode = ast;
        this.globalDescriptor = globalDescriptor;
    }

    public iCFGVisitor buildiCFG() {
        iCFGVisitor visitor = new iCFGVisitor();
        MaximalVisitor maximalVisitor = new MaximalVisitor();
        NopVisitor nopVisitor = new NopVisitor();
        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);
        SymbolTable theSymbolWeCareAbout = globalDescriptor.globalVariablesSymbolTable;


        visitor.methodCFGBlocks.forEach((k, v) -> {
            nopVisitor.exit = new NOP();
            ((CFGNonConditional) v).autoChild.accept(nopVisitor, theSymbolWeCareAbout);
        });

        visitor.methodCFGBlocks.forEach((k, v) -> v.accept(maximalVisitor, theSymbolWeCareAbout));
        visitor.initialGlobalBlock.accept(nopVisitor, theSymbolWeCareAbout);
        HashMap<String, CFGBlock> methodBlocksCFG = new HashMap<>();
        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (v.getLabel().equals("∅")) {
                if (((CFGNonConditional) v).autoChild != null) {
                    ((CFGNonConditional) v).autoChild.parents.remove(v);
                    v = ((CFGNonConditional) v).autoChild;
                }
            }
            methodBlocksCFG.put(k, v);
        });
        visitor.methodCFGBlocks = methodBlocksCFG;
        visitor.initialGlobalBlock.accept(maximalVisitor, theSymbolWeCareAbout);

        return visitor;
    }

    public static void printAllParents(CFGBlock block) {
        Stack<CFGBlock> toVisit = new Stack<>();
        toVisit.add(block);
        Set<CFGBlock> seen = new HashSet<>();
        while (!toVisit.isEmpty()) {
            block = toVisit.pop();
            if (seen.contains(block) || block == null) {
                continue;
            }
            seen.add(block);
            System.out.println(Utils.coloredPrint(block.getLabel(), Utils.ANSIColorConstants.ANSI_PURPLE) + "  >>>>   ");
            for (CFGBlock parent: block.parents) {
                System.out.println(Utils.indentBlock(parent.getLabel()));
                System.out.println();
            }
            if (block instanceof CFGConditional) {
                toVisit.add(((CFGConditional) block).falseChild);
                toVisit.add(((CFGConditional) block).trueChild);
            } else {
                toVisit.add(((CFGNonConditional) block).autoChild);
            }
        }
    }

    public static void printAllChildren(CFGBlock block) {
        Stack<CFGBlock> toVisit = new Stack<>();
        toVisit.add(block);
        Set<CFGBlock> seen = new HashSet<>();
        while (!toVisit.isEmpty()) {
            block = toVisit.pop();
            if (seen.contains(block) || block == null) {
                continue;
            }
            seen.add(block);
            System.out.println(Utils.coloredPrint(block.getLabel(), Utils.ANSIColorConstants.ANSI_BLUE) + "  <<<<   ");

            if (block instanceof CFGConditional) {
                CFGBlock falseChild = ((CFGConditional) block).falseChild;
                CFGBlock trueChild = ((CFGConditional) block).trueChild;
                toVisit.add(falseChild);
                toVisit.add(trueChild);
                if (trueChild != null) {
                    System.out.print("T ---- ");
                    System.out.println(Utils.coloredPrint(trueChild.getLabel(), Utils.ANSIColorConstants.ANSI_GREEN));
                }
                System.out.println();
                if (falseChild != null) {
                    System.out.print("F ---- ");
                    System.out.println(Utils.coloredPrint(falseChild.getLabel(), Utils.ANSIColorConstants.ANSI_RED));
                }
                System.out.println();

            } else {
                CFGBlock autoChild = ((CFGNonConditional) block).autoChild;
                toVisit.add(autoChild);
                if (autoChild != null) {
                    System.out.print("A ---- ");
                    System.out.println(autoChild.getLabel());
                    System.out.println();
                }
            }
        }
    }
}

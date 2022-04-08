package edu.mit.compilers.cfg;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.descriptors.GlobalDescriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.exceptions.DecafException;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class CFGGenerator {
    public AST rootNode;
    public GlobalDescriptor globalDescriptor;
    public List<DecafException> errors = new ArrayList<>();

    public CFGGenerator(AST ast, GlobalDescriptor globalDescriptor) {
        this.rootNode = ast;
        this.globalDescriptor = globalDescriptor;
    }

    private void catchFalloutError(NOP exitNop, MethodDescriptor methodDescriptor) {
        final MethodDefinition methodDefinition = methodDescriptor.methodDefinition;
        if (methodDescriptor.type == BuiltinType.Void)
            return;

        exitNop.getPredecessors().removeIf(block -> !(block
                .getSuccessors()
                .contains(exitNop)));

        final List<CFGBlock> allExecutionPathsReturn = exitNop.getPredecessors()
                .stream()
                .filter(cfgBlock -> (!cfgBlock.lines.isEmpty() && (cfgBlock.lastASTLine() instanceof Return)))
                .collect(Collectors.toList());

        final List<CFGBlock> allExecutionsPathsThatDontReturn = exitNop.getPredecessors()
                .stream()
                .filter(cfgBlock -> (cfgBlock.lines.isEmpty() || (!(cfgBlock.lastASTLine() instanceof Return))))
                .collect(Collectors.toList());

        if (allExecutionPathsReturn.size() != allExecutionsPathsThatDontReturn.size()) {
            errors.addAll(allExecutionsPathsThatDontReturn
                    .stream()
                    .map(cfgBlock -> new DecafException(methodDefinition.tokenPosition,
                            methodDefinition.methodName.id + "'s execution path ends with" +
                            (cfgBlock.lines.isEmpty() ? "" : (cfgBlock
                                    .lastASTLine()
                                    .getSourceCode())) + " instead of a return statement"))
                    .collect(Collectors.toList()));
        }
        if (allExecutionPathsReturn.isEmpty()) {
            errors.add(new DecafException(
                    methodDefinition.tokenPosition,
                    methodDefinition.methodName.id + " method does not return expected type " + methodDefinition.returnType));
        }
    }

    public iCFGVisitor buildiCFG() {
        final iCFGVisitor visitor = new iCFGVisitor();
        final MaximalVisitor maximalVisitor = new MaximalVisitor();
        final NopVisitor nopVisitor = new NopVisitor();

        rootNode.accept(visitor, globalDescriptor.globalVariablesSymbolTable);
        final SymbolTable theSymbolWeCareAbout = globalDescriptor.globalVariablesSymbolTable;

        visitor.methodCFGBlocks.forEach((k, v) -> {
            nopVisitor.exit = visitor.methodToExitNOP.get(k);
            ((CFGNonConditional) v).autoChild.accept(nopVisitor, theSymbolWeCareAbout);
        });

        visitor.methodCFGBlocks.forEach((k, v) -> {
            maximalVisitor.exitNOP = visitor.methodToExitNOP.get(k);
            v.accept(maximalVisitor, theSymbolWeCareAbout);
            catchFalloutError(maximalVisitor.exitNOP, (MethodDescriptor) globalDescriptor.methodsSymbolTable
                    .getDescriptorFromValidScopes(k)
                    .orElseThrow());
        });
        visitor.initialGlobalBlock.accept(nopVisitor, theSymbolWeCareAbout);
        HashMap<String, CFGBlock> methodBlocksCFG = new HashMap<>();
        visitor.methodCFGBlocks.forEach((k, v) -> {
            if (v
                    .getLabel()
                    .isBlank()) {
                if (((CFGNonConditional) v).autoChild != null) {
                    ((CFGNonConditional) v).autoChild.getPredecessors().remove(v);
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
            for (CFGBlock parent : block.getPredecessors()) {
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

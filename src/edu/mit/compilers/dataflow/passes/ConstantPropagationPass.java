package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.analyses.ReachingDefinitions;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.dataflow.reachingvalues.DefValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConstantPropagationPass extends OptimizationPass {

    public ConstantPropagationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    private List<HasOperand> reachingDefinitions(int indexOfDefinition, InstructionList instructionList) {
        var rds = new ArrayList<HasOperand>();
        var store = ((Store) instructionList.get(indexOfDefinition)).getStore();
        for (int indexOfInstruction = indexOfDefinition + 1; indexOfInstruction < instructionList.size(); indexOfInstruction++) {
            var instruction = instructionList.get(indexOfInstruction);
            if (instruction instanceof Store) {
                if (((Store) instruction).getStore()
                        .equals(store))
                    break;
            } else {
                if (instruction instanceof HasOperand)
                    rds.add((HasOperand) instruction);
            }
        }
        return rds;
    }


    public void localConstantPropagation(BasicBlock basicBlock) {
        int indexOfInstruction = -1;
        for (Instruction instruction : basicBlock.instructionList) {
            indexOfInstruction++;
            if (instruction instanceof Assign) {
                if (((Assign) instruction).operand instanceof ConstantName) {
                    ConstantName constant = (ConstantName) ((Assign) instruction).operand;
                    for (HasOperand hasOperand : reachingDefinitions(indexOfInstruction, basicBlock.instructionList)) {
                        hasOperand.replace(((Assign) instruction).store, constant);
                    }
                }
            }
        }
    }

    public void runGlobalConstantPropagation() {
        final ReachingDefinitions reachingDefinitions = new ReachingDefinitions(entryBlock);
        for (BasicBlock basicBlock : basicBlocks) {
            localConstantPropagation(basicBlock);
            var tacList = basicBlock.getCopyOfInstructionList();
            final var newTacList = basicBlock.instructionList;
            newTacList.clear();
            var inSet = organize(reachingDefinitions.in.get(basicBlock));
            HashMap<AbstractName, DefValue> freshlyGen = new HashMap<>();
            for (Instruction line : tacList) {
                // if the line is a definition of a variable with variables in its righthand side, we will attempt to replace
                // the variables with constants
                if (line instanceof HasOperand) {
                    var newLine = (HasOperand) line;
                    // for each variable name
                    for (AbstractName oldName : newLine.getOperandNames()) {
                        if (globalVariables.contains(oldName))
                            continue;
                        // check if that variable has only one reaching definition and it hasn't been redefined in the block
                        if (inSet.containsKey(oldName) && inSet.get(oldName)
                                .size() == 1 && !freshlyGen.containsKey(oldName)) {
                            DefValue defValue = (DefValue) inSet.get(oldName)
                                    .toArray()[0];
                            if (defValue.operand instanceof UnmodifiedOperand) {
                                var unmodified = (UnmodifiedOperand) defValue.operand;
                                if (unmodified.abstractName instanceof ConstantName) {
                                    var constant = (ConstantName) unmodified.abstractName;
                                    ((HasOperand) line).replace(oldName, constant);
                                }
                            }
                        }
                        // check if the variable was freshly redefined
                        if (freshlyGen.containsKey(oldName)) {
                            DefValue defValue = freshlyGen.get(oldName);
                            if (defValue.operand instanceof UnmodifiedOperand) {
                                var unmodified = (UnmodifiedOperand) defValue.operand;
                                if (unmodified.abstractName instanceof ConstantName) {
                                    var constant = (ConstantName) unmodified.abstractName;
                                    ((HasOperand) line).replace(oldName, constant);
                                }
                            }
                        }
                    }
                }
                // if line is new definition of a variable, we update the map which keeps track of the latest definition
                // of a variable in our current block
                if (line instanceof Store) {
                    DefValue defValue = new DefValue((Store) line);
                    freshlyGen.put(defValue.variableName, defValue);
                }
                newTacList.add(line);
            }
        }
    }

    private HashMap<AbstractName, Set<DefValue>> organize(Set<DefValue> setOfStuff) {
        HashMap<AbstractName, Set<DefValue>> labelToKilled = new HashMap<>();
        for (var stuff : setOfStuff) {
            var label = stuff.variableName;
            if (labelToKilled.containsKey(label)) {
                labelToKilled.get(label)
                        .add(stuff);
            } else {
                HashSet<DefValue> placeholder = new HashSet<>();
                placeholder.add(stuff);
                labelToKilled.put(label, placeholder);
            }
        }
        return labelToKilled;
    }

    @Override
    public boolean run() {
        final var oldCodes = entryBlock.getCopyOfInstructionList();
        runGlobalConstantPropagation();
        return oldCodes.equals(entryBlock.instructionList);
    }
}

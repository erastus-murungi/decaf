package edu.mit.compilers.dataflow.passes;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ConstantName;
import edu.mit.compilers.dataflow.analyses.ReachingDefinitions;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.dataflow.reachingvalues.DefValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ConstantPropagationPass extends OptimizationPass {

    public ConstantPropagationPass(Set<AbstractName> globalVariables, MethodBegin methodBegin) {
        super(globalVariables, methodBegin);
    }

    public void runGlobalConstantPropagation() {
        final ReachingDefinitions reachingDefinitions = new ReachingDefinitions(entryBlock);
        for (BasicBlock basicBlock : basicBlocks) {
            System.out.println(basicBlock);
            var tacList = basicBlock.getCopyOfInstructionList();
            final var newTacList = basicBlock.instructionList;
            newTacList.clear();
            var inSet = organize(reachingDefinitions.in.get(basicBlock));
            HashMap<AbstractName, DefValue> freshlyGen = new HashMap<>();
            for (Instruction line : tacList){
                // if line is new definition of a variable, we update the map which keeps track of the latest definition
                // of a variable in our current block
                if (line instanceof Store){
                    DefValue defValue = new DefValue((Store) line);
                    freshlyGen.put(defValue.variableName, defValue);
                }

                // if the line is a definition of a variable with variables in its righthand side, we will attempt to replace
                // the variables with constants
                if (line instanceof HasOperand){
                    var newLine = (HasOperand) line;
                    // for each variable name
                    for (AbstractName oldName : newLine.getOperandNames()){
                        if (globalVariables.contains(oldName))
                            continue;
                        // check if that variable has only one reaching definition and it hasn't been redefined in the block
                        if (inSet.containsKey(oldName) && inSet.get(oldName).size() == 1 && !freshlyGen.containsKey(oldName)){
                            DefValue defValue = (DefValue) inSet.get(oldName).toArray()[0];
                            if (defValue.operand instanceof UnmodifiedOperand) {
                                var unmodified = (UnmodifiedOperand) defValue.operand;
                                if (unmodified.abstractName instanceof ConstantName){
                                    var constant = (ConstantName) unmodified.abstractName;
                                    ((HasOperand) line).replace(oldName, constant);
                                }
                            }
                        }
                        // check if the variable was freshly redefined
                        if (freshlyGen.containsKey(oldName)){
                            DefValue defValue = freshlyGen.get(oldName);
                            if (defValue.operand instanceof UnmodifiedOperand) {
                                var unmodified = (UnmodifiedOperand) defValue.operand;
                                if (unmodified.abstractName instanceof ConstantName){
                                    var constant = (ConstantName) unmodified.abstractName;
                                    ((HasOperand) line).replace(oldName, constant);
                                }
                            }
                        }
                    }
                }
                newTacList.add(line);
            }
        }
    }

    private HashMap<AbstractName, Set<DefValue>> organize(Set<DefValue> setOfStuff) {
        HashMap<AbstractName, Set<DefValue>> labelToKilled = new HashMap<>();
        for (var stuff : setOfStuff){
            var label = stuff.variableName;
            if (labelToKilled.containsKey(label)){
                labelToKilled.get(label).add(stuff);
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

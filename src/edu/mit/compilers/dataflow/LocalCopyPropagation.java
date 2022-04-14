package edu.mit.compilers.dataflow;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.TemporaryName;
import edu.mit.compilers.codegen.names.VariableName;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class LocalCopyPropagation {
    HashMap<ThreeAddressCode, Integer> tacToPosition;
    Map<AbstractName, AbstractName> copy;

    public LocalCopyPropagation(BasicBlock basicBlock, Set<AbstractName> globalVariables) {
        copy = new LinkedHashMap<>();
        getTacToPosMapping(basicBlock.threeAddressCodeList);
        performLocalCopyPropagation(basicBlock, globalVariables);
    }

    private void performLocalCopyPropagation(BasicBlock basicBlock, Set<AbstractName> globalVariables) {
        final var tac = basicBlock.threeAddressCodeList;
        var oldTacList = basicBlock.threeAddressCodeList.clone();
        for (ThreeAddressCode threeAddressCode : tac) {
            if (threeAddressCode instanceof Assign) {
                Assign assign = (Assign) threeAddressCode;
                if (assign.assignmentOperator.equals("=") &&
                        (assign.getResultLocation() instanceof VariableName
                                || assign.getResultLocation() instanceof TemporaryName) &&
                        !(assign.operand instanceof ArrayName) && !globalVariables.contains(assign.getResultLocation())) {
                    copy.put(assign.getResultLocation(), assign.operand);
                }
            }
        }
    }


    public void getTacToPosMapping(ThreeAddressCodeList threeAddressCodeList) {
        tacToPosition = new HashMap<>();
        var index = 0;
        for (ThreeAddressCode tac : threeAddressCodeList) {
            tacToPosition.put(tac, index);
            ++index;
        }
    }
}

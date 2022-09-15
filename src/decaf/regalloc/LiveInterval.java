package decaf.regalloc;

import decaf.codegen.InstructionList;
import decaf.codegen.codes.Instruction;
import decaf.codegen.names.IrAssignableValue;
import decaf.codegen.codes.Method;

/**
 * @param irAssignableValue        The irAssignableValue this {@link LiveInterval} belongs to
 * @param startPoint      The start index of the {@link Instruction} in the given {@link InstructionList}
 * @param endPoint        The end index of the instruction in the given {@link InstructionList}
 * @param instructionList The {@link InstructionList} this {@link LiveInterval} belongs to
 * @param method          The {@link Method} containing this {@link LiveInterval}
 *
 *                        <ul>
 *                           <li>The <strong>live range</strong> for a irAssignableValue is the set of program points at which that irAssignableValue is live.</p>
 *                        </ul>
 *                        <ul>
 *                           <li>
 *                              The <strong>live interval</strong> for a irAssignableValue is the smallest subrange of the IR code containing all a irAssignableValue's live ranges. </p>
 *                              <ul>
 *                                 <li>A property of the IR code, not CFG. </li>
 *                                 <li>Less precise than live ranges, but simpler to work with</li>
 *                              </ul>
 *                           </li>
 *                        </ul>
 *                        </li>
 */
public record LiveInterval(IrAssignableValue irAssignableValue, int startPoint, int endPoint, InstructionList instructionList, Method method) {
    public LiveInterval updateEndpoint(int endPoint) {
        return new LiveInterval(irAssignableValue, startPoint, endPoint, instructionList, method);
    }
    public int compareStartPoint(LiveInterval other) {
        if (startPoint == other.startPoint)
            return 0;
        return startPoint < other.startPoint ? -1 : 1;
    }

    public int compareEndpoint(LiveInterval other) {
        if (endPoint == other.endPoint)
            return 0;
        return endPoint < other.endPoint ? -1 : 1;
    }
}

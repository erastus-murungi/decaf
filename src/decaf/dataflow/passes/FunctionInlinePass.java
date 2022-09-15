package decaf.dataflow.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import decaf.codegen.InstructionList;
import decaf.codegen.codes.FunctionCallWithResult;
import decaf.codegen.codes.HasOperand;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.ReturnInstruction;
import decaf.codegen.codes.UnconditionalBranch;
import decaf.common.TarjanSCC;
import decaf.cfg.BasicBlock;
import decaf.codegen.TraceScheduler;
import decaf.codegen.codes.ArrayBoundsCheck;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCall;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.names.IrValue;


public class FunctionInlinePass {
    List<Method> methodList;

    public FunctionInlinePass(List<Method> methodList) {
        this.methodList = methodList;
    }

    public List<Method> run() {
        List<Method> newMethodList = new ArrayList<>();
        for (var methodBegin : methodList) {
            if (shouldBeInlined(methodBegin)) {
                inlineMethodBegin(methodBegin);
                continue;
            }
            newMethodList.add(methodBegin);
        }
        return newMethodList;
    }

    private Map<IrValue, IrValue> mapParametersToArguments(
        InstructionList functionTacList,
        List<IrValue> arguments) {
        var method = (Method) functionTacList.get(0);
        var map = new HashMap<IrValue, IrValue>();
        var parameters = method.getParameterNames();
        for (int indexOfCode = 0; indexOfCode < parameters.size(); indexOfCode++) {
            map.put(parameters.get(indexOfCode), arguments.get(indexOfCode));
        }
        return map;
    }

    private Map<InstructionList, List<Integer>> findCallSites(Method method, String functionName) {
        var callSites = new HashMap<InstructionList, List<Integer>>();
        for (BasicBlock basicBlock : TarjanSCC.getReversePostOrder(method.getEntryBlock())) {
            var threeAddressCodeList = basicBlock.getInstructionList();
            callSites.put(threeAddressCodeList, new ArrayList<>());
            IntStream.range(0, threeAddressCodeList.size())
                    .forEach(indexOfTac -> {
                        if (isMethodCallAndNameMatches(threeAddressCodeList.get(indexOfTac), functionName)) {
                            callSites.get(threeAddressCodeList)
                                    .add(indexOfTac);
                        }
                    });
        }
        return callSites;
    }

    private List<IrValue> findArgumentNames(InstructionList instructionList, int indexOfFunctionCall) {
        assert instructionList.get(indexOfFunctionCall) instanceof FunctionCall;
        return ((FunctionCall) instructionList.get(indexOfFunctionCall)).getArguments();
    }

    private void inlineMethodBegin(Method method) {
        var functionName = method.methodName();
        for (Method targetMethod : methodList) {
            if (method == targetMethod)
                continue;
            findCallSites(targetMethod, functionName).forEach(((threeAddressCodeList, indicesOfCallSites) -> inlineCallSite(threeAddressCodeList, indicesOfCallSites, TraceScheduler.flattenIr(method))));
        }
    }

    private List<Instruction> findReplacementBody(List<IrValue> arguments,
                                                  InstructionList functionBody) {
        // replace all instances of arguments with
        // find my actual parameter names
        List<Instruction> newTacList = new ArrayList<>();
        Map<IrValue, IrValue> paramToArg = mapParametersToArguments(functionBody, arguments);
        for (var tac : functionBody) {
            if (tac instanceof MethodEnd || tac instanceof UnconditionalBranch || tac instanceof Method)
                continue;
            if (tac instanceof HasOperand) {
                tac = tac.copy();
                paramToArg.forEach(((HasOperand) tac)::replaceValue);
            }
            newTacList.add(tac);
        }
        return newTacList;
    }

    /**
     * Assumes the indices of the call sites are sorted
     *
     * @param targetTacList
     * @param indicesOfCallSite
     * @param functionBody
     */
    private void inlineCallSite(InstructionList targetTacList, List<Integer> indicesOfCallSite, InstructionList functionBody) {
        if (targetTacList.isEmpty())
            return;
        var replacementBodies = new ArrayList<List<Instruction>>();
        for (var indexOfCallSite : indicesOfCallSite) {
            replacementBodies.add(findReplacementBody(findArgumentNames(targetTacList, indexOfCallSite), functionBody));
        }
        var newTacList = new ArrayList<Instruction>();
        Collections.reverse(indicesOfCallSite);
        int last = targetTacList.size() - 1;

        // we are going in reverse order
        int indexOfReplacementBody = replacementBodies.size() - 1;
        for (var indexOfCallSite : indicesOfCallSite) {
            var subTacList = targetTacList.subList(indexOfCallSite + 1, last + 1);

            Collections.reverse(subTacList);
            newTacList.addAll(subTacList);
            var replacement = replacementBodies.get(indexOfReplacementBody);
            Collections.reverse(replacement);

            if (targetTacList.get(indexOfCallSite) instanceof FunctionCallWithResult methodCallSetResult) {

                assert replacement.get(0) instanceof ReturnInstruction;
                var methodReturn = (ReturnInstruction) replacement.get(0);
                newTacList.add(CopyInstruction.noMetaData(methodCallSetResult.getDestination(), methodReturn.getReturnAddress()
                        .orElseThrow()));

                replacement.remove(0);
            }
            newTacList.addAll(replacement);
            last = indexOfCallSite - 1;
            indexOfReplacementBody--;
        }
        var subTacList = targetTacList
                .subList(0, last + 1);

        Collections.reverse(subTacList);
        newTacList.addAll(subTacList);
        Collections.reverse(newTacList);
        targetTacList.reset(newTacList);

    }

    private boolean isMethodCallAndNameMatches(Instruction instruction, String functionName) {
        return instruction instanceof FunctionCall && ((FunctionCall) instruction).getMethodName()
                .equals(functionName);
    }


    private int getNumberOfCallsToFunction(String functionName) {
        int numberOfCalls = 0;
        for (Method method : methodList) {
            for (Instruction instruction : method.getEntryBlock()
                                                 .getInstructionList()) {
                if (isMethodCallAndNameMatches(instruction, functionName)) {
                    numberOfCalls += 1;
                }
            }
        }
        return numberOfCalls;
    }

    private int getProgramSize() {
        int programSize = 0;
        for (Method method : methodList) {
            programSize += TraceScheduler.flattenIr(method).size();
        }
        return programSize;
    }

    private int getMethodSize(Method functionName) {
        return functionName.getEntryBlock()
                           .getInstructionList().size();
    }

    private boolean isRecursive(Method method) {
        for (Instruction instruction : TraceScheduler.flattenIr(method)) {
            if (isMethodCallAndNameMatches(instruction, method.methodName())) {
                return true;
            }
        }
        return false;
    }


    private boolean hasBranching(Method method) {
        return TraceScheduler.flattenIr(method).toString().contains("if");
    }

    private boolean hasArrayAccesses(Method method) {
        return method.getEntryBlock()
                     .getInstructionList().stream().anyMatch(instruction -> instruction instanceof ArrayBoundsCheck);
    }

    private boolean shouldBeInlined(Method method) {
        if (method.isMain())
            return false;
        if (isRecursive(method))
            return false;
        if (hasBranching(method))
            return false;
        if (hasArrayAccesses(method))
            return false;
        final var functionName = method.methodName();
        final int programSize = getProgramSize();
        final int methodSize = getMethodSize(method);
        final int numberOfCalls = getNumberOfCallsToFunction(functionName);

        return methodSize * numberOfCalls < programSize;
    }
}

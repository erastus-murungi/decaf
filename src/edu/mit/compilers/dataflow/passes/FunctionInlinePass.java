package edu.mit.compilers.dataflow.passes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.FunctionCall;
import edu.mit.compilers.codegen.codes.HasOperand;
import edu.mit.compilers.codegen.codes.Label;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.MethodReturn;
import edu.mit.compilers.codegen.codes.PopParameter;
import edu.mit.compilers.codegen.codes.PushArgument;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.UnconditionalJump;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.analyses.DataFlowAnalysis;


public class FunctionInlinePass {
    List<MethodBegin> methodBeginList;

    public FunctionInlinePass(List<MethodBegin> methodBeginList) {
        this.methodBeginList = methodBeginList;
    }

    public List<MethodBegin> run() {
        List<MethodBegin> newMethodBeginList = new ArrayList<>();
        for (var methodBegin : methodBeginList) {
            if (shouldBeInlined(methodBegin)) {
                inlineMethodBegin(methodBegin);
                continue;
            }
            newMethodBeginList.add(methodBegin);
        }
        return newMethodBeginList;
    }

    private Map<AbstractName, AbstractName> mapParametersToArguments(InstructionList functionTacList,
                                                                     List<AbstractName> arguments) {
        int indexOfCode = 1;
        int functionTacListSize = functionTacList.size();
        var map = new HashMap<AbstractName, AbstractName>();
        var parameters = new ArrayList<AbstractName>();
        while (indexOfCode < functionTacListSize && functionTacList.get(indexOfCode) instanceof PopParameter) {
            parameters.add(((PopParameter) functionTacList.get(indexOfCode)).parameterName);
            indexOfCode++;

        }
        functionTacListSize = parameters.size();
        for (indexOfCode = 0; indexOfCode < functionTacListSize; indexOfCode++) {
            map.put(parameters.get(indexOfCode), arguments.get(indexOfCode));
        }
        return map;
    }

    private Map<InstructionList, List<Integer>> findCallSites(MethodBegin methodBegin, String functionName) {
        var callSites = new HashMap<InstructionList, List<Integer>>();
        for (BasicBlock basicBlock : DataFlowAnalysis.getReversePostOrder(methodBegin.entryBlock)) {
            var threeAddressCodeList = basicBlock.instructionList;
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

    private List<AbstractName> findArgumentNames(InstructionList instructionList, int indexOfFunctionCall) {
        assert instructionList.get(indexOfFunctionCall) instanceof FunctionCall;
        var arguments = new ArrayList<AbstractName>();
        int indexOfPushParameter = indexOfFunctionCall - 1;
        while (indexOfPushParameter >= 0 && instructionList.get(indexOfPushParameter) instanceof PushArgument) {
            PushArgument pushArgument = (PushArgument) instructionList.get(indexOfPushParameter);
            arguments.add(pushArgument.parameterName);
            indexOfPushParameter--;
        }
        return arguments;
    }

    private void inlineMethodBegin(MethodBegin methodBegin) {
        var functionName = methodBegin.methodName();
        for (MethodBegin targetMethodBegin : methodBeginList) {
            if (methodBegin == targetMethodBegin)
                continue;
            findCallSites(targetMethodBegin, functionName).forEach(((threeAddressCodeList, indicesOfCallSites) -> inlineCallSite(threeAddressCodeList, indicesOfCallSites, methodBegin.entryBlock.instructionList)));
        }
    }

    private List<Instruction> findReplacementBody(List<AbstractName> arguments,
                                                  InstructionList functionBody) {
        functionBody = functionBody.flatten();
        // replace all instances of arguments with
        // find my actual parameter names
        List<Instruction> newTacList = new ArrayList<>();
        Map<AbstractName, AbstractName> paramToArg = mapParametersToArguments(functionBody, arguments);
        for (var tac : functionBody) {
            if (tac instanceof PopParameter || tac instanceof MethodEnd || tac instanceof Label || tac instanceof UnconditionalJump || tac instanceof MethodBegin)
                continue;
            if (tac instanceof HasOperand) {
                tac = tac.copy();
                paramToArg.forEach(((HasOperand) tac)::replace);
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

            if (targetTacList.get(indexOfCallSite) instanceof FunctionCallWithResult) {

                assert replacement.get(0) instanceof MethodReturn;
                var methodReturn = (MethodReturn) replacement.get(0);
                var methodCallSetResult = (FunctionCallWithResult) targetTacList.get(indexOfCallSite);
                newTacList.add(Assign.ofRegularAssign(methodCallSetResult.store, methodReturn.getReturnAddress()
                        .orElseThrow()));

                replacement.remove(0);
            }
            newTacList.addAll(replacement);
            last = indexOfCallSite - 1;
            while (last >= 0 && targetTacList.get(last) instanceof PushArgument)
                last--;
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
        for (MethodBegin methodBegin : methodBeginList) {
            for (Instruction instruction : methodBegin.entryBlock.instructionList) {
                if (isMethodCallAndNameMatches(instruction, functionName)) {
                    numberOfCalls += 1;
                }
            }
        }
        return numberOfCalls;
    }

    private int getProgramSize() {
        int programSize = 0;
        for (MethodBegin methodBegin : methodBeginList) {
            programSize += methodBegin.entryBlock.instructionList.flattenedSize();
        }
        return programSize;
    }

    private int getMethodSize(MethodBegin functionName) {
        return functionName.entryBlock.instructionList.size();
    }

    private boolean isRecursive(MethodBegin methodBegin) {
        for (Instruction instruction : methodBegin.entryBlock.instructionList.flatten()) {
            if (isMethodCallAndNameMatches(instruction, methodBegin.methodName())) {
                return true;
            }
        }
        return false;
    }

    private InstructionList getNonEmpty(InstructionList tacList) {
        if (!tacList.isEmpty())
            return tacList;
        while (tacList.getNextInstructionList()
                .isPresent()) {
            tacList = tacList
                    .getNextInstructionList()
                    .get();
            if (!tacList.isEmpty())
                return tacList;
        }
        throw new IllegalArgumentException("all ThreeAddressCodeList's are empty");
    }


    private boolean hasBranching(MethodBegin methodBegin) {
        return methodBegin.entryBlock.instructionList.toString()
                .contains("if");
    }

    private boolean shouldBeInlined(MethodBegin methodBegin) {
        if (methodBegin.isMain())
            return false;
        if (isRecursive(methodBegin))
            return false;
        if (hasBranching(methodBegin))
            return false;
        final var functionName = methodBegin.methodName();
        final int programSize = getProgramSize();
        final int methodSize = getMethodSize(methodBegin);
        final int numberOfCalls = getNumberOfCallsToFunction(functionName);

        return methodSize * numberOfCalls < programSize;
    }
}

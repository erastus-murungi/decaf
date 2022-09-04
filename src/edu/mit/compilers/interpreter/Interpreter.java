package edu.mit.compilers.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.AllocateInstruction;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.codes.CopyInstruction;
import edu.mit.compilers.codegen.codes.FunctionCallNoResult;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.codes.Method;
import edu.mit.compilers.codegen.codes.MethodEnd;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;
import edu.mit.compilers.codegen.codes.UnaryInstruction;
import edu.mit.compilers.codegen.names.Constant;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.StringConstant;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.Variable;
import edu.mit.compilers.utils.Operators;
import edu.mit.compilers.utils.Utils;

public class Interpreter {
    private final Collection<Instruction> instructions;
    List<Instruction> currentMethodInstructions = new ArrayList<>();

    Method currentMethod = null;

    private Constant resolveValue(Value value) {
        if (value instanceof LValue lValue) {
            return globalEnv.get(lValue)
                            .orElseThrow();
        } else {
            return (Constant) value;
        }
    }


    private Constant evalBinaryInstruction(BinaryInstruction binaryInstruction) {
        Long left = ((NumericalConstant) resolveValue(binaryInstruction.fstOperand)).getValue();
        Long right = ((NumericalConstant) resolveValue(binaryInstruction.sndOperand)).getValue();
        var maybeEvaluatedLong = Utils.symbolicallyEvaluate(String.format("%d %s %d", left, binaryInstruction.operator, right));
        return new NumericalConstant(maybeEvaluatedLong.orElseThrow(), Type.Int);
    }

    Map<LValue, Optional<Constant>> globalEnv = new HashMap<>();

    Map<Method, Map<LValue, Optional<Constant>>> localEnvs = new HashMap<>();
    Map<Method, List<Instruction>> methods = new HashMap<>();

    public Interpreter(Collection<Instruction> instructions) {
        this.instructions = instructions;
        var instructionList = new InstructionList();
        instructionList.addAll(instructions);
        System.out.println(instructionList);
    }


    private void evalFunctionCallNoResult(FunctionCallNoResult functionCallNoResult) {
        if (functionCallNoResult.getMethodName()
                                .equals("printf")) {
            var arguments = new ArrayList<>(functionCallNoResult.getArguments());
            String formatString = ((StringConstant) arguments.get(0)).getValue();
            var rest = arguments.subList(1, arguments.size())
                                .stream()
                                .map(this::resolveValue)
                                .map(Constant::getValue)
                                .toArray();
            System.out.format(formatString, rest);
        }
    }

    private NumericalConstant evalUnaryInstruction(UnaryInstruction unaryInstruction) {
        Long operand = ((NumericalConstant) resolveValue(unaryInstruction.operand)).getValue();
        var maybeEvaluatedLong = Utils.symbolicallyEvaluate(String.format("%s(%d)", unaryInstruction.operator, operand));
        return new NumericalConstant(maybeEvaluatedLong.orElseThrow(), Type.Int);
    }

    public void interpret() {
        var env = globalEnv;
        for (Instruction instruction : instructions) {
            if (instruction instanceof MethodEnd) {
                assert currentMethod != null;
                methods.put(currentMethod, currentMethodInstructions);
                currentMethod = null;
                continue;
            }
            if (currentMethod != null) {
                currentMethodInstructions.add(instruction);
                continue;
            }

            if (instruction instanceof Method method) {
                if (method.isMain())
                    continue;
                currentMethod = method;
                currentMethodInstructions = new ArrayList<>();
                localEnvs.put(method, new HashMap<>());
                continue;
            }

            if (instruction instanceof AllocateInstruction allocateInstruction) {
                env.put(allocateInstruction.getDestination(), null);
            } else if (instruction instanceof CopyInstruction copyInstruction) {
                env.put(getStoreOnlyIffExistsInEnv(copyInstruction, env), Optional.of(resolveValue(copyInstruction.getValue())));
            } else if (instruction instanceof BinaryInstruction binaryInstruction) {
                env.put(getStoreOnlyIffExistsInEnv(binaryInstruction, env), Optional.of(evalBinaryInstruction(binaryInstruction)));
            } else if (instruction instanceof FunctionCallNoResult functionCallNoResult) {
                evalFunctionCallNoResult(functionCallNoResult);
            } else if (instruction instanceof UnaryInstruction unaryInstruction) {
                env.put(getStoreOnlyIffExistsInEnv(unaryInstruction, env), Optional.of(evalUnaryInstruction(unaryInstruction)));
            }

        }
    }

    public LValue getStoreOnlyIffExistsInEnv(StoreInstruction storeInstruction, Map<LValue, Optional<Constant>> env) {
        if (!env.containsKey(storeInstruction.getDestination())) {
            throw new IllegalStateException("`" + storeInstruction.getDestination() + "` not found in scope");
        } else {
            return storeInstruction.getDestination();
        }
    }

    public static void test() {
        var a = new Variable("a", Type.Int);
        var b = new Variable("b", Type.Int);
        var c = new Variable("c", Type.Int);

        var name = new Name("printf", null, null);
        List<MethodCallParameter> arguments = List.of(new StringLiteral(null, "%d\n"), new ExpressionParameter(new LocationVariable(new Name("a", null, null))));
        var argsStack = new Stack<Value>();
        argsStack.addAll(
                List.of(new StringConstant(new StringLiteralAllocation("%d\n")), c.copy())
        );
        List<Instruction> instructions = List.of(
                new AllocateInstruction(a),
                new AllocateInstruction(b),
                new AllocateInstruction(c),
                CopyInstruction.noAstConstructor(a.copy(), new NumericalConstant(10L, Type.Int)),
                CopyInstruction.noAstConstructor(b.copy(), a.copy()),
                new BinaryInstruction(c, b, Operators.PLUS, a),
                new FunctionCallNoResult(new MethodCall(name, arguments), argsStack, "")
        );

        Interpreter interpreter = new Interpreter(instructions);
        interpreter.interpret();

    }
}

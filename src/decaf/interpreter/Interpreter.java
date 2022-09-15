package decaf.interpreter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

import decaf.ast.Type;
import decaf.codegen.InstructionList;
import decaf.codegen.codes.Instruction;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.codes.UnaryInstruction;
import decaf.codegen.names.IrAssignableValue;
import decaf.codegen.names.IrConstant;
import decaf.common.Operators;
import decaf.common.Utils;
import decaf.ast.ExpressionParameter;
import decaf.ast.LocationVariable;
import decaf.ast.MethodCall;
import decaf.ast.MethodCallParameter;
import decaf.ast.Name;
import decaf.ast.StringLiteral;
import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.CopyInstruction;
import decaf.codegen.codes.FunctionCallNoResult;
import decaf.codegen.codes.Method;
import decaf.codegen.codes.MethodEnd;
import decaf.codegen.names.IrValue;
import decaf.codegen.names.IrIntegerConstant;
import decaf.codegen.names.IrStringConstant;
import decaf.codegen.names.IrRegister;

public class Interpreter {
    private final Collection<Instruction> instructions;
    List<Instruction> currentMethodInstructions = new ArrayList<>();

    Method currentMethod = null;
    Map<IrAssignableValue, Optional<IrConstant>> globalEnv = new HashMap<>();
    Map<Method, Map<IrAssignableValue, Optional<IrConstant>>> localEnvs = new HashMap<>();
    Map<Method, List<Instruction>> methods = new HashMap<>();

    public Interpreter(Collection<Instruction> instructions) {
        this.instructions = instructions;
        var instructionList = new InstructionList();
        instructionList.addAll(instructions);
        System.out.println(instructionList);
    }

    public static void test() {
        var a = new IrRegister("a", Type.Int);
        var b = new IrRegister("b", Type.Int);
        var c = new IrRegister("c", Type.Int);

        var name = new Name("printf", null, null);
        List<MethodCallParameter> arguments = List.of(new StringLiteral(null, "%d\n"), new ExpressionParameter(new LocationVariable(new Name("a", null, null))));
        var argsStack = new Stack<IrValue>();

        argsStack.add(new IrStringConstant("%d\n"));

        List<Instruction> instructions = List.of(
                CopyInstruction.noAstConstructor(a.copy(), new IrIntegerConstant(10L, Type.Int)),
                CopyInstruction.noAstConstructor(b.copy(), a.copy()),
                new BinaryInstruction(c, b, Operators.PLUS, a),
                new FunctionCallNoResult(new MethodCall(name, arguments), argsStack, "")
        );

        Interpreter interpreter = new Interpreter(instructions);
        interpreter.interpret();

    }

    private IrConstant resolveValue(IrValue irValue) {
        if (irValue instanceof IrRegister irRegister) {
            return globalEnv.get(irRegister)
                    .orElseThrow();
        } else {
            return (IrConstant) irValue;
        }
    }

    private IrConstant evalBinaryInstruction(BinaryInstruction binaryInstruction) {
        Long left = ((IrIntegerConstant) resolveValue(binaryInstruction.fstOperand)).getValue();
        Long right = ((IrIntegerConstant) resolveValue(binaryInstruction.sndOperand)).getValue();
        var maybeEvaluatedLong = Utils.symbolicallyEvaluate(String.format("%d %s %d", left, binaryInstruction.operator, right));
        return new IrIntegerConstant(maybeEvaluatedLong.orElseThrow(), Type.Int);
    }

    private void evalFunctionCallNoResult(FunctionCallNoResult functionCallNoResult) {
        if (functionCallNoResult.getMethodName()
                .equals("printf")) {
            var arguments = new ArrayList<>(functionCallNoResult.getArguments());
            String formatString = ((IrStringConstant) arguments.get(0)).getValue();
            var rest = arguments.subList(1, arguments.size())
                    .stream()
                    .map(this::resolveValue)
                    .map(IrConstant::getValue)
                    .toArray();
            System.out.format(formatString, rest);
        }
    }

    private IrIntegerConstant evalUnaryInstruction(UnaryInstruction unaryInstruction) {
        Long operand = ((IrIntegerConstant) resolveValue(unaryInstruction.operand)).getValue();
        var maybeEvaluatedLong = Utils.symbolicallyEvaluate(String.format("%s(%d)", unaryInstruction.operator, operand));
        return new IrIntegerConstant(maybeEvaluatedLong.orElseThrow(), Type.Int);
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

            if (instruction instanceof CopyInstruction copyInstruction) {
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

    public IrAssignableValue getStoreOnlyIffExistsInEnv(StoreInstruction storeInstruction, Map<IrAssignableValue, Optional<IrConstant>> env) {
        if (!env.containsKey(storeInstruction.getDestination())) {
            throw new IllegalStateException("`" + storeInstruction.getDestination() + "` not found in scope");
        } else {
            return storeInstruction.getDestination();
        }
    }
}

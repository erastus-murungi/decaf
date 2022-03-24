package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class MethodBegin extends ThreeAddressCode{
    String label;
    int sizeOfLocalsAndTemps;
    MethodDefinition methodDefinition;

    public MethodBegin(MethodDefinition methodDefinition) {
        super(methodDefinition);
        this.methodDefinition = methodDefinition;
        this.sizeOfLocalsAndTemps  = getSizeOfLocalsAndTemps(methodDefinition);
    }

    @Override
    public String toString() {
        return methodDefinition.methodName.id + ":\n" + "    " + "BeginFunction " + sizeOfLocalsAndTemps;
    }

    public static int getSizeOfLocalsAndTemps(MethodDefinition methodDefinition) {
        int sum = 0;
        for (FieldDeclaration fieldDeclaration: methodDefinition.block.fieldDeclarationList) {
            int fieldSize;
            if (fieldDeclaration.builtinType == BuiltinType.Int || fieldDeclaration.builtinType == BuiltinType.IntArray) {
                fieldSize = 4;
            } else {
                fieldSize = 1;
            }
            sum += (fieldSize * fieldDeclaration.names.size());
            sum += fieldDeclaration.arrays.stream().map((array -> array.size.convertToLong().intValue() * fieldSize)).reduce(0, Integer::sum);
        }

        List<AST> charLiterals = new ArrayList<>();
        findType(methodDefinition.block, CharLiteral.class, charLiterals);
        sum += (charLiterals.size());
        List<AST> stringLiterals = new ArrayList<>();
        findType(methodDefinition.block, StringLiteral.class, stringLiterals);
        sum += stringLiterals.stream().map(stringLiteral -> stringLiteral.getSourceCode().length()).reduce(0, Integer::sum);

        List<AST> methodCalls = new ArrayList<>();
        findType(methodDefinition.block, MethodCall.class, methodCalls);
        sum += methodCalls.stream().map((methodCall) -> ((MethodCall) methodCall).builtinType.getFieldSize()).reduce(0, Integer::sum);

        List<AST> methodCallStatements = new ArrayList<>();
        findType(methodDefinition.block, MethodCallStatement.class, methodCallStatements);
        sum += methodCallStatements.stream().map((methodCallStatement) -> ((MethodCallStatement) methodCallStatement).methodCall.builtinType.getFieldSize()).reduce(0, Integer::sum);
        return sum;
    }

    public static void findType(AST root, Class<?> tClass, List<AST> nodes) {
        if (root.getClass().equals(tClass)) {
            nodes.add(root);
        }
        if (!root.isTerminal()) {
            for (Pair<String, AST> stringASTPair : root.getChildren()) {
                findType(stringASTPair.second(), tClass, nodes);
            }
        }
    }
}

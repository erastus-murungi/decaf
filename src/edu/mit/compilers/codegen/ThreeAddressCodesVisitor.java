package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ThreeAddressCodesVisitor implements Visitor<ThreeAddressCodeList> {
    @Override
    public ThreeAddressCodeList visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        return new ThreeAddressCodeList(
                decimalLiteral.getSourceCode(),
                Collections.singletonList(new Original(decimalLiteral.literal, decimalLiteral)));
    }

    @Override
    public ThreeAddressCodeList visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        ThreeAddressCodeList threeAddressCodeList = new ThreeAddressCodeList();
        threeAddressCodeList.addCode(new MethodBegin(methodDefinition));

        List<String> newParamNames = new ArrayList<>();
        for (MethodDefinitionParameter methodDefinitionParameter: methodDefinition.methodDefinitionParameterList) {
            threeAddressCodeList.add(methodDefinitionParameter.accept(this, symbolTable));
            newParamNames.add(((DirectAssignment)threeAddressCodeList.getFromLast(0)).dst);
        }

        List<PushParameter> pushParams = new ArrayList<>();
        for (int i = 0; i < newParamNames.size(); i++) {
            final PushParameter pushParameter = new PushParameter(newParamNames.get(i), methodDefinition.methodDefinitionParameterList.get(i));
            threeAddressCodeList.addCode(pushParameter);
            pushParams.add(pushParameter);
        }

//        threeAddressCodeList.add(methodDefinition.block.accept(this, symbolTable));
        for (int i = pushParams.size() - 1; i >= 0; i--) {
            threeAddressCodeList.addCode(new PopParameter(pushParams.get(i).which, methodDefinition.methodDefinitionParameterList.get(i)));
        }

        threeAddressCodeList.addCode(new MethodEnd(methodDefinition));

        System.out.println(threeAddressCodeList);
        return threeAddressCodeList;
    }

    @Override
    public ThreeAddressCodeList visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(For forStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Break breakStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Continue continueStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(While whileStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Program program, SymbolTable symbolTable) {
        for (ImportDeclaration importDeclaration : program.importDeclarationList)
            importDeclaration.accept(this, symbolTable);
        for (FieldDeclaration fieldDeclaration : program.fieldDeclarationList)
            fieldDeclaration.accept(this, symbolTable);
        for (MethodDefinition methodDefinition : program.methodDefinitionList)
            methodDefinition.accept(this, symbolTable);
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        final ThreeAddressCodeList operand = unaryOpExpression.operand.accept(this, symbolTable);
        final ThreeAddressCodeList me = new ThreeAddressCodeList();
        me.add(operand);
        me.addCode(new OneOperandAssignCode(unaryOpExpression, me.place, operand.place, unaryOpExpression.op.getSourceCode()));
        System.out.println(me);
        return me;
    }

    @Override
    public ThreeAddressCodeList visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        final ThreeAddressCodeList left = binaryOpExpression.lhs.accept(this, symbolTable);
        final ThreeAddressCodeList right = binaryOpExpression.rhs.accept(this, symbolTable);

        final ThreeAddressCodeList me = new ThreeAddressCodeList();
        me.add(left);
        me.add(right);
        me.addCode(new TwoOperandAssignCode(binaryOpExpression, me.place, left.place, binaryOpExpression.op.getSourceCode(), right.place));
        System.out.println(me);
        return me;
    }

    @Override
    public ThreeAddressCodeList visit(Block block, SymbolTable symbolTable) {
        for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList) {
            fieldDeclaration.accept(this, symbolTable);
        }
        for (Statement statement : block.statementList) {
            statement.accept(this, symbolTable);
        }
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        return parenthesizedExpression.expression.accept(this, symbolTable);
    }

    @Override
    public ThreeAddressCodeList visit(LocationArray locationArray, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(If ifStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Return returnStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Array array, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(MethodCall methodCall, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        locationAssignExpr.location.accept(this, symbolTable);
        locationAssignExpr.assignExpr.accept(this, symbolTable);
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        assignOpExpr.expression.accept(this, symbolTable);
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        String temporaryVariable = TemporaryNameGenerator.getNextTemporaryVariable();
        return new ThreeAddressCodeList(temporaryVariable,
                Collections.singletonList(new DirectAssignment(methodDefinitionParameter.id.id, temporaryVariable, methodDefinitionParameter)));
    }

    @Override
    public ThreeAddressCodeList visit(Name name, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        return new ThreeAddressCodeList(
                locationVariable.getSourceCode(),
                Collections.singletonList(new Original(locationVariable.name.id, locationVariable)));
    }

    @Override
    public ThreeAddressCodeList visit(Len len, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Increment increment, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Decrement decrement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public ThreeAddressCodeList visit(Initialization initialization, SymbolTable symbolTable) {
        return null;
    }
}

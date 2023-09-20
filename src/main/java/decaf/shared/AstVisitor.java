package decaf.shared;

import decaf.analysis.syntax.ast.*;
import decaf.analysis.syntax.ast.types.Type;


public interface AstVisitor<ReturnType, InputType> {
    ReturnType visit(IntLiteral intLiteral, InputType input);

    ReturnType visit(BooleanLiteral booleanLiteral, InputType input);

    ReturnType visit(FieldDeclaration fieldDeclaration, InputType input);

    ReturnType visit(MethodDefinition methodDefinition, InputType input);

    ReturnType visit(ImportDeclaration importDeclaration, InputType input);

    ReturnType visit(For forStatement, InputType input);

    ReturnType visit(Break breakStatement, InputType input);

    ReturnType visit(Continue continueStatement, InputType input);

    ReturnType visit(While whileStatement, InputType input);

    ReturnType visit(Program program, InputType input);

    ReturnType visit(UnaryOpExpression unaryOpExpression, InputType input);

    ReturnType visit(BinaryOpExpression binaryOpExpression, InputType input);

    ReturnType visit(Block block, InputType input);

    ReturnType visit(ParenthesizedExpression parenthesizedExpression, InputType input);

    ReturnType visit(LocationArray locationArray, InputType input);

    ReturnType visit(ExpressionParameter expressionParameter, InputType input);

    ReturnType visit(If ifStatement, InputType input);

    ReturnType visit(Return returnStatement, InputType input);

    ReturnType visit(Array array, InputType input);

    ReturnType visit(MethodCall methodCall, InputType input);

    ReturnType visit(MethodCallStatement methodCallStatement, InputType input);

    ReturnType visit(LocationAssignExpr locationAssignExpr, InputType input);

    ReturnType visit(AssignOpExpr assignOpExpr, InputType input);

    ReturnType visit(FormalArgument formalArgument, InputType input);

    ReturnType visit(RValue RValue, InputType input);

    ReturnType visit(LocationVariable locationVariable, InputType input);

    ReturnType visit(Len len, InputType input);

    ReturnType visit(Increment increment, InputType input);

    ReturnType visit(Decrement decrement, InputType input);

    ReturnType visit(CharLiteral charLiteral, InputType input);

    ReturnType visit(StringLiteral stringLiteral, InputType input);

    ReturnType visit(CompoundAssignOpExpr compoundAssignOpExpr, InputType input);

    ReturnType visit(Initialization initialization, InputType input);

    ReturnType visit(Assignment assignment, InputType input);

    ReturnType visit(VoidExpression voidExpression, InputType input);

    ReturnType visit(Type type, InputType input);

    ReturnType visit(FormalArguments formalArguments, InputType input);
}

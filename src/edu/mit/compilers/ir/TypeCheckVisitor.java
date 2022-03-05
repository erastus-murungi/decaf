package edu.mit.compilers.ir;

import java.util.Optional;
import java.util.TreeSet;

import edu.mit.compilers.ast.ArithmeticOperator;
import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.AssignOpExpr;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Break;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.CharLiteral;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.ConditionalOperator;
import edu.mit.compilers.ast.Continue;
import edu.mit.compilers.ast.DecimalLiteral;
import edu.mit.compilers.ast.Decrement;
import edu.mit.compilers.ast.EqualityOperator;
import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.FieldDeclaration;
import edu.mit.compilers.ast.For;
import edu.mit.compilers.ast.HexLiteral;
import edu.mit.compilers.ast.If;
import edu.mit.compilers.ast.ImportDeclaration;
import edu.mit.compilers.ast.Increment;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
import edu.mit.compilers.ast.Location;
import edu.mit.compilers.ast.LocationArray;
import edu.mit.compilers.ast.LocationAssignExpr;
import edu.mit.compilers.ast.LocationVariable;
import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.ast.MethodCallParameter;
import edu.mit.compilers.ast.MethodCallStatement;
import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.ast.MethodDefinitionParameter;
import edu.mit.compilers.ast.Name;
import edu.mit.compilers.ast.ParenthesizedExpression;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.ast.RelationalOperator;
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.Statement;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.ast.While;
import edu.mit.compilers.descriptors.ArrayDescriptor;
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.descriptors.MethodDescriptor;
import edu.mit.compilers.exceptions.DecafSemanticException;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.symbolTable.SymbolTable;

public class TypeCheckVisitor implements Visitor<BuiltinType> {
    SymbolTable methods;
    SymbolTable globalFields;
    TreeSet<String> imports;
    BuiltinType returnTypeSeen;

    IntLiteral intLiteral = null;
    boolean negInt = false;

    public TypeCheckVisitor(Program root, SymbolTable methods, SymbolTable globalFields, TreeSet<String> imports) {
        this.methods = methods;
        this.globalFields = globalFields;
        this.imports = imports;
        visit(root, globalFields);
    }

    @Override
    public BuiltinType visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        this.intLiteral = intLiteral;
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        return BuiltinType.Bool;
    }

    @Override
    public BuiltinType visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        intLiteral = decimalLiteral;
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        intLiteral = hexLiteral;
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        for (Array array : fieldDeclaration.arrays)
            array.accept(this, symbolTable);
        return fieldDeclaration.builtinType;
    }

    @Override
    public BuiltinType visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        returnTypeSeen = BuiltinType.Undefined;
        methodDefinition.block.accept(this, methodDefinition.block.blockSymbolTable);
        if (methods.getDescriptorFromCurrentScope(methodDefinition.methodName.id).isPresent()) {
            SymbolTable parameterSymbolTable = ((MethodDescriptor) methods.getDescriptorFromCurrentScope(methodDefinition.methodName.id).get()).parameterSymbolTable;
            for (MethodDefinitionParameter methodDefinitionParameter : methodDefinition.methodDefinitionParameterList)
                methodDefinitionParameter.accept(this, parameterSymbolTable);
            if (returnTypeSeen == BuiltinType.Undefined) {
                if (methodDefinition.returnType != BuiltinType.Void) {
                    exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "missing return statement"));
                }
            } else if (methodDefinition.returnType != returnTypeSeen) {
                exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, methodDefinition.methodName.id + " does not have a declared type " + methodDefinition.returnType + " instead it returns type " + returnTypeSeen));
            }
            returnTypeSeen = BuiltinType.Undefined;
        } else {
            exceptions.add(new DecafSemanticException(methodDefinition.tokenPosition, "method definition not found"));
        }
        return methodDefinition.returnType;
    }

    @Override
    public BuiltinType visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(For forStatement, SymbolTable symbolTable) {
        Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(forStatement.initId.id);
        if (optionalDescriptor.isEmpty())
            exceptions.add(new DecafSemanticException(forStatement.initId.tokenPosition, forStatement.initId + " must be declared in scope"));
        else {
            Descriptor initDescriptor = optionalDescriptor.get();
            if (initDescriptor.type != BuiltinType.Int)
                exceptions.add(new DecafSemanticException(forStatement.initId.tokenPosition, forStatement.initId + " must type must be " + BuiltinType.Int + " not " + initDescriptor.type));

            BuiltinType type = forStatement.initExpression.accept(this, symbolTable);
            if (type != BuiltinType.Int)
                exceptions.add(new DecafSemanticException(forStatement.initExpression.tokenPosition, "init expression must evaluate to an int"));

            BuiltinType testType = forStatement.terminatingCondition.accept(this, symbolTable);
            if (testType != BuiltinType.Bool)
                exceptions.add(new DecafSemanticException(forStatement.terminatingCondition.tokenPosition, "for-loop test must evaluate to " + BuiltinType.Bool + " not " + testType));

            optionalDescriptor = symbolTable.getDescriptorFromValidScopes(forStatement.updatingLocation.name.id);
            if (optionalDescriptor.isEmpty())
                exceptions.add(new DecafSemanticException(forStatement.updatingLocation.tokenPosition, forStatement.updatingLocation.name + " must be declared in scope"));
            else {
                Descriptor updatingDescriptor = optionalDescriptor.get();
                if (updatingDescriptor.type != BuiltinType.Int)
                    exceptions.add(new DecafSemanticException(forStatement.initExpression.tokenPosition, "update location must have type int, not " + updatingDescriptor.type));
                BuiltinType updateExprType = forStatement.updateAssignExpr.accept(this, symbolTable);
                if (forStatement.updateAssignExpr instanceof CompoundAssignOpExpr)
                    updateExprType = forStatement.updateAssignExpr.expression.builtinType;
                if (updateExprType != BuiltinType.Int)
                    exceptions.add(new DecafSemanticException(forStatement.updateAssignExpr.tokenPosition, "incrementing/decrementing must have type int, not " + updateExprType));
            }
        }
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(Break breakStatement, SymbolTable symbolTable) {
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(Continue continueStatement, SymbolTable symbolTable) {
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(While whileStatement, SymbolTable symbolTable) {
        BuiltinType type = whileStatement.test.accept(this, symbolTable);
        if (type != BuiltinType.Bool) {
            exceptions.add(new DecafSemanticException(whileStatement.test.tokenPosition, "while statement test must evaluate to a bool, not " + type));
        }
        whileStatement.body.accept(this, symbolTable);
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(Program program, SymbolTable symbolTable) {
        for (FieldDeclaration fieldDeclaration : program.fieldDeclarationList) {
            fieldDeclaration.accept(this, symbolTable);
        }
        for (MethodDefinition methodDefinition : program.methodDefinitionList) {
            methodDefinition.accept(this, methods);
        }
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        BuiltinType operandType = unaryOpExpression.operand.accept(this, symbolTable);
        if (operandType != BuiltinType.Bool && operandType != BuiltinType.Int) {
            exceptions.add(new DecafSemanticException(unaryOpExpression.tokenPosition, "could not infer"));
        }
        if (operandType != BuiltinType.Bool && unaryOpExpression.op.op.equals("!")) {
            exceptions.add(new DecafSemanticException(unaryOpExpression.tokenPosition, "Can only use a not operator on Bools"));
        }
        if (operandType != BuiltinType.Int && unaryOpExpression.op.op.equals("-")) {
            exceptions.add(new DecafSemanticException(unaryOpExpression.tokenPosition, "Can only use an unary minus operator on Integers"));
        }
        if (operandType == BuiltinType.Int && unaryOpExpression.op.op.equals("-"))
            negInt = !negInt;


        unaryOpExpression.builtinType = operandType;
        return operandType;
    }

    @Override
    public BuiltinType visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        BuiltinType leftType = binaryOpExpression.lhs.accept(this, symbolTable);
        checkIntBounds();
        BuiltinType rightType = binaryOpExpression.rhs.accept(this, symbolTable);
        checkIntBounds();

        BuiltinType type = BuiltinType.Undefined;
        if (leftType != BuiltinType.Undefined && rightType != BuiltinType.Undefined) {
            if (binaryOpExpression.op instanceof ConditionalOperator) {
                if (leftType == BuiltinType.Bool && rightType == BuiltinType.Bool) {
                    type = BuiltinType.Bool;
                } else if (leftType != BuiltinType.Bool) {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "lhs must have type bool not `" + leftType + " `"));
                } else {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "rhs must have type bool not `" + rightType + " `"));
                }
            } else if (binaryOpExpression.op instanceof ArithmeticOperator) {
                if (leftType == BuiltinType.Int && rightType == BuiltinType.Int) {
                    type = BuiltinType.Int;
                } else if (leftType != BuiltinType.Int) {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "lhs must have type int not `" + leftType + " `"));
                } else {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "rhs must have type int not `" + rightType + " `"));
                }
            } else if (binaryOpExpression.op instanceof EqualityOperator) {
                if (leftType.equals(rightType) && (leftType == BuiltinType.Int || leftType == BuiltinType.Bool))
                    type = BuiltinType.Bool;
                else
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "operands of " + binaryOpExpression.op + " must have same type, either both bool or both int, not `" + leftType + "` and `" + rightType));
            } else if (binaryOpExpression.op instanceof RelationalOperator) {
                if (leftType.equals(BuiltinType.Int) && rightType.equals(BuiltinType.Int)) {
                    type = BuiltinType.Bool;
                } else if (leftType != BuiltinType.Int) {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "lhs must have type int not `" + leftType + " `"));
                } else {
                    exceptions.add(new DecafSemanticException(binaryOpExpression.tokenPosition, "rhs must have type int not `" + rightType + " `"));
                }
            }
        }
        binaryOpExpression.builtinType = type;
        return type;
    }

    @Override
    public BuiltinType visit(Block block, SymbolTable symbolTable) {
        for (FieldDeclaration fieldDeclaration : block.fieldDeclarationList)
            fieldDeclaration.accept(this, block.blockSymbolTable);
        for (Statement statement : block.statementList)
            statement.accept(this, block.blockSymbolTable);
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        BuiltinType builtinType = parenthesizedExpression.expression.accept(this, symbolTable);
        checkIntBounds();
        return builtinType;
    }

    @Override
    public BuiltinType visit(LocationArray locationArray, SymbolTable symbolTable) {
        if (locationArray.expression.accept(this, symbolTable) != BuiltinType.Int) {
            exceptions.add(new DecafSemanticException(locationArray.tokenPosition, "array index must evaluate to int"));
        }
        BuiltinType type = BuiltinType.Undefined;
        Optional<Descriptor> descriptor = symbolTable.getDescriptorFromValidScopes(locationArray.name.id);
        if (descriptor.isPresent()) {
            if ((descriptor.get() instanceof ArrayDescriptor)) {
                switch (descriptor.get().type) {
                    case Bool: case BoolArray:
                         type = BuiltinType.Bool;
                         break;
                    case Int: case IntArray:
                        type = BuiltinType.Int;
                        break;
                    default:
                        exceptions.add(new DecafSemanticException(locationArray.tokenPosition, locationArray.name.id + " must be an array"));
                }
            }
        }
        locationArray.builtinType = type;
        return type;
    }

    @Override
    public BuiltinType visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        BuiltinType builtinType = expressionParameter.expression.accept(this, symbolTable);
        expressionParameter.builtinType = builtinType;
        checkIntBounds();
        return builtinType;
    }

    @Override
    public BuiltinType visit(If ifStatement, SymbolTable symbolTable) {
        BuiltinType type = ifStatement.test.accept(this, symbolTable);
        if (type != BuiltinType.Bool) {
            exceptions.add(new DecafSemanticException(ifStatement.test.tokenPosition, "if statement test must evaluate to a bool, not " + type));
        }
        ifStatement.ifBlock.accept(this, symbolTable);
        if (ifStatement.elseBlock != null) {
            ifStatement.elseBlock.accept(this, symbolTable);
        }
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(Return returnStatement, SymbolTable symbolTable) {
        BuiltinType builtinType = BuiltinType.Void;
        if (returnStatement.retExpression != null) {
            builtinType = returnStatement.retExpression.accept(this, symbolTable);
            checkIntBounds();
        }
        returnTypeSeen = builtinType;
        return BuiltinType.Void;
    }

    @Override
    public BuiltinType visit(Array array, SymbolTable symbolTable) {
        if (array.size.convertToLong() <= 0) {
            exceptions.add(new DecafSemanticException(array.size.tokenPosition, "The int_literal in an array declaration must be greater than 0"));
        }
        return BuiltinType.Undefined;
    }

    // The number and types of arguments in a method call
    private void checkNumberOfArgumentsAndTypesMatch(MethodDefinition methodDefinition, MethodCall methodCall) {
        if (methodCall.methodCallParameterList.size() != methodDefinition.methodDefinitionParameterList.size()) {
            exceptions.add(new DecafSemanticException(methodCall.tokenPosition, "unequal number of args"));
            return;
        }
        for (int i = 0; i < methodCall.methodCallParameterList.size(); i++) {
            final MethodDefinitionParameter methodDefinitionParameter = methodDefinition.methodDefinitionParameterList.get(i);
            final MethodCallParameter methodCallParameter = methodCall.methodCallParameterList.get(i);
            if (methodCallParameter.builtinType != methodDefinitionParameter.builtinType) {
                exceptions.add(new DecafSemanticException(methodCall.tokenPosition, "method param " + methodDefinitionParameter.id.id + " is defined with type " + methodDefinitionParameter.builtinType + " but " + methodCallParameter.builtinType + " is passed in"));
            }
        }
    }

    @Override
    public BuiltinType visit(MethodCall methodCall, SymbolTable symbolTable) {
        final Optional<Descriptor> optionalMethodDescriptor = methods.getDescriptorFromCurrentScope(methodCall.nameId.id);
        final Descriptor descriptor;
        if (symbolTable.containsEntry(methodCall.nameId.id)){
            exceptions.add(new DecafSemanticException(methodCall.tokenPosition, methodCall.nameId.id + " refers to locally defined variable"));
            return BuiltinType.Undefined;
        }

        if (imports.contains(methodCall.nameId.id)) {
            // All external functions are treated as if they return int
            return BuiltinType.Int;
        }
        if (optionalMethodDescriptor.isPresent()) {
            descriptor = optionalMethodDescriptor.get();
        } else {
            exceptions.add(new DecafSemanticException(methodCall.tokenPosition, "method " + methodCall.nameId.id + " not found"));
            return BuiltinType.Undefined;
        }
        for (MethodCallParameter methodCallParameter : methodCall.methodCallParameterList) {
            if (methodCallParameter instanceof ExpressionParameter)
                visit(((ExpressionParameter) methodCallParameter), symbolTable);
            else
                visit((StringLiteral) methodCallParameter, symbolTable);
        }
        checkNumberOfArgumentsAndTypesMatch(((MethodDescriptor) descriptor).methodDefinition, methodCall);

        return descriptor.type;
    }

    @Override
    public BuiltinType visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        if (!imports.contains(methodCallStatement.methodCall.nameId.id) && !methods.containsEntry(methodCallStatement.methodCall.nameId.id)) {
            exceptions.add(new DecafSemanticException(methodCallStatement.tokenPosition, "identifier `" + methodCallStatement.methodCall.nameId.id + "` in a method statement must be a declared method or import."));
        }
        methodCallStatement.methodCall.accept(this, symbolTable);
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        Optional<Descriptor> optionalDescriptor = symbolTable.getDescriptorFromValidScopes(locationAssignExpr.location.name.id);
        if (optionalDescriptor.isEmpty() || (locationAssignExpr.location instanceof LocationVariable && optionalDescriptor.get() instanceof ArrayDescriptor))
            exceptions.add(new DecafSemanticException(locationAssignExpr.tokenPosition, "id `" + locationAssignExpr.location.name.id + "` being assigned to must be a declared local/global variable or formal parameter."));
        else {
            if (locationAssignExpr.location instanceof LocationArray) {
                final LocationArray locationArray = (LocationArray) locationAssignExpr.location;
                locationArray.expression.builtinType = locationArray.expression.accept(this, symbolTable);
                if (locationArray.expression.builtinType != BuiltinType.Int) {
                    exceptions.add(new DecafSemanticException(locationAssignExpr.tokenPosition, "array index must evaluate to an int"));
                }
            }
            final BuiltinType expressionType = locationAssignExpr.assignExpr.accept(this, symbolTable);
            final Descriptor locationDescriptor = optionalDescriptor.get();

            if (locationAssignExpr.assignExpr instanceof AssignOpExpr) {
                final AssignOpExpr assignOpExpr = (AssignOpExpr) locationAssignExpr.assignExpr;
                if (assignOpExpr.assignOp.op.equals(DecafScanner.ASSIGN)) {
                    if ((locationDescriptor.type == BuiltinType.Int || locationDescriptor.type == BuiltinType.IntArray) && expressionType != BuiltinType.Int) {
                        exceptions.add(new DecafSemanticException(locationAssignExpr.tokenPosition, "lhs is type " + locationDescriptor.type + " rhs must be type Int, not " + expressionType));
                    }
                    if ((locationDescriptor.type == BuiltinType.Bool || locationDescriptor.type == BuiltinType.BoolArray) && expressionType != BuiltinType.Bool) {
                        exceptions.add(new DecafSemanticException(locationAssignExpr.tokenPosition, "lhs is type " + locationDescriptor.type + " rhs must be type Bool, not " + expressionType));
                    }
                }
            }
             if (assignOperatorEquals(locationAssignExpr, DecafScanner.ADD_ASSIGN)
                               || assignOperatorEquals(locationAssignExpr, DecafScanner.MINUS_ASSIGN)
                               || assignOperatorEquals(locationAssignExpr, DecafScanner.MULTIPLY_ASSIGN)
                               || locationAssignExpr.assignExpr instanceof Decrement
                               || locationAssignExpr.assignExpr instanceof Increment
            ) {
                // both must be of type int
                if (!((locationDescriptor.type == BuiltinType.Int || locationDescriptor.type == BuiltinType.IntArray) && expressionType == BuiltinType.Int)) {
                    exceptions.add(new DecafSemanticException(locationAssignExpr.tokenPosition, "The location and the expression in an incrementing/decrementing assignment must be of type int"));
                }
            }
        }
        return BuiltinType.Undefined;
    }

    private boolean assignOperatorEquals(LocationAssignExpr locationAssignExpr, String opStr) {
        return (locationAssignExpr.assignExpr instanceof AssignOpExpr
                        && ((AssignOpExpr) locationAssignExpr.assignExpr).assignOp.op.equals(opStr))
                       || (locationAssignExpr.assignExpr instanceof CompoundAssignOpExpr
                                   && ((CompoundAssignOpExpr) locationAssignExpr.assignExpr).compoundAssignOp.op.equals(opStr));
    }

    @Override
    public BuiltinType visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        BuiltinType builtinType = assignOpExpr.expression.accept(this, symbolTable);
        checkIntBounds();
        return builtinType;
    }

    @Override
    public BuiltinType visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        return methodDefinitionParameter.builtinType;
    }

    @Override
    public BuiltinType visit(Name name, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public BuiltinType visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        Optional<Descriptor> descriptorOptional = symbolTable.getDescriptorFromValidScopes(locationVariable.name.id);
        if (descriptorOptional.isPresent()) {
            locationVariable.builtinType = descriptorOptional.get().type;
            return descriptorOptional.get().type;
        } else {
            exceptions.add(new DecafSemanticException(locationVariable.tokenPosition, "No identifier can be used before it is declared: " + locationVariable.name.id + " not in scope"));
            return BuiltinType.Undefined;
        }
    }

    @Override
    public BuiltinType visit(Len len, SymbolTable symbolTable) {
        return BuiltinType.Undefined;
    }

    @Override
    public BuiltinType visit(Increment increment, SymbolTable symbolTable) {
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(Decrement decrement, SymbolTable symbolTable) {
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        return BuiltinType.Int;
    }

    @Override
    public BuiltinType visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        return BuiltinType.String;
    }

    @Override
    public BuiltinType visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        compoundAssignOpExpr.expression.builtinType = compoundAssignOpExpr.expression.accept(this, curSymbolTable);
        return BuiltinType.Undefined;
    }

    private void checkIntBounds() {
        if (intLiteral != null) {
            try {
                if (negInt) {
                    System.out.println(intLiteral.literal);
                    Long.parseLong("-" + intLiteral.literal);
                }

                else
                    Long.parseLong(intLiteral.literal);
            }
            catch(Exception e) {
                exceptions.add(new DecafSemanticException(intLiteral.tokenPosition, "Encountered int literal that's out of bounds"));
            }
        }
    }
}

package edu.mit.compilers.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.mit.compilers.ast.Array;
import edu.mit.compilers.ast.AssignOpExpr;
import edu.mit.compilers.ast.Assignment;
import edu.mit.compilers.ast.BinaryOpExpression;
import edu.mit.compilers.ast.Block;
import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Break;
import edu.mit.compilers.ast.CharLiteral;
import edu.mit.compilers.ast.CompoundAssignOpExpr;
import edu.mit.compilers.ast.Continue;
import edu.mit.compilers.ast.DecimalLiteral;
import edu.mit.compilers.ast.Decrement;
import edu.mit.compilers.ast.Expression;
import edu.mit.compilers.ast.ExpressionParameter;
import edu.mit.compilers.ast.FieldDeclaration;
import edu.mit.compilers.ast.For;
import edu.mit.compilers.ast.HexLiteral;
import edu.mit.compilers.ast.If;
import edu.mit.compilers.ast.ImportDeclaration;
import edu.mit.compilers.ast.Increment;
import edu.mit.compilers.ast.Initialization;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.ast.Len;
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
import edu.mit.compilers.ast.Return;
import edu.mit.compilers.ast.Statement;
import edu.mit.compilers.ast.StringLiteral;
import edu.mit.compilers.ast.UnaryOpExpression;
import edu.mit.compilers.ast.While;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;


public class ControlFlowGraphVisitor implements Visitor<BasicBlocksPair> {
    public BasicBlock global = BasicBlock.noBranch();
    public HashMap<String, BasicBlock> methodNameToEntryBlock = new HashMap<>();
    public HashMap<String, NOP> methodNameToExitNop = new HashMap<>();

    public Stack<List<BasicBlock>> loopToBreak = new Stack<>(); // a bunch of break blocks to point to the right place
    public Stack<BasicBlock> continueBlocks = new Stack<>(); // a bunch of continue blocks to point to the right place

    /**
     * We need a global NOP which represents the end of all computation in a method
     */
    private NOP exitNop;

    public ControlFlowGraphVisitor() {
    }

    public static Expression rotateBinaryOpExpression(Expression expr) {
        if (expr instanceof BinaryOpExpression) {
            if (((BinaryOpExpression) expr).rhs instanceof BinaryOpExpression rhsTemp) {
                if (BinaryOpExpression.operatorPrecedence.get(((BinaryOpExpression) expr).op.getSourceCode()).equals(BinaryOpExpression.operatorPrecedence.get(rhsTemp.op.getSourceCode()))) {
                    ((BinaryOpExpression) expr).rhs = rhsTemp.lhs;
                    rhsTemp.lhs = expr;
                    ((BinaryOpExpression) expr).lhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
                    ((BinaryOpExpression) expr).rhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
                    return rotateBinaryOpExpression(rhsTemp);
                }
            }
            ((BinaryOpExpression) expr).lhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
            ((BinaryOpExpression) expr).rhs = rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
        } else if (expr instanceof ParenthesizedExpression) {
            rotateBinaryOpExpression(((ParenthesizedExpression) expr).expression);
        } else if (expr instanceof MethodCall) {
            for (int i = 0; i < ((MethodCall) expr).methodCallParameterList.size(); i++) {
                MethodCallParameter param = ((MethodCall) expr).methodCallParameterList.get(i);
                if (param instanceof ExpressionParameter) {
                    ((MethodCall) expr).methodCallParameterList.set(i, new ExpressionParameter(rotateBinaryOpExpression(((ExpressionParameter) param).expression)));
                }
            }
        } else if (expr instanceof LocationArray) {
            rotateBinaryOpExpression(((LocationArray) expr).expression);
        } else if (expr instanceof UnaryOpExpression) {
            rotateBinaryOpExpression(((UnaryOpExpression) expr).operand);
        }
        return expr;
    }

    @Override
    public BasicBlocksPair visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + intLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + booleanLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + decimalLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + hexLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        // multiple fields can be declared in same line, handle/flatten later
        BasicBlock fieldDecl = BasicBlock.noBranch();
        fieldDecl.getAstNodes().add(fieldDeclaration);
        return new BasicBlocksPair(fieldDecl, fieldDecl);
    }

    @Override
    public BasicBlocksPair visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        BasicBlock initial = BasicBlock.noBranch();
        BasicBlocksPair curPair = new BasicBlocksPair(initial, new NOP());
        initial.setSuccessor(curPair.endBlock);
        curPair.startBlock.setSuccessor(curPair.endBlock);
        for (MethodDefinitionParameter param : methodDefinition.parameterList) {
            BasicBlocksPair placeholder = param.accept(this, symbolTable);
            curPair.endBlock.setSuccessor(placeholder.startBlock);
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        BasicBlocksPair methodBody = methodDefinition.block.accept(this, symbolTable);
        curPair.endBlock.setSuccessor(methodBody.startBlock);
        methodBody.startBlock.addPredecessor(curPair.endBlock);
        return new BasicBlocksPair(initial, methodBody.endBlock);
    }

    @Override
    public BasicBlocksPair visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        BasicBlock import_ = BasicBlock.noBranch();
        import_.addAstNode(importDeclaration);
        return new BasicBlocksPair(import_, import_);
    }

    @Override
    public BasicBlocksPair visit(For forStatement, SymbolTable symbolTable) {
        loopToBreak.push(new ArrayList<>());
        // If false, end with NOP, also end of for_statement
        NOP falseBlock = new NOP("For Loop (false) " + forStatement.terminatingCondition.getSourceCode());
        NOP exit = new NOP("exit_for");
        falseBlock.setSuccessor(exit);
        exit.addPredecessor(falseBlock);

        // For the block, the child of that CFGBlock should be a block with the increment line
        BasicBlock incrementBlock = BasicBlock.noBranch();
        incrementBlock.addAstNode(forStatement.update);

        // Evaluate the condition
        final Expression condition = rotateBinaryOpExpression(forStatement.terminatingCondition);
        var evaluateBlock = ShortCircuitProcessor.shortCircuit(BasicBlock.branch(condition, exitNop, exitNop));
        incrementBlock.setSuccessor(evaluateBlock);
        evaluateBlock.addPredecessor(incrementBlock);

        // In for loops, continue should point to an incrementBlock
        continueBlocks.push(incrementBlock);

        // If true, run the block.
        BasicBlocksPair truePair = forStatement.block.accept(this, symbolTable);

        evaluateBlock.setFalseTarget(falseBlock);
        evaluateBlock.getFalseTarget()
                .addPredecessor(evaluateBlock);

        evaluateBlock.setTrueTarget(truePair.startBlock);
        truePair.startBlock.addPredecessor(evaluateBlock);

        if (truePair.endBlock != exitNop) {
            truePair.endBlock.setSuccessor(incrementBlock);
            incrementBlock.addPredecessor(truePair.endBlock);
        }
        // Initialize the condition variable
        BasicBlock initializeBlock = BasicBlock.noBranch();
        initializeBlock.addAstNode(forStatement.initialization);

        // child of initialization block is evaluation
        initializeBlock.setSuccessor(evaluateBlock);
        evaluateBlock.addPredecessor(initializeBlock);

        // Child of that increment block should be the evaluation
        incrementBlock.setSuccessor(evaluateBlock);
        evaluateBlock.addPredecessor(incrementBlock);

        handleBreaksInLoops(falseBlock);
        continueBlocks.pop();
        return new BasicBlocksPair(initializeBlock, exit, false);
    }

    private void handleBreaksInLoops(BasicBlock cfgBlock) {
        List<BasicBlock> toRemove = new ArrayList<>();
        List<BasicBlock> breakBlocks = loopToBreak.pop();
        if (!breakBlocks.isEmpty()) {
            for (BasicBlock breakBlock : breakBlocks) {
                breakBlock.setSuccessor(cfgBlock);
                toRemove.add(breakBlock);
                cfgBlock.addPredecessor(breakBlock);
            }
        }
        for (BasicBlock breakBlock : toRemove)
            breakBlocks.remove(breakBlock);
    }

    @Override
    public BasicBlocksPair visit(Break breakStatement, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + breakStatement.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Continue continueStatement, SymbolTable symbolTable) {
        // unreachable: it's equivalent to returning a NOP
        // Any loop node which sends a visitor to a block should handle continue logic
        throw new IllegalStateException("we cannot visit " + continueStatement.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(While whileStatement, SymbolTable symbolTable) {
        loopToBreak.push(new ArrayList<>());
        // If false, end with NOP, also end of while
        NOP falseBlock = new NOP();

        // Evaluate the condition
        Expression test = rotateBinaryOpExpression(whileStatement.test);
        BasicBlock conditionExpr = BasicBlock.branch(test, exitNop, exitNop);
        conditionExpr.setFalseTarget(falseBlock);
        falseBlock.addPredecessor(conditionExpr);

        // In for loops, continue should point to the evaluation expression
        continueBlocks.push(conditionExpr);

        // If true, run the block.
        BasicBlocksPair truePair = whileStatement.body.accept(this, symbolTable);

        conditionExpr.setTrueTarget(truePair.startBlock);
        conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        if (truePair.endBlock != null) {
            truePair.endBlock.setSuccessor(conditionExpr);
            conditionExpr.addPredecessor(truePair.endBlock);
        }

        handleBreaksInLoops(falseBlock);
        continueBlocks.pop();
        return new BasicBlocksPair(conditionExpr, falseBlock);
    }

    @Override
    public BasicBlocksPair visit(Program program, SymbolTable symbolTable) {
        var curPair = new BasicBlocksPair(global, new NOP("global NOP"));
        global.setSuccessor(curPair.endBlock);
        for (var import_ : program.importDeclarationList) {
            BasicBlocksPair placeholder = import_.accept(this, symbolTable);
            curPair.endBlock.setSuccessor(placeholder.startBlock);
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        for (var field : program.fieldDeclarationList) {
            BasicBlocksPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.setSuccessor(placeholder.startBlock);
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        for (var method : program.methodDefinitionList) {
            exitNop = new NOP("exit_" + method.methodName.getLabel());
            methodNameToEntryBlock.put(method.methodName.getLabel(), method.accept(this, symbolTable).startBlock);
            methodNameToExitNop.put(method.methodName.getLabel(), exitNop);
        }
        // don't need to return pair bc only need start block
        return null;
    }

    @Override
    public BasicBlocksPair visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + unaryOpExpression.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + binaryOpExpression.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Block block, SymbolTable symbolTable) {
        NOP initial = new NOP();
        NOP exit = new NOP();
        BasicBlocksPair curPair = new BasicBlocksPair(initial, new NOP());
        initial.setSuccessor(curPair.endBlock);

        for (FieldDeclaration field : block.fieldDeclarationList) {
            BasicBlocksPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.setSuccessor(placeholder.startBlock);
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }

        for (Statement statement : block.statementList) {
            if (statement instanceof Continue) {
                // will return a NOP() for sure because Continue blocks should be pointers back to the evaluation block
                BasicBlock continueCfg = new NOP();
                BasicBlock nextBlock = continueBlocks.peek();
                continueCfg.setSuccessor(nextBlock);
                nextBlock.addPredecessor(continueCfg);
                continueCfg.addPredecessor(curPair.endBlock);
                curPair.endBlock.setSuccessor(continueCfg);
                return new BasicBlocksPair(initial, continueCfg);
            }
            if (statement instanceof Break) {
                // a break is not a real block either
                BasicBlock breakCfg = new NOP("Break");
                loopToBreak.peek().add(breakCfg);
                breakCfg.addPredecessor(curPair.endBlock);
                curPair.endBlock.setSuccessor(breakCfg);
                return new BasicBlocksPair(initial, breakCfg, false);
            }
            if (statement instanceof Return) {
                BasicBlocksPair returnPair = statement.accept(this, symbolTable);
                curPair.endBlock.setSuccessor(returnPair.startBlock);
                returnPair.startBlock.addPredecessor(curPair.endBlock);
                return new BasicBlocksPair(initial, returnPair.endBlock, false);
            }
            // recurse normally for other cases
            else {
                BasicBlocksPair placeholder = statement.accept(this, symbolTable);
                curPair.endBlock.setSuccessor(placeholder.startBlock);
                placeholder.startBlock.addPredecessor(curPair.endBlock);
                curPair = placeholder;
            }
        }
        curPair.endBlock.setSuccessor(exit);
        exit.addPredecessor(curPair.endBlock);
        return new BasicBlocksPair(initial, exit, false);
    }

    @Override
    public BasicBlocksPair visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        // unreachable (expr)
        throw new IllegalStateException("we cannot visit " + parenthesizedExpression.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(LocationArray locationArray, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + locationArray.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + expressionParameter.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(If ifStatement, SymbolTable symbolTable) {
        // always end with nop
        final NOP exit = new NOP();

        // If true, run the block.
        BasicBlocksPair truePair = ifStatement.ifBlock.accept(this, symbolTable);
        if (truePair.endBlock.getSuccessor() == null) {
            // handling the cases when we have a "Continue" statement
            truePair.endBlock.setSuccessor(exit);
            exit.addPredecessor(truePair.endBlock);
        }

        // Evaluate the condition
        final Expression condition = rotateBinaryOpExpression(ifStatement.test);

        BasicBlock conditionExpr;
        if (ifStatement.elseBlock != null) {
            BasicBlocksPair falsePair = ifStatement.elseBlock.accept(this, symbolTable);
            if (falsePair.endBlock.getSuccessor() == null) {
                // handling the cases when we have a "Continue" statement
                falsePair.endBlock.setSuccessor(exit);
                exit.addPredecessor(falsePair.endBlock);
            }
            conditionExpr = BasicBlock.branch(condition, truePair.startBlock, falsePair.startBlock);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
            falsePair.startBlock.addPredecessor(conditionExpr);
        } else {
            conditionExpr = BasicBlock.branch(condition, truePair.startBlock, exit);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        }
        truePair.startBlock.addPredecessor(conditionExpr);
        return new BasicBlocksPair(ShortCircuitProcessor.shortCircuit(conditionExpr), exit);
    }

    @Override
    public BasicBlocksPair visit(Return returnStatement, SymbolTable symbolTable) {
        BasicBlock returnBlock = BasicBlock.noBranch();
        returnStatement.retExpression = rotateBinaryOpExpression(returnStatement.retExpression);
        returnBlock.addAstNode(returnStatement);
        returnBlock.setSuccessor(exitNop);
        return new BasicBlocksPair(returnBlock, exitNop);
    }

    @Override
    public BasicBlocksPair visit(Array array, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + array.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(MethodCall methodCall, SymbolTable symbolTable) {
        // unreachable - handle later in assembly gen
        throw new IllegalStateException("we cannot visit " + methodCall.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        BasicBlock methodCallExpr = BasicBlock.noBranch();
        for (int i = 0; i < methodCallStatement.methodCall.methodCallParameterList.size(); i++) {
            MethodCallParameter param = methodCallStatement.methodCall.methodCallParameterList.get(i);
            if (param instanceof ExpressionParameter expressionParameter) {
                expressionParameter.expression = rotateBinaryOpExpression(expressionParameter.expression);
                methodCallStatement.methodCall.methodCallParameterList.set(i, param);
            }
        }
        methodCallExpr.addAstNode(methodCallStatement);
        return new BasicBlocksPair(methodCallExpr, methodCallExpr);
    }

    @Override
    public BasicBlocksPair visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        final BasicBlock assignment = BasicBlock.noBranch();
        locationAssignExpr.assignExpr.expression = rotateBinaryOpExpression(locationAssignExpr.assignExpr.expression);

        String op;
        if (locationAssignExpr.assignExpr instanceof final AssignOpExpr assignOpExpr) {
            op = assignOpExpr.assignOp.label;
        } else if (locationAssignExpr.assignExpr instanceof final CompoundAssignOpExpr assignOpExpr) {
            op = assignOpExpr.compoundAssignOp.label;
        } else if (locationAssignExpr.assignExpr instanceof Decrement) {
            op = DecafScanner.DECREMENT;
        } else if (locationAssignExpr.assignExpr instanceof Increment) {
            op = DecafScanner.INCREMENT;
        } else {
            throw new IllegalStateException("unrecognized AST node " + locationAssignExpr.assignExpr);
        }

        assignment.addAstNode(new Assignment(locationAssignExpr.location, locationAssignExpr.assignExpr, op));
        return new BasicBlocksPair(assignment, assignment);
    }

    @Override
    public BasicBlocksPair visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // unreachable - should've been combined into an Assignment elsewhere
        throw new IllegalStateException("we cannot visit " + assignOpExpr.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        BasicBlock methodParam = BasicBlock.noBranch();
        methodParam.setSuccessor(methodParam);
        methodParam.addAstNode(methodDefinitionParameter);
        return new BasicBlocksPair(methodParam, methodParam);
    }

    @Override
    public BasicBlocksPair visit(Name name, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + name.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + locationVariable.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Len len, SymbolTable symbolTable) {
        // unreachable (expr)
        throw new IllegalStateException("we cannot visit " + len.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Increment increment, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + increment.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Decrement decrement, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + decrement.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + charLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + stringLiteral.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + compoundAssignOpExpr.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Initialization initialization, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + initialization.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(Assignment assignment, SymbolTable symbolTable) {
        return null;
    }
}

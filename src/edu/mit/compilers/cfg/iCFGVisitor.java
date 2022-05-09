package edu.mit.compilers.cfg;

import java.util.*;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;


public class iCFGVisitor implements Visitor<BasicBlocksPair> {
    public BasicBlockBranchLess initialGlobalBlock = new BasicBlockBranchLess();
    public HashMap<String, BasicBlock> methodCFGBlocks = new HashMap<>();
    public HashMap<String, NOP> methodToExitNOP = new HashMap<>();

    public Stack<List<BasicBlockBranchLess>> loopToBreak = new Stack<>(); // a bunch of break blocks to point to the right place
    public Stack<BasicBlock> continueBlocks = new Stack<>(); // a bunch of continue blocks to point to the right place

    /**
     * We need a global NOP which represents the end of all computation in a method
     */
    private NOP exitNOP;

    public iCFGVisitor() {
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
        BasicBlockBranchLess fieldDecl = new BasicBlockBranchLess();
        fieldDecl.lines.add(fieldDeclaration);
        return new BasicBlocksPair(fieldDecl, fieldDecl);
    }

    @Override
    public BasicBlocksPair visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        BasicBlockBranchLess initial = new BasicBlockBranchLess();
        BasicBlocksPair curPair = new BasicBlocksPair(initial, new NOP());
        initial.autoChild = curPair.endBlock;
        ((BasicBlockBranchLess) curPair.startBlock).autoChild = curPair.endBlock;
        for (MethodDefinitionParameter param : methodDefinition.methodDefinitionParameterList) {
            BasicBlocksPair placeholder = param.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        BasicBlocksPair methodBody = methodDefinition.block.accept(this, symbolTable);
        curPair.endBlock.autoChild = methodBody.startBlock;
        methodBody.startBlock.addPredecessor(curPair.endBlock);
        return new BasicBlocksPair(initial, methodBody.endBlock);
    }

    @Override
    public BasicBlocksPair visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        BasicBlockBranchLess import_ = new BasicBlockBranchLess();
        import_.lines.add(importDeclaration);
        return new BasicBlocksPair(import_, import_);
    }

    @Override
    public BasicBlocksPair visit(For forStatement, SymbolTable symbolTable) {
        loopToBreak.push(new ArrayList<>());
        // If false, end with NOP, also end of for_statement
        NOP falseBlock = new NOP("For Loop (false) " + forStatement.terminatingCondition.getSourceCode());
        NOP exit = new NOP("Exit For");
        falseBlock.autoChild = exit;
        exit.addPredecessor(falseBlock);

        // For the block, the child of that CFGBlock should be a block with the increment line
        BasicBlockBranchLess incrementBlock = new BasicBlockBranchLess();
        incrementBlock.lines.add(forStatement.update);

        // Evaluate the condition
        final Expression condition = rotateBinaryOpExpression(forStatement.terminatingCondition);
        BasicBlockWithBranch evaluateBlock = ShortCircuitProcessor.shortCircuit(new BasicBlockWithBranch(condition));
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.addPredecessor(incrementBlock);

        // In for loops, continue should point to an incrementBlock
        continueBlocks.push(incrementBlock);

        // If true, run the block.
        BasicBlocksPair truePair = forStatement.block.accept(this, symbolTable);

        evaluateBlock.falseChild = falseBlock;
        evaluateBlock.falseChild.addPredecessor(evaluateBlock);

        evaluateBlock.trueChild = truePair.startBlock;
        truePair.startBlock.addPredecessor(evaluateBlock);

        if (truePair.endBlock != exitNOP) {
            truePair.endBlock.autoChild = incrementBlock;
            incrementBlock.addPredecessor(truePair.endBlock);
        }
        // Initialize the condition variable
        BasicBlockBranchLess initializeBlock = new BasicBlockBranchLess();
        initializeBlock.lines.add(forStatement.initialization);

        // child of initialization block is evaluation
        initializeBlock.autoChild = evaluateBlock;
        evaluateBlock.addPredecessor(initializeBlock);

        // Child of that increment block should be the evaluation
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.addPredecessor(incrementBlock);

        handleBreaksInLoops(falseBlock);
        continueBlocks.pop();
        return new BasicBlocksPair(initializeBlock, exit, false);
    }

    private void handleBreaksInLoops(BasicBlock cfgBlock) {
        List<BasicBlockBranchLess> toRemove = new ArrayList<>();
        List<BasicBlockBranchLess> breakBlocks = loopToBreak.pop();
        if (!breakBlocks.isEmpty()) {
            for (BasicBlockBranchLess breakBlock: breakBlocks) {
                breakBlock.autoChild = cfgBlock;
                toRemove.add(breakBlock);
                cfgBlock.addPredecessor(breakBlock);
            }
        }
        for (BasicBlockBranchLess breakBlock: toRemove)
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
        BasicBlockWithBranch conditionExpr = new BasicBlockWithBranch(test);
        conditionExpr.falseChild = falseBlock;
        falseBlock.addPredecessor(conditionExpr);

        // In for loops, continue should point to the evaluation expression
        continueBlocks.push(conditionExpr);

        // If true, run the block.
        BasicBlocksPair truePair = whileStatement.body.accept(this, symbolTable);

        conditionExpr.trueChild = truePair.startBlock;
        conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        if (truePair.endBlock != null) {
            truePair.endBlock.autoChild = conditionExpr;
            conditionExpr.addPredecessor(truePair.endBlock);
        }

        handleBreaksInLoops(falseBlock);
        continueBlocks.pop();
        return new BasicBlocksPair(conditionExpr, falseBlock);
    }

    @Override
    public BasicBlocksPair visit(Program program, SymbolTable symbolTable) {
        BasicBlocksPair curPair = new BasicBlocksPair(initialGlobalBlock, new NOP("global NOP"));
        initialGlobalBlock.autoChild = curPair.endBlock;
        for (ImportDeclaration import_ : program.importDeclarationList) {
            BasicBlocksPair placeholder = import_.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        for (FieldDeclaration field : program.fieldDeclarationList) {
            BasicBlocksPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }
        for (MethodDefinition method : program.methodDefinitionList) {
            exitNOP = new NOP("Exit " +  method.methodName.id);
            methodCFGBlocks.put(method.methodName.id, method.accept(this, symbolTable).startBlock);
            methodToExitNOP.put(method.methodName.id, exitNOP);
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
        initial.autoChild = curPair.endBlock;

        for (FieldDeclaration field : block.fieldDeclarationList) {
            BasicBlocksPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.addPredecessor(curPair.endBlock);
            curPair = placeholder;
        }

        for (Statement statement : block.statementList) {
            if (statement instanceof Continue) {
                // will return a NOP() for sure because Continue blocks should be pointers back to the evaluation block
                BasicBlockBranchLess continueCfg = new NOP();
                BasicBlock nextBlock = continueBlocks.peek();
                continueCfg.autoChild = nextBlock;
                nextBlock.addPredecessor(continueCfg);
                continueCfg.addPredecessor(curPair.endBlock);
                curPair.endBlock.autoChild = continueCfg;
                return new BasicBlocksPair(initial, continueCfg);
            }
            if (statement instanceof Break) {
                // a break is not a real block either
                BasicBlockBranchLess breakCfg = new NOP("Break");
                loopToBreak.peek().add(breakCfg);
                breakCfg.addPredecessor(curPair.endBlock);
                curPair.endBlock.autoChild = breakCfg;
                return new BasicBlocksPair(initial, breakCfg, false);
            }
            if (statement instanceof Return) {
                BasicBlocksPair returnPair = statement.accept(this, symbolTable);
                curPair.endBlock.autoChild = returnPair.startBlock;
                returnPair.startBlock.addPredecessor(curPair.endBlock);
                return new BasicBlocksPair(initial, returnPair.endBlock, false);
            }
            // recurse normally for other cases
            else {
                BasicBlocksPair placeholder = statement.accept(this, symbolTable);
                curPair.endBlock.autoChild = placeholder.startBlock;
                placeholder.startBlock.addPredecessor(curPair.endBlock);
                curPair = placeholder;
            }
        }
        curPair.endBlock.autoChild = exit;
        exit.addPredecessor(curPair.endBlock);
        return new BasicBlocksPair(initial, exit, false);
    }

    @Override
    public BasicBlocksPair visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        // unreachable (expr)
        throw new IllegalStateException("we cannot visit " + parenthesizedExpression.getClass().getSimpleName());}

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
        if (truePair.endBlock.autoChild == null) {
            // handling the cases when we have a "Continue" statement
            truePair.endBlock.autoChild = exit;
            exit.addPredecessor(truePair.endBlock);
        }

        // Evaluate the condition
        final Expression condition = rotateBinaryOpExpression(ifStatement.test);

        BasicBlockWithBranch conditionExpr;
        if (ifStatement.elseBlock != null) {
            BasicBlocksPair falsePair = ifStatement.elseBlock.accept(this, symbolTable);
            if (falsePair.endBlock.autoChild == null) {
                // handling the cases when we have a "Continue" statement
                falsePair.endBlock.autoChild = exit;
                exit.addPredecessor(falsePair.endBlock);
            }
            conditionExpr = new BasicBlockWithBranch(condition, truePair.startBlock, falsePair.startBlock);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
            falsePair.startBlock.addPredecessor(conditionExpr);
        } else {
            conditionExpr = new BasicBlockWithBranch(condition, truePair.startBlock, exit);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        }
        truePair.startBlock.addPredecessor(conditionExpr);
        return new BasicBlocksPair(ShortCircuitProcessor.shortCircuit(conditionExpr), exit);
    }

    @Override
    public BasicBlocksPair visit(Return returnStatement, SymbolTable symbolTable) {
        BasicBlockBranchLess returnBlock = new BasicBlockBranchLess();
        returnStatement.retExpression = rotateBinaryOpExpression(returnStatement.retExpression);
        returnBlock.lines.add(returnStatement);
        returnBlock.autoChild = exitNOP;
        return new BasicBlocksPair(returnBlock, exitNOP);
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
        BasicBlockBranchLess methodCallExpr = new BasicBlockBranchLess();
        for (int i = 0; i < methodCallStatement.methodCall.methodCallParameterList.size(); i++) {
            MethodCallParameter param = methodCallStatement.methodCall.methodCallParameterList.get(i);
            if (param instanceof ExpressionParameter) {
                ExpressionParameter expressionParameter = ((ExpressionParameter) param);
                expressionParameter.expression = rotateBinaryOpExpression(expressionParameter.expression);
                methodCallStatement.methodCall.methodCallParameterList.set(i, param);
            }
        }
        methodCallExpr.lines.add(methodCallStatement);
        return new BasicBlocksPair(methodCallExpr, methodCallExpr);
    }

    @Override
    public BasicBlocksPair visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        final BasicBlockBranchLess assignment = new BasicBlockBranchLess();
        locationAssignExpr.assignExpr.expression = rotateBinaryOpExpression(locationAssignExpr.assignExpr.expression);

        String op;
        if (locationAssignExpr.assignExpr instanceof AssignOpExpr) {
            final AssignOpExpr assignOpExpr = (AssignOpExpr) locationAssignExpr.assignExpr;
            op = assignOpExpr.assignOp.op;
        } else if (locationAssignExpr.assignExpr instanceof CompoundAssignOpExpr) {
            final CompoundAssignOpExpr assignOpExpr = (CompoundAssignOpExpr) locationAssignExpr.assignExpr;
            op = assignOpExpr.compoundAssignOp.op;
        } else if (locationAssignExpr.assignExpr instanceof Decrement) {
            op = DecafScanner.DECREMENT;
        } else if (locationAssignExpr.assignExpr instanceof Increment) {
            op = DecafScanner.INCREMENT;
        } else {
            throw new IllegalStateException("unrecognized AST node " + locationAssignExpr.assignExpr);
        }

        assignment.lines.add(new Assignment(locationAssignExpr.location, locationAssignExpr.assignExpr, op));
        return new BasicBlocksPair(assignment, assignment);
    }

    @Override
    public BasicBlocksPair visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // unreachable - should've been combined into an Assignment elsewhere
        throw new IllegalStateException("we cannot visit " + assignOpExpr.getClass().getSimpleName());
    }

    @Override
    public BasicBlocksPair visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        BasicBlockBranchLess methodParam = new BasicBlockBranchLess();
        methodParam.autoChild = methodParam;
        methodParam.lines.add(methodDefinitionParameter);
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

    public static Expression rotateBinaryOpExpression(Expression expr) {
        if (expr instanceof BinaryOpExpression) {
            if (((BinaryOpExpression) expr).rhs instanceof  BinaryOpExpression) {
                BinaryOpExpression rhsTemp = (BinaryOpExpression) ((BinaryOpExpression) expr).rhs;
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
        }
        else if (expr instanceof ParenthesizedExpression) {
            rotateBinaryOpExpression(((ParenthesizedExpression) expr).expression);
        }
        else if (expr instanceof MethodCall) {
            for (int i = 0; i < ((MethodCall) expr).methodCallParameterList.size(); i++) {
                MethodCallParameter param = ((MethodCall) expr).methodCallParameterList.get(i);
                if (param instanceof ExpressionParameter) {
                    ((MethodCall) expr).methodCallParameterList.set(i, new ExpressionParameter(rotateBinaryOpExpression(((ExpressionParameter) param).expression)));
                }
            }
        }
        else if (expr instanceof LocationArray) {
            rotateBinaryOpExpression(((LocationArray) expr).expression);
        }
        else if (expr instanceof UnaryOpExpression) {
            rotateBinaryOpExpression(((UnaryOpExpression) expr).operand);
        }
        return expr;
    }
}

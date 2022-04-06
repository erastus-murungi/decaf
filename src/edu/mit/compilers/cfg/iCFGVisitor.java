package edu.mit.compilers.cfg;

import java.util.*;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

import static edu.mit.compilers.grammar.DecafParser.printParseTree;

public class iCFGVisitor implements Visitor<CFGPair> {
    public CFGNonConditional initialGlobalBlock = new CFGNonConditional();
    public HashMap<String, CFGBlock> methodCFGBlocks = new HashMap<>();

    public Stack<List<CFGNonConditional>> loopToBreak = new Stack<>(); // a bunch of break blocks to point to the right place
    public Stack<CFGBlock> continueBlocks = new Stack<>(); // a bunch of continue blocks to point to the right place

    /**
     * We need a global NOP which represents the end of all computation in a method
     */
    private final NOP exitNOP;

    public iCFGVisitor() {
        exitNOP = new NOP();
    }

    @Override
    public CFGPair visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + intLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + booleanLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + decimalLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + hexLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(FieldDeclaration fieldDeclaration, SymbolTable symbolTable) {
        // multiple fields can be declared in same line, handle/flatten later
        CFGNonConditional fieldDecl = new CFGNonConditional();
        fieldDecl.lines.add(new CFGDeclaration(fieldDeclaration));
        return new CFGPair(fieldDecl, fieldDecl);
    }

    @Override
    public CFGPair visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        CFGNonConditional initial = new CFGNonConditional();
        CFGPair curPair = new CFGPair(initial, new NOP());
        initial.autoChild = curPair.endBlock;
        ((CFGNonConditional) curPair.startBlock).autoChild = curPair.endBlock;
        for (MethodDefinitionParameter param : methodDefinition.methodDefinitionParameterList) {
            CFGPair placeholder = param.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        CFGPair methodBody = methodDefinition.block.accept(this, symbolTable);
        curPair.endBlock.autoChild = methodBody.startBlock;
        methodBody.startBlock.parents.add(curPair.endBlock);
        return new CFGPair(initial, methodBody.endBlock);
    }

    @Override
    public CFGPair visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        CFGNonConditional import_ = new CFGNonConditional();
        import_.lines.add(new CFGDeclaration(importDeclaration));
        return new CFGPair(import_, import_);
    }

    @Override
    public CFGPair visit(For forStatement, SymbolTable symbolTable) {
        loopToBreak.push(new ArrayList<>());
        // If false, end with NOP, also end of for_statement
        NOP falseBlock = new NOP("For Loop (false) " + forStatement.terminatingCondition.getSourceCode());
        NOP exit = new NOP("Exit For");
        falseBlock.autoChild = exit;
        exit.parents.add(falseBlock);

        // For the block, the child of that CFGBlock should be a block with the increment line
        CFGNonConditional incrementBlock = new CFGNonConditional();
        incrementBlock.lines.add(new CFGAssignment(forStatement.update));

        // Evaluate the condition
        CFGExpression condition = new CFGExpression(forStatement.terminatingCondition);
        CFGConditional evaluateBlock = ShortCircuitProcessor.shortCircuit(new CFGConditional(condition));
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(incrementBlock);

        // In for loops, continue should point to an incrementBlock
        continueBlocks.push(incrementBlock);

        // If true, run the block.
        CFGPair truePair = forStatement.block.accept(this, symbolTable);

        evaluateBlock.falseChild = falseBlock;
        evaluateBlock.falseChild.parents.add(evaluateBlock);

        evaluateBlock.trueChild = truePair.startBlock;
        truePair.startBlock.parents.add(evaluateBlock);

        if (truePair.endBlock != exitNOP) {
            truePair.endBlock.autoChild = incrementBlock;
            incrementBlock.parents.add(truePair.endBlock);
        }
        // Initialize the condition variable
        CFGNonConditional initializeBlock = new CFGNonConditional();
        initializeBlock.lines.add(new CFGDeclaration(forStatement.initialization));

        // child of initialization block is evaluation
        initializeBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(initializeBlock);

        // Child of that increment block should be the evaluation
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(incrementBlock);


//        NOP nop = new NOP();
//        nop.parents.add(falseBlock);
//        falseBlock.autoChild = nop;

        handleBreaksInLoops(falseBlock);

        continueBlocks.pop();
        return new CFGPair(initializeBlock, exit, false);
    }

    private void handleBreaksInLoops(CFGBlock cfgBlock) {
        List<CFGNonConditional> toRemove = new ArrayList<>();
        List<CFGNonConditional> breakBlocks = loopToBreak.pop();
        if (!breakBlocks.isEmpty()) {
            for (CFGNonConditional breakBlock: breakBlocks) {
                breakBlock.autoChild = cfgBlock;
                toRemove.add(breakBlock);
                cfgBlock.parents.add(breakBlock);
            }
        }
        for (CFGNonConditional breakBlock: toRemove)
            breakBlocks.remove(breakBlock);
    }

    @Override
    public CFGPair visit(Break breakStatement, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + breakStatement.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Continue continueStatement, SymbolTable symbolTable) {
        // unreachable: it's equivalent to returning a NOP
        // Any loop node which sends a visitor to a block should handle continue logic
        throw new IllegalStateException("we cannot visit " + continueStatement.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(While whileStatement, SymbolTable symbolTable) {
        loopToBreak.push(new ArrayList<>());
        // If false, end with NOP, also end of while
        NOP falseBlock = new NOP();

        // Evaluate the condition
        CFGExpression cfgExpression = new CFGExpression(whileStatement.test);
        CFGConditional conditionExpr = new CFGConditional(cfgExpression);
        conditionExpr.falseChild = falseBlock;
        falseBlock.parents.add(conditionExpr);

        // In for loops, continue should point to the evaluation expression
        continueBlocks.push(conditionExpr);

        // If true, run the block.
        CFGPair truePair = whileStatement.body.accept(this, symbolTable);

        conditionExpr.trueChild = truePair.startBlock;
        conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        if (truePair.endBlock != null) {
            truePair.endBlock.autoChild = conditionExpr;
            conditionExpr.parents.add(truePair.endBlock);
        }

        handleBreaksInLoops(falseBlock);
        continueBlocks.pop();
        return new CFGPair(conditionExpr, falseBlock);
    }

    @Override
    public CFGPair visit(Program program, SymbolTable symbolTable) {
        CFGPair curPair = new CFGPair(initialGlobalBlock, new NOP());
        initialGlobalBlock.autoChild = curPair.endBlock;
        for (ImportDeclaration import_ : program.importDeclarationList) {
            CFGPair placeholder = import_.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        for (FieldDeclaration field : program.fieldDeclarationList) {
            CFGPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        for (MethodDefinition method : program.methodDefinitionList) {
            methodCFGBlocks.put(method.methodName.id, method.accept(this, symbolTable).startBlock);
        }
        // don't need to return pair bc only need start block
        return null;
    }

    @Override
    public CFGPair visit(UnaryOpExpression unaryOpExpression, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + unaryOpExpression.getClass().getSimpleName());
}

    @Override
    public CFGPair visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + binaryOpExpression.getClass().getSimpleName());
    }


    @Override
    public CFGPair visit(Block block, SymbolTable symbolTable) {
        NOP initial = new NOP();
        NOP exit = new NOP();
        CFGPair curPair = new CFGPair(initial, new NOP());
        initial.autoChild = curPair.endBlock;

        for (FieldDeclaration field : block.fieldDeclarationList) {
            CFGPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }

        for (Statement statement : block.statementList) {
            if (statement instanceof Continue) {
                // will return a NOP() for sure because Continue blocks should be pointers back to the evaluation block
                CFGNonConditional continueCfg = new NOP();
                CFGBlock nextBlock = continueBlocks.peek();
                continueCfg.autoChild = nextBlock;
                nextBlock.parents.add(continueCfg);
                continueCfg.parents.add(curPair.endBlock);
                curPair.endBlock.autoChild = continueCfg;
                return new CFGPair(initial, continueCfg);
            }
            if (statement instanceof Break) {
                // a break is not a real block either
                CFGNonConditional breakCfg = new NOP("Break");
                loopToBreak.peek().add(breakCfg);
                breakCfg.parents.add(curPair.endBlock);
                curPair.endBlock.autoChild = breakCfg;
                return new CFGPair(initial, breakCfg, false);
            }
            if (statement instanceof Return) {
                CFGPair returnPair = statement.accept(this, symbolTable);
                curPair.endBlock.autoChild = returnPair.startBlock;
                returnPair.startBlock.parents.add(curPair.endBlock);
                return new CFGPair(initial, returnPair.endBlock);
            }
            // recurse normally for other cases
            else {
                CFGPair placeholder = statement.accept(this, symbolTable);
                curPair.endBlock.autoChild = placeholder.startBlock;
                placeholder.startBlock.parents.add(curPair.endBlock);
                curPair = placeholder;
            }
        }
        curPair.endBlock.autoChild = exit;
        exit.parents.add(curPair.endBlock);
        return new CFGPair(initial, exit, false);
    }

    @Override
    public CFGPair visit(ParenthesizedExpression parenthesizedExpression, SymbolTable symbolTable) {
        // unreachable (expr)
        throw new IllegalStateException("we cannot visit " + parenthesizedExpression.getClass().getSimpleName());}

    @Override
    public CFGPair visit(LocationArray locationArray, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + locationArray.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + expressionParameter.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(If ifStatement, SymbolTable symbolTable) {
        // always end with nop
        final NOP exit = new NOP();

        // If true, run the block.
        CFGPair truePair = ifStatement.ifBlock.accept(this, symbolTable);
        if (truePair.endBlock.autoChild == null) {
            // handling the cases when we have a "Continue" statement
            truePair.endBlock.autoChild = exit;
            exit.parents.add(truePair.endBlock);
        }

        // Evaluate the condition
        CFGExpression condition = new CFGExpression(ifStatement.test);

        CFGConditional conditionExpr;
        if (ifStatement.elseBlock != null) {
            CFGPair falsePair = ifStatement.elseBlock.accept(this, symbolTable);
            if (falsePair.endBlock.autoChild == null) {
                // handling the cases when we have a "Continue" statement
                falsePair.endBlock.autoChild = exit;
                exit.parents.add(falsePair.endBlock);
            }
            conditionExpr = new CFGConditional(condition, truePair.startBlock, falsePair.startBlock);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
            falsePair.startBlock.parents.add(conditionExpr);
        } else {
            conditionExpr = new CFGConditional(condition, truePair.startBlock, exit);
            conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        }
        truePair.startBlock.parents.add(conditionExpr);
        return new CFGPair(ShortCircuitProcessor.shortCircuit(conditionExpr), exit);
    }

    @Override
    public CFGPair visit(Return returnStatement, SymbolTable symbolTable) {
        CFGNonConditional returnBlock = new CFGNonConditional();
        returnBlock.lines.add(new CFGExpression(returnStatement));
        return new CFGPair(returnBlock, exitNOP);
    }

    @Override
    public CFGPair visit(Array array, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + array.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(MethodCall methodCall, SymbolTable symbolTable) {
        // unreachable - handle later in assembly gen
        throw new IllegalStateException("we cannot visit " + methodCall.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(MethodCallStatement methodCallStatement, SymbolTable symbolTable) {
        CFGNonConditional methodCallExpr = new CFGNonConditional();
        methodCallExpr.lines.add(new CFGExpression(methodCallStatement));
        return new CFGPair(methodCallExpr, methodCallExpr);
    }

    @Override
    public CFGPair visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        CFGNonConditional assignment = new CFGNonConditional();
        locationAssignExpr.assignExpr.expression = rotateBinaryOpExpression(locationAssignExpr.assignExpr.expression);
        assignment.lines.add(new CFGAssignment(new Assignment(locationAssignExpr.location, locationAssignExpr.assignExpr)));
        return new CFGPair(assignment, assignment);
    }

    @Override
    public CFGPair visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // unreachable - should've been combined into an Assignment elsewhere
        throw new IllegalStateException("we cannot visit " + assignOpExpr.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(MethodDefinitionParameter methodDefinitionParameter, SymbolTable symbolTable) {
        CFGNonConditional methodParam = new CFGNonConditional();
        methodParam.autoChild = methodParam;
        methodParam.lines.add(new CFGDeclaration(methodDefinitionParameter));
        return new CFGPair(methodParam, methodParam);
    }

    @Override
    public CFGPair visit(Name name, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + name.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + locationVariable.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Len len, SymbolTable symbolTable) {
        // unreachable (expr)
        throw new IllegalStateException("we cannot visit " + len.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Increment increment, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + increment.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Decrement decrement, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + decrement.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + charLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + stringLiteral.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        // unreachable
        throw new IllegalStateException("we cannot visit " + compoundAssignOpExpr.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Initialization initialization, SymbolTable symbolTable) {
        throw new IllegalStateException("we cannot visit " + initialization.getClass().getSimpleName());
    }

    @Override
    public CFGPair visit(Assignment assignment, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public CFGPair visit(Update update, SymbolTable symbolTable) {
        return null;
    }

    public static Expression rotateBinaryOpExpression(Expression expr) {
        printParseTree(expr);
        System.out.println();
        if (expr instanceof BinaryOpExpression) {
            System.out.println("inside expr");
            if (((BinaryOpExpression) expr).rhs instanceof  BinaryOpExpression) {
                System.out.println("rhs expr");
                BinaryOpExpression rhsBinOpExpr = (BinaryOpExpression) ((BinaryOpExpression) expr).rhs;
                if (BinaryOpExpression.operatorPrecedence.get(((BinaryOpExpression) expr).op).equals(BinaryOpExpression.operatorPrecedence.get(rhsBinOpExpr.op))) {
                    System.out.println("same precedence");
                    rhsBinOpExpr.lhs = expr;
                    ((BinaryOpExpression) expr).rhs = rhsBinOpExpr.lhs;
                    rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
                    rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
                    rotateBinaryOpExpression(rhsBinOpExpr.rhs);
                    System.out.println("rotatation");
                    System.out.println(rhsBinOpExpr);
                    System.out.println();
                    return rhsBinOpExpr;
                }
            }
            System.out.println("test");
            System.out.println(((BinaryOpExpression) expr).lhs);
            System.out.println(((BinaryOpExpression) expr).rhs);
            System.out.println();
            rotateBinaryOpExpression(((BinaryOpExpression) expr).lhs);
            rotateBinaryOpExpression(((BinaryOpExpression) expr).rhs);
        }
        else if (expr instanceof ParenthesizedExpression) {
            System.out.println("paren " + ((ParenthesizedExpression) expr).expression);
            rotateBinaryOpExpression(((ParenthesizedExpression) expr).expression);
        }
        else if (expr instanceof MethodCall) {
            for (MethodCallParameter param: ((MethodCall) expr).methodCallParameterList) {
                if (param instanceof ExpressionParameter) {
                    rotateBinaryOpExpression(((ExpressionParameter) param).expression);
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

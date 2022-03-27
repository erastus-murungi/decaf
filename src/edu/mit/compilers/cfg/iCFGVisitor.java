package edu.mit.compilers.cfg;

import java.util.*;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class iCFGVisitor implements Visitor<CFGPair> {
    public CFGNonConditional initialGlobalBlock = new CFGNonConditional();
    public HashMap<String, CFGBlock> methodCFGBlocks = new HashMap<>();
    public ArrayList<CFGPair> loopStack = new ArrayList<>();
    public Stack<CFGNonConditional> breakBlocks = new Stack<>(); // a bunch of break blocks to point to the right place
    private final NOP exitNOP;

    public iCFGVisitor() {
        exitNOP = new NOP();
    }

    @Override
    public CFGPair visit(IntLiteral intLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public CFGPair visit(BooleanLiteral booleanLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public CFGPair visit(DecimalLiteral decimalLiteral, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public CFGPair visit(HexLiteral hexLiteral, SymbolTable symbolTable) {
        return null;
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
        // If false, end with NOP, also end of for_statement
        CFGNonConditional falseBlock = new NOP();

        // For the block, the child of that CFGBlock should be a block with the increment line
        CFGNonConditional incrementBlock = new CFGNonConditional();
        incrementBlock.lines.add(new CFGAssignment(forStatement.update));

        // Evaluate the condition
        CFGExpression condition = new CFGExpression(forStatement.terminatingCondition);
        CFGConditional evaluateBlock = ShortCircuitProcessor.shortCircuit(new CFGConditional(condition));
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(incrementBlock);

        // If true, run the block.
        loopStack.add(new CFGPair(evaluateBlock, falseBlock));
        CFGPair truePair = forStatement.block.accept(this, symbolTable);
        loopStack.remove(loopStack.size() - 1);

        evaluateBlock.falseChild = falseBlock;

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

        NOP nop = new NOP();
        nop.parents.add(falseBlock);
        falseBlock.autoChild = nop;
        return new CFGPair(initializeBlock, nop, false);
    }

    @Override
    public CFGPair visit(Break breakStatement, SymbolTable symbolTable) {
        return null;
    }

    @Override
    public CFGPair visit(Continue continueStatement, SymbolTable symbolTable) {
        // unreachable: it's equivalent to returning a NOP
        return null;
    }

    @Override
    public CFGPair visit(While whileStatement, SymbolTable symbolTable) {
        // If false, end with NOP, also end of while
        NOP falseBlock = new NOP();

        // Evaluate the condition
        CFGExpression cfgExpression = new CFGExpression(whileStatement.test);
        CFGConditional conditionExpr = new CFGConditional(cfgExpression);
        conditionExpr.falseChild = falseBlock;
        falseBlock.parents.add(conditionExpr);
        loopStack.add(new CFGPair(conditionExpr, falseBlock));

        // If true, run the block.
        CFGPair truePair = whileStatement.body.accept(this, symbolTable);

        conditionExpr.trueChild = truePair.startBlock;
        conditionExpr = ShortCircuitProcessor.shortCircuit(conditionExpr);
        if (truePair.endBlock != null) {
            truePair.endBlock.autoChild = conditionExpr;
            conditionExpr.parents.add(truePair.endBlock);
        }

        loopStack.remove(loopStack.size() - 1);
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
        return null;
    }

    @Override
    public CFGPair visit(BinaryOpExpression binaryOpExpression, SymbolTable symbolTable) {
        // unreachable
        return null;
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
                CFGBlock evalBlock = loopStack.get(loopStack.size() - 1).startBlock;
                continueCfg.autoChild = evalBlock.parents.get(0);
                evalBlock.parents.get(0).parents.add(continueCfg);
                return new CFGPair(continueCfg, continueCfg, false);
            }
            if (statement instanceof Break) {
                // a break is not a real block either
                CFGNonConditional breakCfg = new NOP();
                breakBlocks.add(breakCfg);
                return new CFGPair(breakCfg, breakCfg);
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
                if (!breakBlocks.isEmpty() && !loopStack.isEmpty()) {
                    CFGBlock afterLoop = loopStack.get(loopStack.size() - 1).endBlock;
                    for (CFGNonConditional breakBlock: breakBlocks) {
                        breakBlock.autoChild = afterLoop;
                        afterLoop.parents.add(breakBlock);
                    }
                    breakBlocks.clear();
                }

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
        return null;
    }

    @Override
    public CFGPair visit(LocationArray locationArray, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(ExpressionParameter expressionParameter, SymbolTable symbolTable) {
        // unreachable
        return null;
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
        NOP nop = new NOP();
        returnBlock.autoChild = nop;
        nop.parents.add(returnBlock);
        return new CFGPair(returnBlock, exitNOP);
    }

    @Override
    public CFGPair visit(Array array, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(MethodCall methodCall, SymbolTable symbolTable) {
        // unreachable - handle later in assembly gen
        return null;
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
        assignment.lines.add(new CFGAssignment(new Assignment(locationAssignExpr.location, locationAssignExpr.assignExpr)));
        return new CFGPair(assignment, assignment);
    }

    @Override
    public CFGPair visit(AssignOpExpr assignOpExpr, SymbolTable symbolTable) {
        // unreachable - should've been combined into an Assignment elsewhere
        return null;
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
        return null;
    }

    @Override
    public CFGPair visit(LocationVariable locationVariable, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(Len len, SymbolTable symbolTable) {
        // unreachable (expr)
        return null;
    }

    @Override
    public CFGPair visit(Increment increment, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(Decrement decrement, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(CharLiteral charLiteral, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(StringLiteral stringLiteral, SymbolTable symbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(CompoundAssignOpExpr compoundAssignOpExpr, SymbolTable curSymbolTable) {
        // unreachable
        return null;
    }

    @Override
    public CFGPair visit(Initialization initialization, SymbolTable symbolTable) {
        return null;
    }
}

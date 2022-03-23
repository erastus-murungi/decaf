package edu.mit.compilers.cfg;

import java.util.ArrayList;
import java.util.HashMap;

import edu.mit.compilers.ast.*;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class iCFGVisitor implements Visitor<CFGPair> {
    public CFGBlock initialGlobalBlock = new CFGBlock();
    public HashMap<String, CFGBlock> methodCFGBlocks = new HashMap<>();
    public ArrayList<CFGPair> loopStack = new ArrayList<>();

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
        CFGBlock fieldDecl = new CFGBlock();
        fieldDecl.lines.add(new CFGDeclaration(fieldDeclaration));
        return new CFGPair(fieldDecl, fieldDecl);
    }
    @Override
    public CFGPair visit(MethodDefinition methodDefinition, SymbolTable symbolTable) {
        CFGBlock initial = new CFGBlock();
        CFGPair curPair = new CFGPair(initial, new NOP());
        for (MethodDefinitionParameter param : methodDefinition.methodDefinitionParameterList){
            CFGPair placeholder = param.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        CFGPair methodBody = methodDefinition.block.accept(this, symbolTable);
        curPair.endBlock = methodBody.startBlock;
        curPair.endBlock.parents.add(curPair.startBlock);

        return new CFGPair(initial, methodBody.endBlock);
    }
    @Override
    public CFGPair visit(ImportDeclaration importDeclaration, SymbolTable symbolTable) {
        CFGBlock import_ = new CFGBlock();
        import_.lines.add(new CFGDeclaration(importDeclaration));
        return new CFGPair(import_, import_);
    }
    @Override
    public CFGPair visit(For forStatement, SymbolTable symbolTable) {
        // If false, end with NOP, also end of for_statement
        CFGBlock falseBlock = new NOP();

        // For the block, the child of that CFGBlock should be a block with the increment line
        CFGBlock incrementBlock = new CFGBlock();
        incrementBlock.lines.add(new CFGAssignment(new Assignment(forStatement.updatingLocation, forStatement.updateAssignExpr)));

        // Evaluate the condition
        CFGBlock evaluateBlock = new CFGBlock();
        evaluateBlock.lines.add(new CFGExpression(forStatement.terminatingCondition));
        evaluateBlock.falseChild = falseBlock;
        falseBlock.parents.add(evaluateBlock);
    
        // If true, run the block.
        loopStack.add(new CFGPair(evaluateBlock, falseBlock));
        CFGPair truePair = forStatement.block.accept(this, symbolTable);
        loopStack.remove(loopStack.size() - 1);

        evaluateBlock.trueChild = truePair.startBlock;
        truePair.startBlock.parents.add(evaluateBlock);
        if (truePair.endBlock != null){
            truePair.endBlock.autoChild = evaluateBlock;
            evaluateBlock.parents.add(truePair.endBlock);
        }
        // Initialize the condition variable
        CFGBlock initializeBlock = new CFGBlock();
        initializeBlock.lines.add(new CFGAssignment(forStatement.initExpression));

        // child of initialization block is evaluation
        initializeBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(initializeBlock);

        // Child of that increment block should be the evaluation
        incrementBlock.autoChild = evaluateBlock;
        evaluateBlock.parents.add(incrementBlock);

        return new CFGPair(initializeBlock, falseBlock);
    }
    @Override
    public CFGPair visit(Break breakStatement, SymbolTable symbolTable) {
        CFGBlock breakBlock =  new CFGBlock();
        breakBlock.lines.add(new CFGExpression(breakStatement));
        return new CFGPair(breakBlock, breakBlock);
    }

    @Override
    public CFGPair visit(Continue continueStatement, SymbolTable symbolTable) {
        CFGBlock continueBlock =  new CFGBlock();
        continueBlock.lines.add(new CFGExpression(continueStatement));
        return new CFGPair(continueBlock, continueBlock);
    }
    @Override
    public CFGPair visit(While whileStatement, SymbolTable symbolTable) {
        // If false, end with NOP, also end of while
        CFGBlock falseBlock = new NOP();

        // Evaluate the condition 
        CFGBlock conditionExpr = new CFGBlock();
        conditionExpr.lines.add(new CFGExpression(whileStatement.test));
        conditionExpr.falseChild = falseBlock;
        falseBlock.parents.add(conditionExpr);
        loopStack.add(new CFGPair(conditionExpr, falseBlock));

        // If true, run the block.
        CFGPair truePair = whileStatement.body.accept(this, symbolTable);

        conditionExpr.trueChild = truePair.startBlock;
        if (truePair.endBlock != null){
            truePair.endBlock.autoChild = conditionExpr;
            conditionExpr.parents.add(truePair.endBlock);
        }
        
        loopStack.remove(loopStack.size() - 1);
        return new CFGPair(conditionExpr, falseBlock);
    }
    @Override
    public CFGPair visit(Program program, SymbolTable symbolTable) {
        CFGPair curPair = new CFGPair(initialGlobalBlock, new NOP());
        for (ImportDeclaration import_ : program.importDeclarationList){
            CFGPair placeholder = import_.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        for (FieldDeclaration field : program.fieldDeclarationList){
            CFGPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        for (MethodDefinition method : program.methodDefinitionList){
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
        CFGBlock exit = new NOP();
        CFGBlock initial = new NOP();
        CFGPair curPair = new CFGPair(initial, new NOP());

        for (FieldDeclaration field : block.fieldDeclarationList){
            CFGPair placeholder = field.accept(this, symbolTable);
            curPair.endBlock.autoChild = placeholder.startBlock;
            placeholder.startBlock.parents.add(curPair.endBlock);
            curPair = placeholder;
        }
        for (Statement statement : block.statementList){
           if (statement instanceof Continue) {
               CFGBlock continueCfg = statement.accept(this, symbolTable).startBlock;
               CFGBlock evalBlock = loopStack.get(loopStack.size() - 1).startBlock;
               continueCfg.autoChild = evalBlock;
               evalBlock.parents.add(continueCfg);
               curPair.endBlock.autoChild = continueCfg;
               continueCfg.parents.add(curPair.endBlock);
               return new CFGPair(initial, null);
           }
           if (statement instanceof Break ){
                CFGBlock breakCfg = statement.accept(this, symbolTable).startBlock;
                CFGBlock endBlock = loopStack.get(loopStack.size() - 1).endBlock;
                breakCfg.autoChild = endBlock;
                endBlock.parents.add(breakCfg);
                curPair.endBlock.autoChild = breakCfg;
                breakCfg.parents.add(curPair.endBlock);
                return new CFGPair(initial, endBlock);
           }
           if (statement instanceof Return){
               CFGPair returnPair = statement.accept(this, symbolTable);
               curPair.endBlock.autoChild = returnPair.startBlock;
               returnPair.startBlock.parents.add(curPair.endBlock);
               return new CFGPair(initial, null);
           }
           // recurse normally for other cases
           else {
               CFGPair placeholder = statement.accept(this, symbolTable);
               curPair.endBlock.autoChild = placeholder.startBlock;
               placeholder.startBlock.parents.add(curPair.endBlock);
               curPair = placeholder;
           }
           curPair.endBlock.autoChild = exit;
           exit.parents.add(curPair.endBlock);
        }
        return new CFGPair(initial, exit);
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
        CFGBlock exit = new NOP();

        // If true, run the block.
        CFGPair truePair = ifStatement.ifBlock.accept(this, symbolTable);
        truePair.endBlock.autoChild = exit;
        exit.parents.add(truePair.endBlock);

        // Evaluate the condition
        CFGBlock conditionExpr = new CFGBlock();
        conditionExpr.lines.add(new CFGExpression(ifStatement.test));
        conditionExpr.trueChild = truePair.startBlock;
        truePair.startBlock.parents.add(conditionExpr);

        // Connect else block if it exists
        if (ifStatement.elseBlock != null) {
            CFGPair falsePair = ifStatement.elseBlock.accept(this, symbolTable);
            falsePair.endBlock.autoChild = exit;
            exit.parents.add(falsePair.endBlock);
            conditionExpr.falseChild = falsePair.startBlock;
            falsePair.startBlock.parents.add(conditionExpr);
        }
        else {
            conditionExpr.falseChild = exit;
            exit.parents.add(conditionExpr);
        }

        return new CFGPair(conditionExpr, exit);
    }
    @Override
    public CFGPair visit(Return returnStatement, SymbolTable symbolTable) {
         CFGBlock returnBlock = new CFGBlock();
         returnBlock.lines.add(new CFGExpression(returnStatement));
         CFGBlock nop = new NOP();
         returnBlock.autoChild = nop;
         nop.parents.add(returnBlock);
         return new CFGPair(returnBlock, nop);
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
        CFGBlock methodCallExpr = new CFGBlock();
        methodCallExpr.lines.add(new CFGExpression(methodCallStatement));
        return new CFGPair(methodCallExpr, methodCallExpr);
    }
    @Override
    public CFGPair visit(LocationAssignExpr locationAssignExpr, SymbolTable symbolTable) {
        CFGBlock assignment = new CFGBlock();
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
        CFGBlock methodParam = new CFGBlock();
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
}

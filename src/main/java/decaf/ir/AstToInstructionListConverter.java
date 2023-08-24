package decaf.ir;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import decaf.analysis.syntax.ast.AST;
import decaf.analysis.syntax.ast.Assignment;
import decaf.analysis.syntax.ast.BinaryOpExpression;
import decaf.analysis.syntax.ast.Block;
import decaf.analysis.syntax.ast.BooleanLiteral;
import decaf.analysis.syntax.ast.CompoundAssignOpExpr;
import decaf.analysis.syntax.ast.ExpressionParameter;
import decaf.analysis.syntax.ast.FieldDeclaration;
import decaf.analysis.syntax.ast.Initialization;
import decaf.analysis.syntax.ast.IntLiteral;
import decaf.analysis.syntax.ast.Len;
import decaf.analysis.syntax.ast.Location;
import decaf.analysis.syntax.ast.LocationArray;
import decaf.analysis.syntax.ast.LocationAssignExpr;
import decaf.analysis.syntax.ast.LocationVariable;
import decaf.analysis.syntax.ast.MethodCall;
import decaf.analysis.syntax.ast.ActualArgument;
import decaf.analysis.syntax.ast.MethodCallStatement;
import decaf.analysis.syntax.ast.FormalArgument;
import decaf.analysis.syntax.ast.ParenthesizedExpression;
import decaf.analysis.syntax.ast.RValue;
import decaf.analysis.syntax.ast.Return;
import decaf.analysis.syntax.ast.StringLiteral;
import decaf.analysis.syntax.ast.Type;
import decaf.analysis.syntax.ast.UnaryOpExpression;
import decaf.ir.codes.ArrayBoundsCheck;
import decaf.ir.codes.BinaryInstruction;
import decaf.ir.codes.CopyInstruction;
import decaf.ir.codes.FunctionCallNoResult;
import decaf.ir.codes.FunctionCallWithResult;
import decaf.ir.codes.GetAddress;
import decaf.ir.codes.ReturnInstruction;
import decaf.ir.codes.UnaryInstruction;
import decaf.ir.names.IrAssignable;
import decaf.ir.names.IrIntegerConstant;
import decaf.ir.names.IrMemoryAddress;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrStackArray;
import decaf.ir.names.IrStringConstant;
import decaf.ir.names.IrValue;
import decaf.shared.Operators;
import decaf.shared.descriptors.ArrayDescriptor;
import decaf.shared.env.Scope;

class AstToInstructionListConverter implements CodegenAstVisitor<InstructionList> {
  private final Scope scope;
  private final HashMap<String, IrStringConstant> stringLiteralMapping;
  private final Map<String, IrValue> cachedAddresses = new HashMap<>();

  public AstToInstructionListConverter(
      Scope scope,
      HashMap<String, IrStringConstant> stringLiteralMapping,
      Set<IrValue> irGlobals
  ) {
    this.scope = scope;
    this.stringLiteralMapping = stringLiteralMapping;
    for (var global : irGlobals) {
      cachedAddresses.put(
          global.getLabel(),
          global
      );
    }
  }

  private IrValue newIrLocation(
      String label,
      Type type
  ) {
    if (cachedAddresses.containsKey(label)) return cachedAddresses.get(label)
                                                                  .copy();
    return new IrSsaRegister(
        label,
        type
    );
  }

  @Override
  public InstructionList visit(
      LocationArray locationArray,
      IrAssignable resultLocation
  ) {
    final var indexInstructionList = locationArray.expression.accept(
        this,
        resultLocation
    );
    final var locationArrayInstructionList = new InstructionList();
    locationArrayInstructionList.addAll(indexInstructionList);

    final var maybeArrayDescriptorFromValidScopes = scope.lookup(locationArray.getLabel());
    final var arrayDescriptor = (ArrayDescriptor) maybeArrayDescriptorFromValidScopes.orElseThrow(() -> new IllegalStateException(
        "expected to find array " + locationArray.getLabel() + " in scope"));
    var base = newIrLocation(
        locationArray.getLabel(),
        arrayDescriptor.getType()
    );
    final var getAddressInstruction = new GetAddress(
        base,
        indexInstructionList.getPlace(),
        generateAddressName(arrayDescriptor.getType()),
        arrayDescriptor.size,
        locationArray
    );
    locationArrayInstructionList.add(getAddressInstruction);

    if (!(indexInstructionList.getPlace() instanceof IrIntegerConstant)) {
      locationArrayInstructionList.add(new ArrayBoundsCheck(
          getAddressInstruction,
          IndexManager.getNextArrayBoundsCheckLabelIndex()
      ));
    }
    locationArrayInstructionList.setPlace(getAddressInstruction.getDestination());
    return locationArrayInstructionList;
  }

  private Stack<IrValue> lowerMethodCallParameters(
      InstructionList instructionList,
      List<ActualArgument> actualArgumentList
  ) {
    var parameterIrRegisters = new Stack<IrValue>();
    for (ActualArgument actualArgument : actualArgumentList) {
      var parameterInstructionList = actualArgument.accept(
          this,
          null
      );
      instructionList.addAll(parameterInstructionList);
      parameterIrRegisters.add(parameterInstructionList.getPlace());
    }
    return parameterIrRegisters;
  }

  private IrAssignable resolveStoreLocation(Type type) {
    return IrSsaRegister.gen(type);
  }

  private IrAssignable resolveStoreLocation(
      IrAssignable resultLocation,
      Type type
  ) {
    if (resultLocation == null) return resolveStoreLocation(type);
    return resultLocation.copy();
  }

  private IrMemoryAddress generateAddressName(
      Type type
  ) {
    return new IrMemoryAddress(
        IndexManager.genRegisterIndex(),
        Type.lower(type)
    );
  }

  @Override
  public InstructionList visit(
      MethodCall methodCall,
      IrAssignable resultLocation
  ) {
    final InstructionList instructionList = new InstructionList();
    var place = resolveStoreLocation(methodCall.getType());
    instructionList.add(new FunctionCallWithResult(
        methodCall,
        place,
        lowerMethodCallParameters(
            instructionList,
            methodCall.actualArgumentList
        ),
        methodCall.getSourceCode()
    ));
    instructionList.setPlace(place.copy());
    return instructionList;
  }


  @Override
  public InstructionList visit(
      MethodCallStatement methodCallStatement,
      IrAssignable resultLocation
  ) {
    var instructionList = new InstructionList();
    instructionList.add(new FunctionCallNoResult(
        methodCallStatement.methodCall,
        lowerMethodCallParameters(
            instructionList,
            methodCallStatement.methodCall.actualArgumentList
        ),
        methodCallStatement.getSourceCode()
    ));
    return instructionList;
  }

  @Override
  public InstructionList visit(
      LocationAssignExpr locationAssignExpr,
      IrAssignable resultLocation
  ) {
    final InstructionList lhs = locationAssignExpr.location.accept(
        this,
        resultLocation
    );
    IrSsaRegister locationVariable = (IrSsaRegister) lhs.getPlace();
    final InstructionList rhs = locationAssignExpr.assignExpr.accept(
        this,
        resultLocation
    );
    IrValue irValueVariable = rhs.getPlace();
    lhs.addAll(rhs);
    lhs.addAll(lowerAugmentedAssignment(
        locationVariable,
        irValueVariable,
        locationAssignExpr.getSourceCode(),
        locationAssignExpr
    ));
    return lhs;
  }

  @Override
  public InstructionList visit(
      FormalArgument formalArgument,
      IrAssignable resultLocation
  ) {
    return new InstructionList(new IrSsaRegister(
        formalArgument.getName(),
        formalArgument.getType()
    ));
  }

  @Override
  public InstructionList visit(
      RValue RValue,
      IrAssignable resultLocation
  ) {
    Type type = scope.lookup(RValue.getLabel())
                     .orElseThrow().getType();
    return new InstructionList(newIrLocation(
        RValue.getLabel(),
        type
    ));
  }

  @Override
  public InstructionList visit(
      LocationVariable locationVariable,
      IrAssignable resultLocation
  ) {
    Type type = scope.lookup(locationVariable.getLabel())
                     .orElseThrow().getType();
    return new InstructionList(newIrLocation(
        locationVariable.getLabel(),
        type
    ));
  }

  @Override
  public InstructionList visit(Len len) {
    final ArrayDescriptor arrayDescriptor = (ArrayDescriptor) scope.lookup(len.rValue.getLabel())
                                                                   .orElseThrow(() -> new IllegalStateException(
                                                                       len.rValue.getLabel() +
                                                                           " should be present"));
    return new InstructionList(new IrIntegerConstant(
        arrayDescriptor.size,
        Type.Int
    ));
  }

  @Override
  public InstructionList visit(
      StringLiteral stringLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(stringLiteralMapping.get(stringLiteral.literal));
  }

  @Override
  public InstructionList visit(
      CompoundAssignOpExpr compoundAssignOpExpr,
      IrAssignable resultLocation
  ) {
    return compoundAssignOpExpr.expression.accept(
        this,
        resultLocation
    );
  }

  @Override
  public InstructionList visit(
      Initialization initialization,
      IrAssignable resultLocation
  ) {
    var initializationLocationInstructionList = initialization.initLocation.accept(
        this,
        resultLocation
    );
    var initializationExpressionInstructionList = initialization.initExpression.accept(
        this,
        resultLocation
    );
    var initializationInstructionList = new InstructionList(initializationLocationInstructionList.getPlace());
    initializationInstructionList.addAll(initializationLocationInstructionList);
    initializationInstructionList.addAll(initializationExpressionInstructionList);
    initializationInstructionList.add(new CopyInstruction(
        (IrSsaRegister) initializationLocationInstructionList.getPlace()
                                                             .copy(),
        initializationExpressionInstructionList.getPlace()
                                               .copy(),
        initialization,
        initialization.getSourceCode()
    ));
    return initializationInstructionList;
  }

  private InstructionList lowerAugmentedAssignment(
      IrValue lhs,
      IrValue rhs,
      String op,
      AST assignment
  ) {
    var instructionList = new InstructionList(lhs);
    switch (op) {
      case Operators.ADD_ASSIGN, Operators.MINUS_ASSIGN, Operators.MULTIPLY_ASSIGN, Operators.DECREMENT, Operators.INCREMENT -> {
        final var constant = new IrIntegerConstant(
            1L,
            Type.Int
        );
        var dest = IrSsaRegister.gen(lhs.getType());
        var inst = switch (op) {
          case Operators.ADD_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.PLUS,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.MULTIPLY_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MULTIPLY,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.MINUS_ASSIGN -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MINUS,
              rhs.copy(),
              assignment.getSourceCode(),
              assignment
          );
          case Operators.DECREMENT -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.MINUS,
              constant.copy(),
              assignment.getSourceCode(),
              assignment
          );
          default -> new BinaryInstruction(
              dest,
              lhs.copy(),
              Operators.PLUS,
              constant.copy(),
              assignment.getSourceCode(),
              assignment
          );
        };
        instructionList.add(inst);
        instructionList.add(CopyInstruction.noAstConstructor(
            (IrSsaRegister) lhs.copy(),
            dest.copy()
        ));
      }
      default -> instructionList.add(new CopyInstruction(
          (IrAssignable) lhs.copy(),
          rhs.copy(),
          assignment,
          assignment.getSourceCode()
      ));
    }
    return instructionList;
  }

  @Override
  public InstructionList visit(
      Assignment assignment,
      IrAssignable resultLocation
  ) {
    var assignmentInstructionList = new InstructionList();
    var storeLocationInstructionList = assignment.getLocation()
                                                 .accept(
                                                     this,
                                                     resultLocation
                                                 );
    InstructionList operandInstructionList;
    if (assignment.assignExpr.expression != null) {
      if (Operators.isCompoundOperator(assignment.getOperator())) {
        var type = assignment.assignExpr.expression.getType();
        operandInstructionList = assignment.assignExpr.expression.accept(
            this,
            IrSsaRegister.gen(type)
        );
      } else {
        operandInstructionList = assignment.assignExpr.expression.accept(
            this,
            (IrAssignable) storeLocationInstructionList.getPlace()
                                                       .copy()
        );
      }
    } else {
      operandInstructionList = new InstructionList();
    }
    assignmentInstructionList.addAll(storeLocationInstructionList);
    assignmentInstructionList.addAll(operandInstructionList);

    if (!(assignment.getOperator()
                    .equals(Operators.ASSIGN) && storeLocationInstructionList.getPlace()
                                                                             .equals(operandInstructionList.getPlace()))) {
      assignmentInstructionList.addAll(lowerAugmentedAssignment(
          storeLocationInstructionList.getPlace(),
          operandInstructionList.getPlace(),
          assignment.getOperator(),
          assignment
      ));
    }
    assignmentInstructionList.setPlace(storeLocationInstructionList.getPlace()
                                                                   .copy());
    return assignmentInstructionList;
  }

  @Override
  public InstructionList visit(
      BooleanLiteral booleanLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(IrIntegerConstant.fromBooleanLiteral(booleanLiteral));
  }

  @Override
  public InstructionList visit(
      IntLiteral intLiteral,
      IrAssignable resultLocation
  ) {
    return new InstructionList(IrIntegerConstant.fromIntLiteral(intLiteral));
  }

  @Override
  public InstructionList visit(
      FieldDeclaration fieldDeclaration,
      IrAssignable resultLocation
  ) {
    var instructionList = new InstructionList();
    for (var array : fieldDeclaration.arrays) {
      var length = array.getSize()
                        .convertToLong();
      var label = new IrStackArray(
          fieldDeclaration.getType(),
          array.getLabel(),
          length
      );
      cachedAddresses.put(
          array.getLabel(),
          label
      );
      for (long i = 0; i < length; i++) {
        var getAddressInstruction = new GetAddress(
            label,
            new IrIntegerConstant(
                i,
                Type.Int
            ),
            generateAddressName(fieldDeclaration.getType()),
            length,
            array
        );
        instructionList.add(getAddressInstruction);
        instructionList.add(new CopyInstruction(
            getAddressInstruction.getDestination()
                                 .copy(),
            IrIntegerConstant.zero(),
            array,
            String.format(
                "%s[%s] = 0",
                label,
                i
            )
        ));
      }
    }
    return instructionList;
  }

  @Override
  public InstructionList visit(
      UnaryOpExpression unaryOpExpression,
      IrAssignable resultLocation
  ) {
    final var operandInstructionList = unaryOpExpression.operand.accept(
        this,
        resultLocation
    );
    final var unaryOpExpressionInstructionList = new InstructionList();
    unaryOpExpressionInstructionList.addAll(operandInstructionList);
    var place = resolveStoreLocation(unaryOpExpression.getType());
    unaryOpExpressionInstructionList.add(new UnaryInstruction(
        place,
        unaryOpExpression.getUnaryOperator()
                         .getSourceCode(),
        operandInstructionList.getPlace()
                              .copy(),
        unaryOpExpression
    ));
    unaryOpExpressionInstructionList.setPlace(place.copy());
    return unaryOpExpressionInstructionList;
  }

  @Override
  public InstructionList visit(
      BinaryOpExpression binaryOpExpression,
      IrAssignable resultLocation
  ) {
    final var firstOperandInstructionList = binaryOpExpression.lhs.accept(
        this,
        null
    );
    final var secondOperandInstructionList = binaryOpExpression.rhs.accept(
        this,
        null
    );

    final var binaryOpExpressionInstructionList = new InstructionList();
    binaryOpExpressionInstructionList.addAll(firstOperandInstructionList);
    binaryOpExpressionInstructionList.addAll(secondOperandInstructionList);

    resultLocation = resolveStoreLocation(
        resultLocation,
        binaryOpExpression.getType()
    );
    binaryOpExpressionInstructionList.add(new BinaryInstruction(
        resultLocation,
        firstOperandInstructionList.getPlace()
                                   .copy(),
        binaryOpExpression.op.getSourceCode(),
        secondOperandInstructionList.getPlace()
                                    .copy(),
        binaryOpExpression.getSourceCode(),
        binaryOpExpression
    ));
    binaryOpExpressionInstructionList.setPlace(resultLocation.copy());
    return binaryOpExpressionInstructionList;
  }

  @Override
  public InstructionList visit(
      Block block,
      IrAssignable resultLocation
  ) {
    final InstructionList blockInstructionList = new InstructionList();
    block.getFieldDeclarations().forEach(fieldDeclaration -> blockInstructionList.addAll(fieldDeclaration.accept(
        this,
        null
    )));
    block.getStatements().forEach(statement -> statement.accept(
        this,
        null
    ));
    return blockInstructionList;
  }

  @Override
  public InstructionList visit(
      ParenthesizedExpression parenthesizedExpression,
      IrAssignable resultLocation
  ) {
    return parenthesizedExpression.expression.accept(
        this,
        resultLocation
    );
  }

  @Override
  public InstructionList visit(
      ExpressionParameter expressionParameter,
      IrAssignable resultLocation
  ) {
    InstructionList expressionParameterInstructionList = new InstructionList();

    if (expressionParameter.expression instanceof LocationVariable) {
      // no need for temporaries
      expressionParameterInstructionList.setPlace(newIrLocation(
          ((Location) expressionParameter.expression).getLabel(),
          expressionParameter.expression.getType()
      ));
    } else if (expressionParameter.expression instanceof IntLiteral) {
      expressionParameterInstructionList.setPlace(new IrIntegerConstant(
          ((IntLiteral) expressionParameter.expression).convertToLong(),
          Type.Int
      ));
    } else {
      var temporaryVariable = IrSsaRegister.gen(expressionParameter.expression.getType());
      InstructionList expressionInstructionList = expressionParameter.expression.accept(
          this,
          resultLocation
      );
      expressionParameterInstructionList.addAll(expressionInstructionList);
      expressionParameterInstructionList.add(new CopyInstruction(
          temporaryVariable,
          expressionInstructionList.getPlace()
                                   .copy(),
          expressionParameter,
          temporaryVariable + " = " + expressionInstructionList.getPlace()
      ));
      expressionParameterInstructionList.setPlace(temporaryVariable.copy());
    }
    return expressionParameterInstructionList;
  }

  @Override
  public InstructionList visit(
      Return returnStatement,
      IrAssignable resultLocation
  ) {
    final InstructionList returnStatementInstructionList;
    if (returnStatement.retExpression != null) {
      InstructionList returnExpressionInstructionList = returnStatement.retExpression.accept(
          this,
          resultLocation
      );
      returnStatementInstructionList = new InstructionList();
      returnStatementInstructionList.addAll(returnExpressionInstructionList);
      returnStatementInstructionList.setPlace(returnExpressionInstructionList.getPlace());
      returnStatementInstructionList.add(new ReturnInstruction(
          returnStatement,
          returnStatementInstructionList.getPlace()
      ));
    } else {
      returnStatementInstructionList = new InstructionList();
      returnStatementInstructionList.add(new ReturnInstruction(returnStatement));
    }
    return returnStatementInstructionList;
  }
}

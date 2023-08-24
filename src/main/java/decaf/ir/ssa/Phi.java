package decaf.ir.ssa;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import decaf.ir.cfg.BasicBlock;
import decaf.ir.codes.Instruction;
import decaf.ir.codes.StoreInstruction;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.dataflow.operand.PhiOperand;
import decaf.ir.names.IrSsaRegister;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;
import decaf.synthesis.asm.AsmWriter;

public class Phi extends StoreInstruction {
  private final Map<BasicBlock, IrValue> basicBlockValueMap;

  public Phi(
      IrSsaRegister v,
      Map<BasicBlock, IrValue> xs
  ) {
    super(
        v,
        null
    );
    assert xs.size() > 2;
    basicBlockValueMap = xs;
  }

  public void removePhiOperand(IrValue irValue) {
    assert basicBlockValueMap.containsValue(irValue);
    for (var entrySet : basicBlockValueMap.entrySet()) {
      if (entrySet.getValue()
                  .equals(irValue)) {
        basicBlockValueMap.remove(entrySet.getKey());
        return;
      }
    }
    throw new IllegalStateException();
  }

  public void removePhiOperandForBlock(BasicBlock basicBlock) {
    checkState(basicBlockValueMap.containsKey(basicBlock));
    basicBlockValueMap.remove(basicBlock);
  }


  public IrValue getVariableForB(BasicBlock B) {
    var value = basicBlockValueMap.get(B);
    checkState(
        value != null,
        this + "\nno irAssignableValue for block: \n" + B
    );
    return value;
  }

  public void replaceBlock(BasicBlock B) {
    var Rs = B.getPredecessors();
    if (Rs.isEmpty()) return;
    var ret = basicBlockValueMap.remove(B);
    checkState(ret != null);
    Rs.forEach(R -> basicBlockValueMap.put(
        R,
        ret
    ));

  }


  public BasicBlock getBasicBlockForV(IrValue irValue) {
    checkState(basicBlockValueMap.containsValue(irValue));
    for (var entrySet : basicBlockValueMap.entrySet()) {
      if (entrySet.getValue()
                  .equals(irValue)) return entrySet.getKey();
    }
    throw new IllegalStateException();
  }

  @Override
  public Operand getOperand() {
    return new PhiOperand(this);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    return new ArrayList<>(basicBlockValueMap.values());
  }

  @Override
  public boolean replaceValue(
      IrValue oldName,
      IrValue newName
  ) {
    var replaced = false;
    for (BasicBlock basicBlock : basicBlockValueMap.keySet()) {
      var abstractName = basicBlockValueMap.get(basicBlock);
      if (abstractName.equals(oldName)) {
        basicBlockValueMap.put(
            basicBlock,
            newName
        );
        replaced = true;
      }
    }
    return replaced;
  }

  @Override
  public void accept(AsmWriter asmWriter) {
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    var allNames = genOperandIrValuesSurface();
    allNames.add(getDestination());
    return allNames;
  }

  @Override
  public Instruction copy() {
    return new Phi(
        (IrSsaRegister) getDestination(),
        basicBlockValueMap
    );
  }

  @Override
  public Optional<Operand> getOperandNoArray() {
    return Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Phi phi = (Phi) o;
    return Objects.equals(
        basicBlockValueMap.values(),
        phi.basicBlockValueMap.values()
    ) && Objects.equals(
        getDestination(),
        phi.getDestination()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(basicBlockValueMap.values());
  }

  @Override
  public String toString() {
    String rhs = basicBlockValueMap.values()
                                   .stream()
                                   .map(IrValue::toString)
                                   .collect(Collectors.joining(", "));
    return String.format(
        "%s%s %s: %s = phi(%s)",
        DOUBLE_INDENT,
        getPrefix(),
        getDestination(),
        getDestination().getType()
                        .getSourceCode(),
        rhs
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    String rhs = basicBlockValueMap.values()
                                   .stream()
                                   .map(IrValue::toString)
                                   .collect(Collectors.joining(", "));
    return String.format(
        "%s%s %s: %s = %s(%s)",
        DOUBLE_INDENT,
        getPrefixSyntaxHighlighted(),
        getDestination(),
        getDestination().getType()
                        .getColoredSourceCode(),
        Utils.coloredPrint(
            "phi",
            Utils.ANSIColorConstants.ANSI_GREEN_BOLD
        ),
        rhs
    );
  }
}

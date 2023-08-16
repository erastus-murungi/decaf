package decaf.ir.names;


import java.util.function.Predicate;

public class IrValuePredicates {
  public static Predicate<IrValue> isRegisterAllocatable() {
    return irValue -> (irValue instanceof IrRegisterAllocatable);
  }

  public static Predicate<IrValue> isSsaRegister() {
    return irValue -> (irValue instanceof IrSsaRegister);
  }

  public static Predicate<IrValue> isConstant() {
    return irValue -> (irValue instanceof IrConstant);
  }

  public static Predicate<IrValue> isNonConstant() {
    return irValue -> !(irValue instanceof IrConstant);
  }

  public static Predicate<IrValue> isAssignable() {
    return irValue -> (irValue instanceof IrAssignable);
  }

  public static Predicate<IrValue> isGlobal() {
    return irValue -> irValue instanceof IrGlobalArray || irValue instanceof IrGlobalScalar;
  }
}

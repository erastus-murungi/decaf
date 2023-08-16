package decaf.ir.codes;


import decaf.ir.cfg.BasicBlock;

public interface WithTarget {
  BasicBlock getTarget();

  void setTargetWithTributary(BasicBlock newTarget);

  default void replaceTarget(BasicBlock newTarget) {
    var oldTarget = getTarget();
    setTargetWithTributary(newTarget);
    oldTarget.removeTributary(this);
  }
}

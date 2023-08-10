package decaf.codegen.codes;


import decaf.cfg.BasicBlock;

public interface WithTarget {
  BasicBlock getTarget();

  void setTargetWithTributary(BasicBlock newTarget);

  default void replaceTarget(BasicBlock newTarget) {
    var oldTarget = getTarget();
    setTargetWithTributary(newTarget);
    oldTarget.removeTributary(this);
  }
}

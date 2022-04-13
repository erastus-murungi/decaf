package edu.mit.compilers.dataflow;

import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.Objects;

public class LocalCSE {
    public class AEBinExp {
        private Integer position;
        private AbstractName operandLeft;
        private String operator;
        private AbstractName operandRight;
        private AssignableName result;

        public AEBinExp(Builder binExpBuilder) {
            this.position = binExpBuilder.position;
            this.operandLeft = binExpBuilder.operandLeft;
            this.operator = binExpBuilder.operator;
            this.operandRight = binExpBuilder.operandRight;
            this.result = binExpBuilder.result;
        }

        public class Builder {
            int position;
            AbstractName operandLeft;
            String operator;
            AbstractName operandRight;
            AssignableName result;

            public Builder setPosition(int position) {
                this.position = position;
                return this;
            }

            public Builder setOperandLeft(AbstractName operandLeft) {
                this.operandLeft = operandLeft;
                return this;
            }

            public Builder setOperator(String operator) {
                this.operator = operator;
                return this;
            }

            public Builder setOperandRight(AbstractName operandRight) {
                this.operandRight = operandRight;
                return this;
            }

            public Builder setResult(AssignableName result) {
                this.result = result;
                return this;
            }

            public AEBinExp build() {
                AEBinExp aeBinExp = new AEBinExp(this);
                validateAEBinExpObject(aeBinExp);
                return aeBinExp;
            }

            private void validateAEBinExpObject(AEBinExp aeBinExp) {
                Objects.requireNonNull(aeBinExp.operandLeft);
                Objects.requireNonNull(aeBinExp.operandRight);
                Objects.requireNonNull(aeBinExp.operator);
                Objects.requireNonNull(aeBinExp.position);
            }
        }
    }
}

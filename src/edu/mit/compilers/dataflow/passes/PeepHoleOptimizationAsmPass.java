package edu.mit.compilers.dataflow.passes;

import java.util.Optional;

import edu.mit.compilers.asm.X64Code;
import edu.mit.compilers.asm.X64CodeConverter;
import edu.mit.compilers.asm.X64Instruction;
import edu.mit.compilers.asm.X64Program;

public class PeepHoleOptimizationAsmPass {
    X64Program x64Program;
    private int numInstructionsRemoved = 0;

    public int getNumInstructionsRemoved() {
        return numInstructionsRemoved;
    }

    public PeepHoleOptimizationAsmPass(X64Program x64Program) {
        this.x64Program = x64Program;
    }

    static class Move {
        public final String src;
        public final String dst;

        public Move(String src, String dst) {
            this.src = src;
            this.dst = dst;
        }

        private static String[] tokenize(X64Code x64Code) {
            String[] split = x64Code.toString()
                    .split("\\s+");
            for (int i = 0; i < split.length; i++) {
                if (split[i].endsWith(",")) {
                    split[i] = split[i].substring(0, split[i].length() - 1);
                }
            }
            return split;
        }

        public static Optional<Move> fromX64Code(X64Code x64Code) {
            if (isMov(x64Code)) {
                var tokens = tokenize(x64Code);
                if (!tokens[1].contains("mov")) {
                    throw new IllegalArgumentException();
                }
                final String src = tokens[2];
                final String dst = tokens[3];
                return Optional.of(new Move(src, dst));
            }
            return Optional.empty();
        }

        @Override
        public String toString() {
            return String.format("%s    %s, %s", "movq", src, dst);
        }

        private static boolean isMov(X64Code x64Code) {
            return x64Code.toString().contains("mov");
        }
    }

    public boolean run() {
        removeRedundantMoves();
        removeRedundantConsecutiveMoves();
        return numInstructionsRemoved != 0;
    }

    private boolean isConstant(String loc) {
        return loc.startsWith("$");
    }

    private boolean isRegister(String loc) {
        return loc.startsWith("%");
    }

    private void removeRedundantMoves() {
        int indexOfX64Code = 0;
        int programSize = x64Program.size();
        while (indexOfX64Code < programSize) {
            var mov = Move.fromX64Code(x64Program.get(indexOfX64Code));
            if (mov.isPresent()) {
                var movInst = mov.get();
                var prevMov = Move.fromX64Code(x64Program.get(indexOfX64Code - 1));
                if (prevMov.isPresent()) {
                    var prevMovInst = prevMov.get();
                    if (prevMovInst.dst.equals(movInst.src)) {
                        if (isConstant(prevMovInst.src) || isRegister(prevMovInst.src)) {
                            // in the case of an instruction involving the %rax
                            //
                            //      1:   addq	-16(%rbp), %rax
                            //      2:   movq	%rax, -32(%rbp)	    (prevMovInst)
                            //      3:   mov 	-32(%rbp), %rax     (movInst)
                            // we do not need the third instruction
                            // remove `movInst`
                            if (!prevMovInst.src.equals("%al")) {
                                x64Program.set(indexOfX64Code, X64CodeConverter.x64InstructionLine(X64Instruction.movq, prevMovInst.src, movInst.dst));
                                var newSrc = prevMovInst.src;
                                if (newSrc.equals(movInst.dst)) {
                                    x64Program.remove(indexOfX64Code);
                                    programSize = x64Program.size();
                                    numInstructionsRemoved++;
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
            indexOfX64Code++;
        }
    }

    private void removeRedundantConsecutiveMoves() {
        int indexOfX64Code = 0;
        int programSize = x64Program.size();
        while (indexOfX64Code < programSize) {
            var mov = Move.fromX64Code(x64Program.get(indexOfX64Code));
            if (mov.isPresent()) {
                var movInst = mov.get();
                var prevMov = Move.fromX64Code(x64Program.get(indexOfX64Code - 1));
                if (prevMov.isPresent()) {
                    var prevMovInst = prevMov.get();
                        if (prevMovInst.dst.equals(movInst.dst)) {
                            /* Consider the case of:
                            movq	$0, -8(%rbp)	 # x = 0
	                        movq	%rdi, -8(%rbp)
	                        // we remove the first one
                            */
                            x64Program.remove(indexOfX64Code - 1);
                            programSize = x64Program.size();
                            numInstructionsRemoved++;
                            continue;
                        }
                }
            }
            indexOfX64Code++;
        }
    }
}

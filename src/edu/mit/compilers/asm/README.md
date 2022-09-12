How ASM generation works:

```python
def map_arg_sources(method): 
	for method_param in method.params:
		P[method][method_param] = free register or stack location

for global_value in program.prologue:
	G[global_value] = X64Operand(global_value)

for method in program.methods:
	for instruction in method:
		for value in instruction:
			M[method][value] = X64Operand(value)

for param in method.param:
	mov from (corresponding arg register or stack) to P[method][param]
for instruction_list in method.instruction_list:
	if should_add_label(instruction_list):
		emit_label(instruction_list.label)
	for instruction in instruction_list:
		emit_X64_for(instruction)

 ```

Commands to debug with:
```
erastusmurungi@C02F257ZMD6M compiler % as -o test.o test.s -g
erastusmurungi@C02F257ZMD6M compiler % gcc -o main test.o -g 
erastusmurungi@C02F257ZMD6M compiler % gdb main
```


### Notes on LLDB debugging:

- To set a breakpoint:
  `breakpoint set -f foo.s -l 12`
- To start the program and stop at the beggining
  `process launch --stop-at-entry`
- To see the value of registers
  `register read`
- To see the value of one register
  `register read rsp`
- Print 8 stack values
  `memory read -fu -s8 -c8 $rsp`
- Step into a function
  `thread step-in`



# Notes
If you decide to callee save and restore an odd number of registers, make sure to check the register alignment
Generating assembly:
  `clang -S -mllvm --x86-asm-syntax=att scratch.c -o scratch.s -fno-asynchronous-unwind-tables`

Sanjana Jadhav

## CPU.java
- simulates the CPU
- fetches instructions from memory and executes it
- can also write to memory
- handles interrupts and syscalls 

## Memory.java
- simulates memory
- reads from a file and compiles a memory array
- sends/receives addresses and data from the CPU

## How to Compile & Run the Program
javac Memory.java CPU.java
- args[0] = filename and args[1] = interrupt interval
java CPU sample5.txt 30

## sample5.txt
This sample file will print out the "3" multiplication table (up to 3x10).
- In the beginning, AC, X and Y will equal 3. 
- After each interaction, the AC value will be stored in X and a newline will be loaded into AC through a branch statement.
- Then, the original (numeric) AC value is copied to AC by X, and Y is added to it (increments 3 each time)
- It will print 3-30, each iteration on a new line

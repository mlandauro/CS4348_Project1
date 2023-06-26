Files:
CPU.java
   Memory class:
   - takes in file name from CPU
   - reads provided file and puts instructions into memory
   - polls output stream for commands from CPU and processes accordingly
   - read send back corresponding item in memory
   - write puts given item into memory

   CPU class:
   - takes in filename and timeout value
   - creates memory process and sends file name to memory
   - communicates with memory to get next instruction
   - executes given instruction
   - calls timer interrupt when timeout is reached

How to compile and run
 - to COMPILE type "javac CPU.java" in concole
 - to RUN type "java CPU [filename] [timeout]"
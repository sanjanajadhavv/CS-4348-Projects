import java.io.*;
import java.lang.Runtime;
import java.util.Scanner;
import java.util.Random;

public class CPU {
    // registers are global variables
    static int PC = 0; // user program starts at 0
    static int interruptInterval = 0; // user enters this value in command line (timer interrupt)
    static int IR = 0; // instruction register
    static int AC = 0; // accumulator
    static int X = 0;
    static int Y = 0;
    static int SP = 1000;

    // user mode and system mode
    static boolean user = true;
    static boolean system = false;
    static boolean currentInterrupt = false; // ensures multiple interrupts don't occur at once
    static int numTillInterrupt = 0; // keeps incrementing till it equals interruptInterval, then resets
    static boolean endOfIns = false;

    static int systemStackIndex = 2000; // decrements from this index
    static int userStackIndex = 1000; // decrements from this index

    public static void main(String args[]) {

        // set filename and interrupt interval number
        String fileName = "";
        if (args.length == 2) {
            fileName = args[0];
            interruptInterval = Integer.parseInt(args[1]);
        } else if (args.length < 2) {
            System.out.println("Please enter 2 arguments!! --> fileName and interruptInterval");
            System.exit(0); // exit
        } else {
            System.out.println("Please enter 2 arguments only!! --> fileName and interruptInterval");
            System.exit(0); // exit
        }

        try {
            Runtime rt = Runtime.getRuntime();

            Process proc = rt.exec("java Memory");

            InputStream is = proc.getInputStream();
            Scanner sc = new Scanner(is); // read from the input stream
            OutputStream os = proc.getOutputStream();
            PrintWriter pw = new PrintWriter(os); // write to memory (child)

            // send the fileName to the Memory Process (child)
            pw.printf(fileName + "\n");
            pw.flush();
            int instruction = 0;
            int localVar = 0;
            int temp = 0;

            // this loop will keep the communication going between CPU and memory
            while (!endOfIns) {

                // if both are equal
                if (numTillInterrupt > 0 && (numTillInterrupt == interruptInterval)) {
                    if (!currentInterrupt) { // ensures only one interrupt at a time
                        user = false;
                        system = true; // switch to kernel
                        currentInterrupt = true;

                        localVar = SP;
                        SP = systemStackIndex;

                        SP--; // decrement
                        write(SP, pw, localVar); // write --> // save original SP on system stack

                        temp = PC++; // increment PC to start again at next index

                        SP--; // decrement
                        write(SP, pw, temp); // write --> save original PC on system stack
                        PC = 1000; // PC is at 1000 during timer interrupt

                        numTillInterrupt = 0; // reset num of instructions till next timer interrupt
                    }
                }

                // get the IR number
                instruction = read(PC, pw, sc);

                if (instruction == -10) { // error value or end of instructions
                    endOfIns = true;
                    break; // end loop
                } else {
                    execute(instruction, is, os, pw, sc); // execute
                }
            }

            proc.waitFor();
            int exitVal = proc.exitValue();
            System.out.println("Process Exited: " + exitVal);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // return value at a specific address
    private static int read(int currentAddress, PrintWriter pw, Scanner sc) {

        // check if illegal memory access is occurring
        if (currentAddress >= 1000) {
            if (user) {
                System.out.println("Memory violation: accessing system address 1000 in user mode!");
                System.exit(0);
            }
        }

        // string contains: action & address
        // send to child
        pw.printf("r" + " " + currentAddress + "\n");
        pw.flush();

        int insAtAddress = 0;
        if (sc.hasNext()) {
            insAtAddress = Integer.parseInt(sc.next()); // get the instruction at the address from Child
            // System.out.println("ins: " + insAtAddress);
            return (insAtAddress);
        }

        return -10; // if it reaches end of memory or if there is an error in reading
    }

    // writes value at a specific address
    private static void write(int currentAddress, PrintWriter pw, int data) {

        // string contains: action, value, address
        // send to child
        pw.printf("w" + " " + data + " " + currentAddress + "\n");
        pw.flush();
    }

    // pop from stack (system)
    private static int pop(int currentAddress, PrintWriter pw, Scanner sc) {
        int value = read(SP, pw, sc); // read
        SP++; // go back up
        return value; // return popped value
    }

    // executes instructions
    private static void execute(int currentInstruction, InputStream is, OutputStream os,
            PrintWriter pw, Scanner sc) {

        // load the current instruction into IR
        IR = currentInstruction;
        int localVar = 0; // local variable
        int temp = 0;

        switch (IR) {
            case 1:
                // load the value into the AC --> Load value
                // System.out.println("in " + 1);
                PC++; // get next value
                AC = read(PC, pw, sc);
                // System.out.println("AC 1: " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 2:
                // load the value at the address into the AC --> Load addr
                // System.out.println("in " + 2);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                // System.out.println("local 2: " + localVar);

                // check if illegal memory access is occurring
                if (localVar >= 1000) {
                    if (user) {
                        System.out.println("Memory violation: accessing system address 1000 in user mode!");
                        System.exit(0);
                    }
                }

                AC = read(localVar, pw, sc); // load from the address from localVar's location into AC
                // System.out.println("AC 2: " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 3:
                // 1) load the value from the 2) address found in the 3) given address into the
                // AC -->
                // LoadInd addr
                // System.out.println("in " + 3);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable

                localVar = temp;

                for (int i = 0; i < 2; i++) {
                    temp = read(temp, pw, sc); // find the address stored at localVar
                }

                AC = temp; // load from the address from temp's location into AC
                // System.out.println("ac 3 " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 4:
                // Load the value at (address+X) into the AC --> LoadIdxX addr
                // System.out.println("in " + 4);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                // System.out.println("local 4 " + localVar + " X " + X);
                temp = localVar + X; // add X
                // System.out.println("temp 4 " + temp);
                AC = read(temp, pw, sc); // load from the address into AC
                // System.out.println("ac 4 " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 5:
                // Load the value at (address+Y) into the AC --> LoadIdxY addr
                // System.out.println("in " + 5);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                temp = localVar + Y; // add Y
                AC = read(temp, pw, sc); // load from the address into AC

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 6:
                // Load from (Sp+X) into the AC --> LoadSpX
                // System.out.println("in " + 6);
                localVar = SP + X;
                AC = read(localVar, pw, sc); // load from the address into AC

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 7:
                // Store the value in the AC into the address --> Store addr
                // System.out.println("in " + 7);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address
                write(localVar, pw, AC); // write to the address

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 8:
                // gets a random int from 1-100 into the AC --> Get
                // System.out.println("in " + 8);
                // Source to find a Random Number:
                // https://java2blog.com/generate-random-number-between-1-and-100-java/
                Random rand = new Random();
                int randNum = rand.nextInt(100) + 1;
                AC = randNum;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 9:
                // If port=1, writes AC as an int to the screen
                // If port=2, writes AC as a char to the screen --> Put port
                // System.out.println("in " + 9);

                PC++; // increment to read port number
                localVar = read(PC, pw, sc);
                // System.out.println("ac 9 " + AC);

                if (localVar == 1) { // print int
                    System.out.print(AC);

                    // next ins
                    PC++;
                    if (!currentInterrupt)
                        numTillInterrupt++;
                    break;
                } else if (localVar == 2) { // print char
                    System.out.print((char) AC);

                    // next ins
                    PC++;
                    if (!currentInterrupt)
                        numTillInterrupt++;
                    break;
                }

            case 10:
                // add the value in X to AC --> AddX
                // System.out.println("in " + 10);
                AC = X + AC;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 11:
                // add the value in Y to AC --> AddY
                // System.out.println("in " + 11);
                AC = Y + AC;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 12:
                // subtract the value in X from the AC --> SubX
                // System.out.println("in " + 12);
                // System.out.println("ac 12 " + AC);
                AC = AC - X;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 13:
                // subtract the value in Y from the AC --> SubY
                // System.out.println("in " + 13);
                AC = AC - Y;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 14:
                // copy the value in the AC to X --> CopyToX
                // System.out.println("in " + 14);
                X = AC;
                // System.out.println(" X 14 " + X);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 15:
                // copy the value in X to the AC --> CopyFromX
                // System.out.println("in " + 15);
                // System.out.println("AC " + AC);
                AC = X;
                // System.out.println("AC 15 " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 16:
                // copy the value in the AC to Y --> CopyToY
                // System.out.println("in " + 16);
                Y = AC;
                // System.out.println(Y);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 17:
                // copy the value in Y to the AC --> CopyFromY
                // System.out.println("in " + 17);
                AC = Y;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 18:
                // copy the value in AC to the SP --> CopyToSP
                // System.out.println("in " + 18);
                SP = AC;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 19:
                // copy the value in SP to the AC --> CopyFromSP
                // System.out.println("in " + 19);
                AC = SP;
                // System.out.println("AC 19: " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 20:
                // Jump to the address --> Jump addr
                // System.out.println("in " + 20);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                PC = localVar; // update PC

                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 21:
                // Jump to the address only if the value in the AC is zero --> JumpIfEqual addr
                // System.out.println("in " + 21);
                PC++; // get next value
                // System.out.println("ac 21 " + AC);

                if (AC == 0) {
                    localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                    PC = localVar; // update PC
                    if (!currentInterrupt)
                        numTillInterrupt++;
                    break;
                }

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 22:
                // Jump to the address only if the value in the AC is not zero --> JumpNotEqual
                // addr
                // System.out.println("in " + 22);
                PC++; // get next value
                if (AC != 0) {
                    localVar = read(PC, pw, sc); // get the address and store in the localVar variable
                    PC = localVar; // update PC
                    if (!currentInterrupt)
                        numTillInterrupt++;
                    break;
                }

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 23:
                // Push return address onto stack, jump to the address --> Call addr
                // notes: push = go down
                // System.out.println("in " + 23);
                PC++; // get next value
                localVar = read(PC, pw, sc); // get the address and store in the localVar variable

                // push on stack --> write
                PC++; // next PC

                SP--; // decrement
                write(SP, pw, PC); // write --> current PC number is on stack

                // update PC
                PC = localVar; // new address to jump to
                userStackIndex = SP; // update SP -> new address to store values at

                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 24:
                // Pop return address from the stack, jump to the address --> Ret
                // notes: pop = go back up
                // System.out.println("in " + 24);
                // pop --> read from stack
                localVar = pop(SP, pw, sc);

                // return original PC address
                PC = localVar;

                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 25:
                // increment the value in X --> IncX
                // System.out.println("in " + 25);
                X = X + 1;
                // System.out.println("x 25 " + X);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 26:
                // decrement the value in X --> IncX
                // System.out.println("in " + 26);
                X--;

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 27:
                // Push AC onto stack -> Push
                // notes: push = go down
                // System.out.println("in " + 27);
                // push on stack --> write

                SP--; // decrement
                write(SP, pw, AC); // write --> save AC on stack
                // System.out.println("AC 27: " + AC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 28:
                // Pop from stack into AC --> Pop
                // notes: pop = go back up
                // System.out.println("in " + 28);
                // pop --> read from stack
                AC = pop(SP, pw, sc);
                // System.out.println("AC 28: " + AC);
                // System.out.println("PC 28: " + PC);

                // next ins
                PC++;
                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 29:
                // Perform system call --> Int
                // System.out.println("in " + 29);
                user = false;
                system = true; // enter kernel mode
                currentInterrupt = true;

                // save SP
                localVar = SP;
                // save PC
                PC = PC + 1;
                temp = PC;

                // save SP and PC on stack
                SP = 2000; // switch to system stack
                SP--; // decrement
                write(SP, pw, localVar); // write --> save SP address at 2000

                PC = 1500; // The int instruction should cause execution at address 1500
                SP--; // decrement
                write(SP, pw, temp); // write --> save PC address at 1500

                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 30:
                // Return from system call --> IRet
                // System.out.println("in " + 30);
                // return registers

                // read/pop PC first because it was pushed last
                PC = pop(SP, pw, sc);
                SP = pop(SP, pw, sc);

                system = false;
                user = true; // return to user mode
                currentInterrupt = false;

                if (!currentInterrupt)
                    numTillInterrupt++;
                break;

            case 50:
                // end execution --> End
                // System.out.println("in " + 50);
                if (!currentInterrupt)
                    numTillInterrupt++;

                System.exit(0);
                break;

            default:
                System.out.println("error!");
                System.exit(0);
                break;
        }
    }
}
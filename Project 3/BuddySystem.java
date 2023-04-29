// CS 4348.001
// Sanjana Jadhav

import java.io.File;
import java.util.Scanner;
import java.util.*;
import java.io.FileNotFoundException;

public class BuddySystem {

    public static void main(String[] args) {
        String[] data;
        int memSize = 0; // store size of block
        int counter = 65; // keep track of process name (calculation)
        String processName; // keep track of process name

        // keep track of free memory blocks: (i list)
        List<MemoryBlock> memory = new ArrayList<>();

        // add root node
        memory.add(new MemoryBlock("", 1024, 1024));

        // print initial memory available
        System.out.println("Beginning Memory:");
        System.out.print("|  " + memory.get(0).processName + " " + memory.get(0).memSize + "K  |");
        System.out.println();
        System.out.println();

        // read input.txt
        try {
            File f = new File("input.txt");
            Scanner sc = new Scanner(f);
            while (sc.hasNextLine()) {
                data = sc.nextLine().split(" "); // split input

                // request memory
                if (data[0].equals("Request")) {

                    // get amount of memory
                    memSize = Integer.parseInt(data[1].substring(0, data[1].length() - 1));

                    // print statement
                    System.out.println("Request " + memSize + "K");

                    // error message if memory size is not within range
                    if (memSize < 64 || memSize > 1024) {
                        System.out.println("Cannot allocate less than 64K or more than 1024K! Terminating!");
                        System.exit(0);
                    }

                    // create process name
                    char name = (char) (counter++);

                    // call addMem
                    addMem(memSize, String.valueOf(name), memory, 0);

                    for (int i = 0; i < memory.size(); i++) {

                        System.out.print("|  " + memory.get(i).processName + " " + memory.get(i).memSize + "K  ");
                    }
                    System.out.print("|");
                    System.out.println();
                    System.out.println();

                } else if (data[0].equals("Release")) { // release memory

                    // get process name (char)
                    processName = String.valueOf(data[1].charAt(0));

                    // call freeMem
                    boolean valid = freeMem(processName, memory);

                    // print error
                    if (valid == false) {
                        System.out.println("Process " + processName + " does not Exist!");
                        System.out.println();
                    } else {

                        // print statement
                        System.out.println("Release Process " + processName);
                        for (int i = 0; i < memory.size(); i++) {

                            System.out.print("|  " + memory.get(i).processName + " " + memory.get(i).memSize + "K  ");
                        }
                        System.out.print("|");
                        System.out.println();
                        System.out.println();
                    }
                }
            }

            sc.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    // request memory: uses recursion
    public static boolean addMem(int actualSize, String processName, List<MemoryBlock> memory, int index) {
        int sizeOfMemory = memory.size(); // size of memory arr
        int updatedSize = 0;

        // case 1: if memory block is available and greater than actual size needed
        if (memory.get(index).memSize > actualSize && memory.get(index).available) {

            // split size of the current memory block
            updatedSize = memory.get(index).memSize / 2;

            // if memory size is still too big after splitting, continue
            // Textbook
            // Otherwise, the block is split into two equal buddies of size 2U - 1. If 2U-2
            // < s <= 2U-1,then the request is allocated to one of the two buddies
            if (actualSize <= updatedSize) {
                int leftBudIndex = index;
                int rightBudIndex = index + 1;

                // Remove the current memory block from the list
                // Textbook
                // A hole may be removed from the (i + 1) list by splitting it in half to create
                // two buddies of size 2^i in the i list
                memory.remove(index);

                // create new left block
                MemoryBlock leftBlock = new MemoryBlock(processName, updatedSize, actualSize);
                memory.add(leftBudIndex, leftBlock);

                // create new right block
                MemoryBlock rightBlock = new MemoryBlock("", updatedSize, 0);
                memory.add(rightBudIndex, rightBlock);

                // If the process size is still too big, recursive call to the left buddy (split
                // again)
                if (actualSize != updatedSize) {
                    addMem(actualSize, processName, memory, leftBudIndex);
                }

            } else {
                // "If a request of size s such that 2U-1 < s <= 2U is made, then the entire
                // block is allocated" --> Textbook

                memory.get(index).processName = processName;
                memory.get(index).actualSize = actualSize;
                memory.get(index).available = false;

            }
        } else if (!memory.get(index).available || memory.get(index).memSize < actualSize) {
            // case 2: if current
            // memory block size
            // is too small or if it is
            // taken

            if (index == sizeOfMemory - 1) {
                // no blocks available
                System.out.println(
                        "TERMINATING: Unable to allocate memory for " + processName + " (size " + actualSize + "K)");
                System.exit(0);
            } else {
                // Recursive call to check next memory block
                return addMem(actualSize, processName, memory, index + 1);
            }

        } else if (memory.get(index).memSize == actualSize) { // if equal size, then check if block is free
            if (memory.get(index).available && memory.get(index).processName.equals("")) {

                // assign values
                memory.get(index).processName = processName;
                memory.get(index).actualSize = actualSize;
                memory.get(index).available = false;

            } else {
                return addMem(actualSize, processName, memory, index + 1); // recursive call if not free
            }
        }

        return true;
    }

    // release memory: uses recursion
    public static boolean freeMem(String processName, List<MemoryBlock> memory) {

        int index = -1;

        // iterate through memory list and find the process
        for (int i = 0; i < memory.size(); i++) {
            String currentProcessName = memory.get(i).processName;
            // if they match, store the index
            if (currentProcessName.equals(processName)) {
                index = i;
                break;
            }
        }

        // if the index has not been found, print error
        if (index == -1) {
            return false;

        } else if (index >= 0) {

            memory.get(index).processName = ""; // remove process name
            memory.get(index).available = true; // block becomes available

            // combine memory blocks if they are free
            merge(index, memory);
        }

        return true;

    }

    // merge memory blocks: uses recursion
    public static boolean merge(int index, List<MemoryBlock> memory) {
        boolean validMerge = false;

        // if only one block, then it is 1024
        if (memory.size() == 1) {
            memory.get(0).processName = "";
            memory.get(0).memSize = 1024;
        }

        // if there are only 2 elements
        else if (memory.size() == 2) {
            MemoryBlock block1 = memory.get(0);
            MemoryBlock block2 = memory.get(1);

            // check if they are available and their sizes are equal
            if (block1.available && block2.available && block1.memSize == block2.memSize
                    && block1.processName.equals("") && block2.processName.equals("")) {
                int newSize = block1.memSize * 2; // calculate new size

                // remove from list
                memory.remove(1);
                memory.remove(0);

                // create new block and add to list
                MemoryBlock mergedBlock = new MemoryBlock("", newSize, 1024);
                memory.add(0, mergedBlock);
                return true;
            }
        } else {

            // store current memory block
            MemoryBlock current = memory.get(index);

            int index1 = -1;
            int index2 = -1;
            int newIndex = -1; // updated index
            int xor = 0;

            List<Integer> xorVals = new ArrayList<>();

            // store xor values in arraylist
            for (int i = 0; i < memory.size(); i++) {
                xor = current.address ^ memory.get(i).address + memory.get(i).memSize;
                xorVals.add(xor);
            }

            // check if the xor values equal
            for (int i = 0; i < xorVals.size(); i++) {
                for (int j = i + 1; j < xorVals.size(); j++) {
                    if (xorVals.get(i).equals(xorVals.get(j))) {

                        // only store if they are available and close to each other
                        if (memory.get(i).available && memory.get(j).available && (Math.abs(j - i) == 1)) {
                            index1 = i;
                            index2 = j;
                            break;
                        }
                    }
                }

                // only break if the index values are not -1
                if (index1 >= 0 && index2 >= 0)
                    break;
            }

            // check if they're available, equal size, and don't have a process assigned
            if (index1 >= 0 && index2 >= 0 && (memory.get(index1).available && memory.get(index2).available)
                    && (memory.get(index1).memSize == memory.get(index2).memSize)
                    && memory.get(index1).processName.equals("") && memory.get(index2).processName.equals("")) {

                // get new index
                newIndex = Math.min(index1, index2);

                // get new size
                int newSize = memory.get(index1).memSize * 2;

                // remove old memory blocks
                memory.remove(Math.max(index1, index2));
                memory.remove(Math.min(index1, index2));

                // create new block and add to list
                MemoryBlock mergedBlock = new MemoryBlock("", newSize, 0);
                memory.add(newIndex, mergedBlock);
                validMerge = true;
            }

            if (validMerge) {
                // Recursive call to merge again if needed
                merge(newIndex, memory);
            }
        }

        return true;
    }

    // Memory Block Class
    static class MemoryBlock {
        public String processName = ""; // process name
        public int memSize; // size of memory block acquired (power of 2)
        public int actualSize; // actual size of process
        public boolean available; // free or not
        public int address; // store address

        // create other memory sizes
        MemoryBlock(String processName, int memSize, int actualSize) {
            this.processName = processName;
            this.memSize = memSize;
            this.actualSize = actualSize;
            this.available = true;
            this.address = memSize;
        }
    }
}

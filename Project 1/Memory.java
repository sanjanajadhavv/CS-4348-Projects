import java.io.File;
import java.util.Scanner;

public class Memory {
    // global variable to store memory
    static int[] memoryArr = new int[2000];
    static boolean endOfInput = false;

    public static void main(String args[]) {
        try {

            // scanner obj
            Scanner sc = new Scanner(System.in);

            String fileName = "";
            File f = null;
            if (sc.hasNext()) {

                // get fileName from CPU (parent)
                fileName = sc.nextLine();
                f = new File(fileName);

                // error if file does not exist
                if (f.exists() == false) {
                    System.out.println("File does not exist!");
                    System.exit(0); // exit
                }

            }

            // transfer file contents to memory array
            readMemory(f);

            String line;
            int address = 0;
            int data = 0;
            int value = 0;

            while (!endOfInput) {
                if (sc.hasNext()) {
                    line = sc.nextLine(); // read full string

                    // if "r", then read
                    if (line.contains("r")) {
                        address = Integer.parseInt(line.substring(2, line.length())); // get address
                        value = read(address);
                        System.out.println(value);// send to Parent (print)
                    }

                    // if "w", then write
                    else if (line.contains("w")) {
                        String[] input = line.split(" "); // split by spaces
                        address = Integer.parseInt(input[2]);
                        data = Integer.parseInt(input[1]);
                        write(address, data); // call write function
                    }
                } else {
                    endOfInput = true;
                    break;
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // read from file and build the memory array
    private static void readMemory(File f) {

        try {
            int index = 0; // index incrementer for memory array
            int currentVal = 0;
            String current = "";
            String temp = "";

            // use file as input for scanner object
            Scanner sc = new Scanner(f);
            while (sc.hasNext()) {

                // processing for //, empty, .
                if (!sc.hasNextInt()) {

                    current = sc.next();
                    if (current.charAt(0) == '.') { // if it contains a memory address
                        temp = current.substring(1, current.length());
                        index = Integer.parseInt(temp);
                    } else if (current.contains("//")) { // comment: skip to next line
                        sc.nextLine();
                    } else if (current.length() == 0) { // empty line: skip to next line
                        sc.nextLine();
                    } else
                        sc.nextLine(); // multiple int tokens in one line, skip to next line

                } else if (sc.hasNextInt()) {
                    currentVal = sc.nextInt(); // read int val
                    memoryArr[index] = currentVal; // store in array
                    // System.out.println(memoryArr[index]);
                    index++; // increment array index
                }
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    // read(address) - returns the value at the address
    public static int read(int currentAddress) {
        return memoryArr[currentAddress]; // return value at address
    }

    // write(address, data) - writes the data to the address
    public static void write(int currentAddress, int data) {
        memoryArr[currentAddress] = data; // update value at address
    }

}

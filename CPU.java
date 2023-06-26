//MXL190030
//CS 4348.002
import java.io.*;
import java.util.Random;
import java.util.Scanner;

class Memory{
    static int mem[] = new int [2000]; //memory array 0-999 user program; 1000-1999 system code


    public static void main(String [] args) throws Exception{
        Scanner in = new Scanner(System.in);
        String filename = in.nextLine(); //gets file name from CPU
        File f = new File(filename);

        try{
            FileInputStream fstream = new FileInputStream(f);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

            int index = 0;
            String token;
            while((token = br.readLine()) != null){ //keep reading until end of file

                if(!token.isEmpty()) {
                    if (token.charAt(0) == '.') {//provided memory location in file
                        index = Integer.parseInt(token.substring(1)); // get rid of .
                        continue;
                    } else {
                        if(!Character.isDigit(token.charAt(0)))//check for line with only comment
                            continue;
                        mem[index] = new Scanner(token).useDelimiter("\\D+").nextInt();
                    }
                    index++;
                }
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //read what CPU wants
        String inputCPU;
        while((inputCPU = in.nextLine()) != null){//while CPU is giving stuff to read
            String t[] = inputCPU.split(" ");//command read
            int PC = Integer.parseInt(t[1]);

            if(t[0].equals("read")){//read
                System.out.println(mem[PC]);
            } else if(t[0].equals("write")){//write
                mem[PC] = Integer.parseInt(t[2]);
            }else
                break;
        }
    }
}

public class CPU {
    //REGISTERS
    static int PC = 0, SP = 999, IR, AC, X, Y, timer, count = 0;
    static boolean kernel = false;//true when in kernel mode
    static Scanner receive;
    static PrintWriter sent;

    public static void main(String [] args){
        File filename = new File(args[0]);
        timer = Integer.parseInt(args[1]);
        timer++;

        try{

            Runtime rt = Runtime.getRuntime();
            //create memory process
            Process proc = rt.exec("java Memory");

            InputStream input = proc.getInputStream();
            OutputStream output = proc.getOutputStream();

            //communication between CPU and memory
            receive = new Scanner(input);
            sent = new PrintWriter(output);

            //send filename to mem
            sent.printf(filename + "\n");
            sent.flush();

            //process program
            while(true){
                IR = readMem(PC++);//get instruction
                if(IR == 50) {//end if instruction is 50
                    proc.destroy();//destory memory process
                    break;
                }

                execute(IR);//execute instruction
                count++;

                //interrupt timer
                if(count == timer){
                    if(!kernel){//no interrupt if interrupt is happening
                        count = 0;
                        interrupt();//run interrupt
                        PC = 1000;
                    }
                }
            }
            //end program
            proc.waitFor();
            int exitVal = proc.exitValue();
            System.out.println("Proc done: " + exitVal);

        } catch (Throwable t){
            t.printStackTrace();
        }
    }

    public static void interrupt(){
        kernel = true;//in kernel mode
        int tempSP = SP;
        SP = 1999;
        push(tempSP);//push SP onto system stack
        push(PC);//push PC onto system stack
    }

    public static int readMem(int addr){
        //check in proper stack
        if(addr >= 1000 && !kernel){
            System.out.println("accessing restricted memory");
            System.out.println(-1);//output error
            return -1;
        }

        //send read request to memory
        sent.printf("read " + addr + '\n');
        sent.flush();
        return Integer.parseInt(receive.next());
    }

    public static void writeMem(int addr, int val){
        //send write request with memory
        sent.printf("write " + addr + " " + val + '\n');
        sent.flush();
    }

    public static void fetch(){ // get new instruction from memory
        IR = readMem(PC++);
    }

    public static void push(int data){ // put data onto stack
        writeMem(--SP, data);
    }

    public static int pop(){ // get data from stack
        return readMem(SP++);
    }

    //function for instruction execution
    static public void execute(int exc){
        switch(exc){
            case 1: //Load value
                fetch();
                AC = IR; // load value into AC
                break;
            case 2: //Load addr
                fetch();
                AC = readMem(IR);
                break;
            case 3: // LoadInd addr
                fetch();
                AC = readMem(readMem(IR));
                break;
            case 4: //LoadIdxX addr
                fetch();
                AC = readMem(IR + X);
                break;
            case 5: //LoadIdxY addr
                fetch();
                AC = readMem(IR + Y);
                break;
            case 6: //LoadSpX
                AC = readMem(SP + X);
                break;
            case 7: //Store addr
                fetch();
                writeMem(IR, AC);
                break;
            case 8: //Get
                Random rand = new Random();
                AC = rand.nextInt(100) + 1;
                break;
            case 9: //Put port
                fetch();
                if(IR == 1)
                    System.out.print((int) AC);
                else if(IR == 2)
                    System.out.print((char) AC);
                else{
                    System.out.println("Error: invalid operand value");
                }
                break;
            case 10: //AddX
                AC += X;
                break;
            case 11: //AddY
                AC += Y;
                break;
            case 12: //SubX
                AC -= X;
                break;
            case 13: //SubY
                AC -= Y;
                break;
            case 14: //CopyToX
                X = AC;
                break;
            case 15: //CopyFromX
                AC = X;
                break;
            case 16: //CopyToY
                Y = AC;
                break;
            case 17: //CopyFromY
                AC = Y;
                break;
            case 18: //CopyToSp
                SP = AC;
                break;
            case 19: //CopyFromSp
                AC = SP;
                break;
            case 20: //Jump addr
                fetch();
                PC = IR;
                break;
            case 21: //JumpIfEqual addr >> AC == 0
                fetch();
                if(AC == 0)
                    PC = IR;
                break;
            case 22: //JumpIfNotEqual addr >> AC != 0
                fetch();
                if(AC != 0)
                    PC = IR;
                break;
            case 23: //Call addr
                fetch();
                push(PC);
                PC = IR;
                break;
            case 24: //Ret
                PC = pop();
                break;
            case 25: //IncX
                X++;
                break;
            case 26: //DecX
                X--;
                break;
            case 27: //Push
                push(AC);
                break;
            case 28: //Pop
                AC = pop();
                break;
            case 29: //Int
                if(!kernel){//check that not in kernel mode to only run one interrupt at a time
                    interrupt();
                    PC = 1500;
                }
                break;
            case 30: //IRet
                //return to state before interrupt
                PC = pop();
                SP = pop();
                kernel = false;
                count = 0;

                break;
            case 50: //End
                break;
            default:
                break;
        }
    }
}

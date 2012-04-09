package com.minesnap.dcpu.assembler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AssemblerLauncher {
    public static void main(String[] args) {
        boolean optimize = true;
        List<String> argsList = new ArrayList<String>(2);
        Map<String, Integer> newNBOpcodes = new HashMap<String, Integer>();

        for(int i=0; i<args.length; i++) {
            switch(args[i].charAt(0)) {
            case '-':
                if(args[i].equals("-h") || args[i].equals("--help")) {
                    usage();
                    return;
                } else if(args[i].equals("--no-optimizations")) {
                    optimize = false;
                } else if(args[i].equals("-n") || args[i].equals("--new-nbopcode")) {
                    if(args.length <= i+2) {
                        System.err.println("-n/--new-nbopcode requires two arguments.");
                        usage();
                        System.exit(1);
                    }
                    String name = args[++i].toUpperCase();
                    int number = Integer.parseInt(args[++i]);
                    newNBOpcodes.put(name, number);
                } else {
                    System.err.println("Not a valid argument: "+args[i]);
                    usage();
                    System.exit(1);
                }
                break;
            default:
                argsList.add(args[i]);
            }
        }

        if(argsList.size() < 1 || argsList.size() > 2) {
            System.err.println("Wrong number of arguments.");
            usage();
            System.exit(1);
        }

        String filename = argsList.get(0);
        String outname;
        if(argsList.size() < 2)
            outname = "a.out";
        else
            outname = argsList.get(1);

        Assembler as = new Assembler();
        as.setOptimizations(optimize);
        as.setNewNBOpcodes(newNBOpcodes);

        try {
            as.assemble(filename, outname);
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find file "+filename);
            System.exit(2);
        } catch (CompileError e) {
            System.err.println("Compile Error: "+e.getMessage());
            System.exit(3);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(5);
        }
        System.out.println("Successfully compiled "+filename+" to "+outname);
    }

    public static void usage() {
        System.out.println("Parameters: [OPTION]... INPUTFILENAME [OUTPUTFILENAME]");
        System.out.println("Assembles INPUTFILENAME and writes the output to OUTPUTFILENAME.");
        System.out.println("Default OUTPUTFILENAME is \"a.out\".");
        System.out.println();
        System.out.println("Available options:");
        System.out.println(" -h, --help    Show this help message.");
        System.out.println(" --no-optimizations");
        System.out.println("               Disable automatic optimiziations.");
        System.out.println(" -n, --new-nbopcode name number");
        System.out.println("               Define a custom non-basic opcode. May be used more than once.");
    }
}

package com.minesnap.dcpu.assembler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class AssemblerLauncher {
    public static void main(String[] args) {
        boolean endianDecided = false;
        boolean littleEndian = true;
        boolean optimize = true;
        List<String> argsList = new ArrayList<String>(2);
        Map<String, Integer> newNBOpcodes = new HashMap<String, Integer>();

        for(int i=0; i<args.length; i++) {
            if(args[i].length() == 0)
                continue;
            switch(args[i].charAt(0)) {
            case '-':
                if(args[i].length() == 1) {
                    // Just "-" represents stdin or stdout
                    argsList.add(args[i]);
                } else if(args[i].equals("-h") || args[i].equals("--help")) {
                    usage();
                    return;
                } else if(args[i].equals("--no-optimizations")) {
                    optimize = false;
                } else if(args[i].equals("-b") || args[i].equals("--big-endian")) {
                    if(endianDecided) {
                        System.err.println("Error: You can't specify multiple endian types.");
                        usage();
                        System.exit(1);
                    }
                    endianDecided = true;
                    littleEndian = false;
                } else if(args[i].equals("-l") || args[i].equals("--little-endian")) {
                    if(endianDecided) {
                        System.err.println("Error: You can't specify multiple endian types.");
                        usage();
                        System.exit(1);
                    }
                    endianDecided = true;
                    littleEndian = true;
                } else if(args[i].equals("-n") || args[i].equals("--new-nbopcode")) {
                    if(args.length <= i+2) {
                        System.err.println("-n/--new-nbopcode requires two arguments.");
                        usage();
                        System.exit(1);
                    }
                    String name = args[++i].toUpperCase();
                    int number = Integer.parseInt(args[++i]);
                    if(newNBOpcodes.containsKey(name)) {
                        System.err.println("You may not specify multiple NB-opcodes with the same name: "+name);
                        usage();
                        System.exit(1);
                    }
                    if(newNBOpcodes.containsValue(number)) {
                        System.err.println("You may not specify multiple NB-opcodes with the same number: "+number);
                        usage();
                        System.exit(1);
                    }
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
        if(argsList.size() < 2) {
            if(filename.endsWith(".dasm16")) {
                outname = filename.substring(0,filename.length()-7)+".dcpu16";
            } else if(filename.endsWith(".dasm")) {
                outname = filename.substring(0,filename.length()-5)+".dcpu";
            } else {
                outname = "a.out";
            }
	} else {
            outname = argsList.get(1);
        }

        Assembler as = new Assembler();
        as.setLittleEndian(littleEndian);
        as.setOptimizations(optimize);
        as.setNewNBOpcodes(newNBOpcodes);

        try {
            as.assemble(filename);
            as.writeTo(outname);
        } catch (FileNotFoundException e) {
            System.err.println("Error: "+e.getMessage());
            System.exit(2);
        } catch (CompileError e) {
            System.err.println("Compile Error: "+e.getMessage());
            System.exit(3);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(5);
        }
        System.out.println("Successfully assembled "+filename+" to "+outname);
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
        System.out.println(" -l, --little-endian");
        System.out.println("               Output little endian binaries (default).");
        System.out.println(" -b, --big-endian");
        System.out.println("               Output big endian binaries.");
        System.out.println(" -n, --new-nbopcode name number");
        System.out.println("               Define a custom non-basic opcode. May be used more than once.");
    }
}

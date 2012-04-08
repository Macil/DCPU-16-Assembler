package com.minesnap.dcpu.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class Assembler {
    public static void main(String[] args) {
        if(args.length != 1) {
            usage();
            System.exit(1);
        }
        try {
            assemble(args[0], "a.out");
        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not find file "+args[0]);
            System.exit(2);
        } catch (CompileError e) {
            System.err.println("Compile Error: "+e.getMessage());
            System.exit(3);
        } catch (IOException e) {
            System.err.println(e);
            System.exit(5);
        }
    }

    public static void assemble(String filename, String outname)
        throws FileNotFoundException, CompileError, IOException {
        Scanner sc = new Scanner(new File(filename), "UTF-8");
        List<Token> tokens = ASMTokenizer.tokenize(sc, filename);
        ResolverList resolvables = new ResolverList();
        Iterator<Token> tokensI = tokens.listIterator();
        while(tokensI.hasNext()) {
            Token opToken = tokensI.next();
            if(opToken.getValue().equals("\n"))
                continue;

            String opterm = opToken.getValue().toUpperCase();
            if(opterm.charAt(0)==':') {
                String label = opterm.substring(1);
                try {
                    resolvables.addLabel(label);
                } catch (LabelAlreadyExistsError e) {
                    throw new TokenCompileError("Duplicate label found", opToken);
                }
                continue;
            }

            Opcode opcode;
            try {
                opcode = Opcode.valueOf(opterm);
            } catch (IllegalArgumentException e) {
                // Turns out that token wasn't a real opcode
                throw new TokenCompileError("Unknown opcode", opToken);
            }

            if(opcode == Opcode.DAT) {
                boolean isFirst = true;
                while(tokensI.hasNext()) {
                    if(!isFirst) {
                        Token comma = tokensI.next();
                        if(comma.getValue().equals("\n"))
                            break;
                        else if(!comma.getValue().equals(","))
                            throw new TokenCompileError("Expected comma", comma);
                    } else {
                        isFirst = false;
                    }
                    Token dataToken = tokensI.next();
                    char firstChar = dataToken.getValue().charAt(0);
                    if(firstChar=='"') {
                        String full = dataToken.getValue();
                        String quoted = full.substring(1, full.length()-1);
                        byte[] bytes = quoted.getBytes(Charset.forName("UTF-16LE"));
                        assert(bytes.length%2 == 0);
                        for(int k=0; k<bytes.length; k+=2) {
                            resolvables.add(new UnresolvedData(bytes[k] | (bytes[k+1]<<8)));
                        }
                    } else if(is_digit(firstChar)) {
                        int number = parseIntToken(dataToken);
                        resolvables.add(new UnresolvedData(number));
                    } else {
                        resolvables.add(new UnresolvedData(dataToken.getValue()));
                    }
                }
            } else {
                Instruction instr = new Instruction(opcode);
                instr.setValueA(parseValueTokens(tokensI));
                // If this opcode has 2 arguments
                if(opcode.isBasic()) {
                    Token comma = tokensI.next();
                    if(!comma.getValue().equals(","))
                        throw new TokenCompileError("Expected comma", comma);
                    instr.setValueB(parseValueTokens(tokensI));
                }
                System.out.println(instr);
                resolvables.add(instr);
            }
        }
        FileOutputStream out = new FileOutputStream(new File(outname), false);
        resolvables.writeTo(out);
        out.close();
    }

    // Returns the value that must be passed to Instruction.setValueA or B
    private static Value parseValueTokens(Iterator<Token> tokensI)
        throws TokenCompileError {
        Token first = tokensI.next();
        if(!first.getValue().equals("[")) {
            if(is_digit(first.getValue().charAt(0))) {
                return new Value(ValueType.LITERAL, new UnresolvedData(parseIntToken(first)));
            } else {
                String firstS = first.getValue().toUpperCase();
                try {
                    ValueType type = ValueType.valueOf(firstS);
                    return new Value(type);
                } catch (IllegalArgumentException e) {
                    return new Value(ValueType.LITERAL, new UnresolvedData(firstS));
                }
            }
        } else {
            Token second = tokensI.next();
            if(is_digit(second.getValue().charAt(0))) {
                int number = parseIntToken(second);
                Token third = tokensI.next();
                if(third.getValue().equals("]")) {
                    return new Value(ValueType.DN, new UnresolvedData(number));
                } else if(third.getValue().equals("+")) {
                    Token fourth = tokensI.next();
                    String fourthS = fourth.getValue().toUpperCase();
                    ValueType type = ValueType.valueOf(fourthS).dereferenceNextPlus();
                    Token close = tokensI.next();
                    if(!close.getValue().equals("]"))
                        throw new TokenCompileError("Expected closing bracket", close);
                    return new Value(type, new UnresolvedData(number));
                } else {
                    throw new TokenCompileError("Expected '+' or ']'", third);
                }
            } else {
                ValueType type;
                UnresolvedData data = null;
                String secondS = second.getValue().toUpperCase();
                try {
                    type = ValueType.valueOf(secondS).dereference();
                } catch (IllegalArgumentException e) {
                    type = ValueType.DN;
                    data = new UnresolvedData(secondS);
                }
                Token third = tokensI.next();
                if(third.getValue().equals("]")) {
                    return new Value(type, data);
                } else if(third.getValue().equals("+")) {
                    if(data == null)
                        throw new TokenCompileError("Register must go second in offset expressions", second);
                    Token fourth = tokensI.next();
                    String fourthS = fourth.getValue().toUpperCase();
                    type = ValueType.valueOf(fourthS).dereferenceNextPlus();
                    Token close = tokensI.next();
                    if(!close.getValue().equals("]"))
                        throw new TokenCompileError("Expected closing bracket", close);
                    return new Value(type, data);
                } else {
                    throw new TokenCompileError("Expected '+' or ']'", third);
                }
            }
        }
        
    }

    private static final String digits = "0123456789";
    private static boolean is_digit(char c) {
        return digits.indexOf(c) != -1;
    }

    private static int parseIntToken(Token token)
        throws TokenCompileError {
        int number;
        try {
            number = parseInt(token.getValue());
        } catch (NumberFormatException e) {
            throw new TokenCompileError("Invalid number", token);
        }
        if((number & 0xffff) != number) {
            throw new TokenCompileError("Number can't fit in two byte word", token);
        }
        return number;
    }

    private static int parseInt(String value)
        throws NumberFormatException {
        value = value.toUpperCase();
        if(value.startsWith("0X") || value.startsWith("0H")) {
            return Integer.parseInt(value.substring(2), 16);
        } else if(value.startsWith("0B")) {
            return Integer.parseInt(value.substring(2), 2);
        } else {
            return Integer.parseInt(value, 10);
        }
    }

    public static void usage() {
        System.out.println("Parameters: FILE");
    }
}

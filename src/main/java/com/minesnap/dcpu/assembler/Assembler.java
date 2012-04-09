package com.minesnap.dcpu.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ListIterator;

public class Assembler {
    private boolean littleEndian = true;
    private boolean optimize = true;
    private Map<String, Integer> newNBOpcodes = null;

    public Assembler() {
    }

    public void setLittleEndian(boolean littleEndian) {
        this.littleEndian = littleEndian;
    }

    public void setOptimizations(boolean optimize) {
        this.optimize = optimize;
    }

    public void setNewNBOpcodes(Map<String, Integer> newNBOpcodes) {
        for(Integer value : newNBOpcodes.values()) {
            if(value < 0x02 || value > 0x3f) {
                throw new IllegalArgumentException("Custom non-basic opcodes must be between 0x02 and 0x3f");
            }
        }
        this.newNBOpcodes = newNBOpcodes;
    }

    public void assemble(String filename, String outname)
        throws FileNotFoundException, CompileError, IOException {
        Scanner sc = new Scanner(new File(filename), "UTF-8");
        List<Token> tokens = ASMTokenizer.tokenize(sc, filename);
        sc.close();
        ResolverList resolvables = new ResolverList();
        ListIterator<Token> tokensI = tokens.listIterator();
        boolean newlineRequired = false;
        while(tokensI.hasNext()) {
            Token opToken = tokensI.next();

            if(opToken.getValue().equals("\n")) {
                newlineRequired = false;
                continue;
            }

            // Handle labels
            String opterm = opToken.getValue().toUpperCase();
            if(opterm.charAt(0)==':' || opterm.charAt(opterm.length()-1)==':') {
                String label;
                if(opterm.charAt(0)==':')
                    label = opterm.substring(1);
                else
                    label = opterm.substring(0, opterm.length()-1);

                try {
                    resolvables.addLabel(label);
                } catch (LabelAlreadyExistsError e) {
                    throw new TokenCompileError("Duplicate label found", opToken);
                }
                continue;
            }

            // A newline is required after an instruction before
            // another instruction can be read.
            if(newlineRequired) {
                throw new TokenCompileError("Expected newline", opToken);
            }
            newlineRequired = true;

            Opcode opcode = Opcode.getByName(opterm, newNBOpcodes);
            if(opcode == null) {
                // Turns out that token wasn't a real opcode
                throw new TokenCompileError("Unknown opcode", opToken);
            }

            int datRepeat = 1;

            switch(opcode.getType()) {
            case DS:
            {
                if(opcode.getName().charAt(0) != '.') {
                    throw new TokenCompileError(".DS directive requires a period", opToken);
                }
                Token countToken = tokensI.next();
                int count = parseIntToken(countToken);

                List<UnresolvedData> dataList = new ArrayList<UnresolvedData>(1);
                dataList.add(new UnresolvedData(0));
                resolvables.add(new UnresolvedMultiData(dataList, count));

                break;
            }
            case TIMES:
            {
                Token countToken = tokensI.next();
                datRepeat = parseIntToken(countToken);

                opToken = tokensI.next();
                opterm = opToken.getValue().toUpperCase();
                opcode = Opcode.getByName(opterm, newNBOpcodes);
                if(opcode == null) {
                    throw new TokenCompileError("Unknown opcode", opToken);
                }
                if(opcode.getType() != OpcodeType.DAT) {
                    throw new TokenCompileError("Expected DAT opcode after TIMES/DUP opcode", opToken);
                }

                // Fall through to DAT
            }
            case DAT:
            {
                boolean isFirst = true;
                List<UnresolvedData> dataList = new ArrayList<UnresolvedData>();

                while(tokensI.hasNext()) {
                    if(!isFirst) {
                        Token comma = tokensI.next();
                        if(comma.getValue().equals("\n")) {
                            // Put the newline back so that way it's
                            // detected later properly.
                            tokensI.previous();
                            break;
                        } else if(!comma.getValue().equals(",")) {
                            throw new TokenCompileError("Expected comma", comma);
                        }
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
                            dataList.add(new UnresolvedData(bytes[k] | (bytes[k+1]<<8)));
                        }
                    } else if(is_digit(firstChar)) {
                        int number = parseIntToken(dataToken);
                        dataList.add(new UnresolvedData(number));
                    } else {
                        dataList.add(parseLabelToken(dataToken));
                    }
                }

                resolvables.add(new UnresolvedMultiData(dataList, datRepeat));

                break;
            }
            case BRK:
            {
                // Replace BRK with SUB PC, 1
                Instruction instr = new Instruction(Opcode.get(OpcodeType.SUB));
                instr.setValueA(new Value(ValueType.PC));
                instr.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(1)));
                resolvables.add(instr);
                break;
            }
            case JMP:
            {
                Token next = tokensI.next();
                String nextS = next.getValue();
                UnresolvedData data;
                if(is_digit(nextS.charAt(0))) {
                    data = new UnresolvedData(parseIntToken(next));
                } else {
                    data = parseLabelToken(next);
                }
                JMPInstruction jmp = new JMPInstruction(data);
                resolvables.add(jmp);
                break;
            }
            default:
            {
                Instruction instr = new Instruction(opcode);
                instr.setValueA(parseValueTokens(tokensI));
                // If this opcode has 2 arguments
                if(opcode.isBasic()) {
                    Token comma = tokensI.next();
                    if(!comma.getValue().equals(","))
                        throw new TokenCompileError("Expected comma", comma);
                    instr.setValueB(parseValueTokens(tokensI));
                }
                if(optimize) {
                    if(instr.getOpcode().getType() == OpcodeType.SET
                       && instr.getValueA().getType() == ValueType.PC
                       && instr.getValueB().getType() == ValueType.LITERAL) {
                        UnresolvedData data = instr.getValueB().getData();
                        JMPInstruction jmp = new JMPInstruction(data);
                        resolvables.add(jmp);
                        instr = null;
                    }
                }
                // This will be null if we replaced this with an
                // optimized instruction and that means we've already
                // added the optimized version.
                if(instr != null) {
                    resolvables.add(instr);
                }
            }
            }
        }
        resolvables.prepare();
        WordWriter out = new WordWriter(new FileOutputStream(outname, false), littleEndian);
        try {
            resolvables.writeTo(out);
        } finally {
            out.close();
        }
    }

    // Returns the value that must be passed to Instruction.setValueA or B
    private static Value parseValueTokens(ListIterator<Token> tokensI)
        throws TokenCompileError {
        Token first = tokensI.next();
        if(!first.getValue().equals("[")) {
            if(is_digit(first.getValue().charAt(0))) {
                // We're processing something like "3"
                return new Value(ValueType.LITERAL, new UnresolvedData(parseIntToken(first)));
            } else {
                String firstS = first.getValue().toUpperCase();
                try {
                    // We're processing something like "B"
                    ValueType type = ValueType.valueOf(firstS);
                    return new Value(type);
                } catch (IllegalArgumentException e) {
                    // Wait, we're processing something like "somelabel"
                    return new Value(ValueType.LITERAL, parseLabelToken(first));
                }
            }
        } else {
            Token second = tokensI.next();
            if(is_digit(second.getValue().charAt(0))) {
                int number = parseIntToken(second);
                Token third = tokensI.next();
                if(third.getValue().equals("]")) {
                    // We're processing something like "[3]"
                    return new Value(ValueType.DN, new UnresolvedData(number));
                } else if(third.getValue().equals("+")) {
                    // We're processing something like "[3+B]"
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
                    data = parseLabelToken(second);
                }
                Token third = tokensI.next();
                if(third.getValue().equals("]")) {
                    return new Value(type, data);
                } else if(third.getValue().equals("+")) {
                    Token fourth = tokensI.next();
                    String fourthS = fourth.getValue().toUpperCase();
                    if(data == null) {
                        // We're processing something like "[B+3]" or "[B+somelabel]"
                        type = ValueType.valueOf(secondS).dereferenceNextPlus();
                        if(is_digit(fourthS.charAt(0))) {
                            data = new UnresolvedData(parseIntToken(fourth));
                        } else {
                            data = parseLabelToken(fourth);
                        }
                    } else {
                        // We're processing something like "[somelabel+B]"
                        type = ValueType.valueOf(fourthS).dereferenceNextPlus();
                    }
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

    private static UnresolvedData parseLabelToken(Token token)
        throws TokenCompileError {
        String tokenS = token.getValue();
        if(tokenS.equals("\n")) {
            throw new TokenCompileError("Was not expecting newline", token);
        }
        return new UnresolvedData(tokenS);
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
}

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
        File sourceDir;
        Scanner sc;
        if(filename.equals("-")) {
            sourceDir = new File(".");
            sc = new Scanner(System.in, "UTF-8");
        } else {
            File sourcefile = new File(filename);
            sourceDir = sourcefile.getParentFile();
            sc = new Scanner(sourcefile, "UTF-8");
        }
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
            case RESERVE:
            {
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
            case INCBIN:
            {
                Token incfilenameToken = tokensI.next();
                String incfilenameS = incfilenameToken.getValue();
                if(incfilenameS.charAt(0) != '"' || incfilenameS.charAt(incfilenameS.length()-1) != '"') {
                    throw new TokenCompileError("Expected string for filename", incfilenameToken);
                }
                String incfilename = incfilenameS.substring(1, incfilenameS.length()-1);
                File incfile = new File(sourceDir, incfilename);

                Token typeToken = tokensI.next();
                String typeS = typeToken.getValue().toUpperCase();
                boolean incLittleEndian;
                if(typeS.equals("\n")) {
                    // Put the newline back so that way it's detected
                    // later properly.
                    tokensI.previous();
                    // Default to whatever this file is using
                    typeS = "THIS";
                }
                if(typeS.equals("THIS")) {
                    incLittleEndian = littleEndian;
                } else if(typeS.equals("BE")) {
                    incLittleEndian = false;
                } else if(typeS.equals("LE")) {
                    incLittleEndian = true;
                } else {
                    throw new TokenCompileError("Unknown endian type", typeToken);
                }

                try {
                    resolvables.add(new BinInclude(incfile, incLittleEndian));
                } catch(IllegalIncludeException e) {
                    throw new TokenCompileError(e.getMessage(), incfilenameToken);
                }
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
        WordWriter out;
        if(outname.equals("-")) {
            out = new WordWriter(System.out, littleEndian);
        } else {
            out = new WordWriter(new FileOutputStream(outname, false), littleEndian);
        }
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
            // We're about to process some expression that is the name
            // of a register, or is an arbitrary amount of numbers
            // added together optionally added to a label.
            int offset = 0;
            String labelRef = null;

            Token expToken = first;
            while(true) {
                String expTokenS = expToken.getValue().toUpperCase();
                if(expTokenS.equals("\n") || expTokenS.equals(",")) {
                    throw new TokenCompileError("Was not expecting symbol", expToken);
                }
                if(is_digit(expTokenS.charAt(0))) {
                    offset += parseIntToken(expToken);
                } else {
                    try {
                        ValueType register = ValueType.valueOf(expTokenS);
                        if(offset != 0 || labelRef != null)
                            throw new TokenCompileError("Can not add register in literal", expToken);
                        return new Value(register);
                    } catch (IllegalArgumentException e) {
                        // This token is a label and not a register.
                        if(labelRef != null)
                            throw new TokenCompileError("Can not have multiple labels in expression", expToken);
                        labelRef = expTokenS;
                    }
                }
                // Now we have either a '+', ',' or '\n' coming up.
                Token sepToken = tokensI.next();
                String sepTokenS = sepToken.getValue();
                if(sepTokenS.equals(",") || sepTokenS.equals("\n")) {
                    // Put that token back for the caller to process.
                    tokensI.previous();
                    break;
                } else if(sepTokenS.equals("+")) {
                    expToken = tokensI.next();
                    continue;
                } else {
                    throw new TokenCompileError("Expected a '+', ',' or '\\n'", sepToken);
                }
            }
            if(labelRef == null) {
                return new Value(ValueType.LITERAL, new UnresolvedData(offset));
            } else {
                return new Value(ValueType.LITERAL, new UnresolvedOffset(labelRef, offset));
            }
        } else {
            // We're about to process some dereference expression like
            // [--SP], [SP++], [B], [B+3], [somelabel+B],
            // [somelabel+1], [somelabel+B+1], etc.
            int offset = 0;
            ValueType register = null;
            String labelRef = null;
            while(true) {
                Token expToken = tokensI.next();
                String expTokenS = expToken.getValue().toUpperCase();
                if(expTokenS.equals("\n")) {
                    throw new TokenCompileError("Was not expecting newline", expToken);
                }
                if(is_digit(expTokenS.charAt(0))) {
                    if(expTokenS.equals("--SP")) {
                        if(register != null)
                            throw new TokenCompileError("Invalid dereference expression", expToken);
                        register = ValueType.PUSH;
                    } else {
                        offset += parseIntToken(expToken);
                    }
                } else {
                    // This token is either a label or register
                    // name. Note that we can only have up to one
                    // label per deref expression, and we can only
                    // have up to one register per deref expression.
                    if(expTokenS.equals("+")) {
                        if(register == ValueType.SP) {
                            register = ValueType.POP;
                        } else {
                            throw new TokenCompileError("Invalid dereference expression", expToken);
                        }
                    } else {
                        try {
                            ValueType temp = ValueType.valueOf(expTokenS);
                            if(register != null)
                                throw new TokenCompileError("Can not have multiple registers in dereference expression", expToken);
                            register = temp;
                        } catch (IllegalArgumentException e) {
                            // This token is a label and not a register.
                            if(labelRef != null)
                                throw new TokenCompileError("Can not have multiple labels in dereference expression", expToken);
                            labelRef = expTokenS;
                        }
                    }
                }
                // Now we have either a + or ] coming up.
                Token sepToken = tokensI.next();
                String sepTokenS = sepToken.getValue();
                if(sepTokenS.equals("]")) {
                    break;
                } else if(sepTokenS.equals("+")) {
                    continue;
                } else {
                    throw new TokenCompileError("Expected a ']' or '+'", sepToken);
                }
            }
            if(register == null) {
                if(labelRef == null) {
                    return new Value(ValueType.DN, new UnresolvedData(offset));
                } else {
                    return new Value(ValueType.DN, new UnresolvedOffset(labelRef, offset));
                }
            } else {
                if(register == ValueType.POP || register == ValueType.PUSH) {
                    if(labelRef == null && offset == 0) {
                        return new Value(register);
                    } else {
                        throw new TokenCompileError("Invalid dereference expression", first);
                    }
                }
                if(labelRef == null && offset == 0) {
                    register = register.dereference();
                    return new Value(register);
                } else {
                    register = register.dereferenceNextPlus();
                    if(labelRef == null) {
                        assert(offset != 0);
                        return new Value(register, new UnresolvedData(offset));
                    } else {
                        return new Value(register, new UnresolvedOffset(labelRef, offset));
                    }
                }
            }
        }
        
    }

    private static final String digits = "-0123456789";
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
        int hibyte = number & 0xffff0000;
        if(hibyte != 0) {
            // We need to make sure that number isn't just negative
            // and within bounds.
            if(hibyte != 0xffff0000 || (number & 0x8000) != 0x8000) {
                throw new TokenCompileError("Number can't fit in two byte word", token);
            }
        }
        number &= 0xffff;
        return number;
    }

    private static int parseInt(String value)
        throws NumberFormatException {

        int ivalue;
        boolean isNegative = false;
        value = value.toUpperCase();

        if(value.startsWith("-")) {
            isNegative = true;
            value = value.substring(1);
            if(value.length() == 0) {
                throw new NumberFormatException();
            }
        }
        if(value.startsWith("0X") || value.startsWith("0H")) {
            ivalue = Integer.parseInt(value.substring(2), 16);
        } else if(value.startsWith("0B")) {
            ivalue = Integer.parseInt(value.substring(2), 2);
        } else {
            ivalue = Integer.parseInt(value, 10);
        }
        if(isNegative) {
            ivalue = -ivalue;
        }
        return ivalue;
    }
}

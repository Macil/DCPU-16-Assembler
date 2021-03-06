package com.minesnap.dcpu.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.ListIterator;

public class Assembler {
    private boolean littleEndian = true;
    private boolean optimize = true;
    private boolean positionIndependent = false;
    private Map<String, Integer> newNBOpcodes = null;
    private ResolverList resolvables = null;
    private boolean assembled = false;

    public Assembler() {
    }

    public void setLittleEndian(boolean littleEndian) {
        this.littleEndian = littleEndian;
    }

    public void setOptimizations(boolean optimize) {
        this.optimize = optimize;
    }

    public void setPositionIndependent(boolean positionIndependent) {
        this.positionIndependent = positionIndependent;
    }

    public void setNewNBOpcodes(Map<String, Integer> newNBOpcodes) {
        for(Integer value : newNBOpcodes.values()) {
            if(value < 0x02 || value > 0x3f) {
                throw new IllegalArgumentException("Custom non-basic opcodes must be between 0x02 and 0x3f");
            }
        }
        this.newNBOpcodes = newNBOpcodes;
    }

    public void assemble(String filename)
        throws FileNotFoundException, CompileError, IOException {
        File sourceDir;
        Reader in;
        if(filename.equals("-")) {
            sourceDir = new File(".");
            in = new InputStreamReader(System.in, "UTF-8");
        } else {
            File sourcefile = new File(filename);
            sourceDir = sourcefile.getParentFile();
            in = new InputStreamReader(new FileInputStream(sourcefile), "UTF-8");
        }
        List<Token> tokens;
	try {
            tokens = ASMTokenizer.tokenize(in, sourceDir, filename);
        } finally {
            in.close();
        }

        assembled = false;
        resolvables = new ResolverList();
        ListIterator<Token> tokensI = tokens.listIterator();
        boolean newlineRequired = false;
        while(tokensI.hasNext()) {
            Token opToken = tokensI.next();

            if(opToken.getText().equals("\n")) {
                newlineRequired = false;
                continue;
            }

            // Handle labels
            String opterm = opToken.getText();
            if(opToken instanceof LabelToken) {
                try {
                    resolvables.addLabel(((LabelToken)opToken).getName());
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
                int count;
                if(countToken instanceof IntToken) {
                    count = ((IntToken)countToken).getValue();
                } else {
                    throw new TokenCompileError("Expected integer", countToken);
                }

                List<UnresolvedData> dataList = new ArrayList<UnresolvedData>(1);
                dataList.add(new UnresolvedData(null, 0));
                resolvables.add(new UnresolvedMultiData(dataList, count));

                break;
            }
            case TIMES:
            {
                Token countToken = tokensI.next();
                if(countToken instanceof IntToken) {
                    datRepeat = ((IntToken)countToken).getValue();
                } else {
                    throw new TokenCompileError("Expected integer", countToken);
                }

                opToken = tokensI.next();
                opterm = opToken.getText();
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
                        if(comma.getText().equals("\n")) {
                            // Put the newline back so that way it's
                            // detected later properly.
                            tokensI.previous();
                            break;
                        } else if(!comma.getText().equals(",")) {
                            throw new TokenCompileError("Expected comma", comma);
                        }
                    } else {
                        isFirst = false;
                    }
                    Token dataToken = tokensI.next();
                    if(dataToken instanceof StringToken) {
                        StringToken sdataToken = (StringToken)dataToken;
                        byte[] bytes = sdataToken.getValue().getBytes(Charset.forName("UTF-16LE"));
                        assert(bytes.length%2 == 0);
                        for(int k=0; k<bytes.length; k+=2) {
                            dataList.add(new UnresolvedData(dataToken, bytes[k] | (bytes[k+1]<<8)));
                        }
                    } else {
                        tokensI.previous();
                        dataList.add(parseLiteralExpression(tokensI));
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
                instr.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(null, 1)));
                resolvables.add(instr);
                break;
            }
            case JMP:
            case BRA:
            {
                UnresolvedData data = parseLiteralExpression(tokensI);
                if(positionIndependent || opcode.getType() == OpcodeType.BRA) {
                    BRAInstruction bra = new BRAInstruction(data);
                    resolvables.add(bra);
                } else {
                    JMPInstruction jmp = new JMPInstruction(data);
                    resolvables.add(jmp);
                }
                break;
            }
            case INCBIN:
            {
                Token incfilenameToken = tokensI.next();
                if(!(incfilenameToken instanceof StringToken)) {
                    throw new TokenCompileError("Expected string for filename", incfilenameToken);
                }
                String incfilename = ((StringToken)incfilenameToken).getValue();
                File incfile = new File(incfilenameToken.getSourceDir(), incfilename);

                Token typeToken = tokensI.next();
                String typeS = typeToken.getText();
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
            case INCLUDE:
            {
                Token incfilenameToken = tokensI.next();
                if(!(incfilenameToken instanceof StringToken)) {
                    throw new TokenCompileError("Expected string for filename", incfilenameToken);
                }
                String incfilename = ((StringToken)incfilenameToken).getValue();
                File incfile = new File(incfilenameToken.getSourceDir(), incfilename);

                Reader incIn = new InputStreamReader(new FileInputStream(incfile), "UTF-8");
                List<Token> incTokens;
                try {
                    incTokens = ASMTokenizer.tokenize(incIn, incfile.getParentFile(), incfile.getName());
                } finally {
                    incIn.close();
                }
                // Add the included tokens after what should be the
                // newline following this directive.
                tokens.addAll(tokensI.nextIndex()+1, incTokens);
                // Reset the tokensI iterator or else it will complain
                // about a concurrent modification.
                tokensI = tokens.listIterator(tokensI.nextIndex());
                break;
            }
            default:
            {
                Instruction instr = new Instruction(opcode);
                instr.setValueA(parseValueTokens(tokensI));
                // If this opcode has 2 arguments
                if(opcode.isBasic()) {
                    Token comma = tokensI.next();
                    if(!comma.getText().equals(","))
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
        assembled = true;
    }

    public void writeTo(String outname)
        throws IOException {
        if(!assembled) {
            throw new IllegalStateException("assemble method must be called before writeTo");
        }

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

    private static UnresolvedData parseLiteralExpression(ListIterator<Token> tokensI)
        throws TokenCompileError {
        Token firstToken = tokensI.next();
        tokensI.previous();

        int offset = 0;
        String labelRef = null;
        boolean nextIsNegative = false;
        while(true) {
            Token expToken = tokensI.next();
            String expTokenS = expToken.getText();
            if(expToken instanceof SymbolToken) {
                if(expTokenS.equals("-")) {
                    if(nextIsNegative) {
                        throw new TokenCompileError("Multiple '-' symbols may not be chained here", expToken);
                    } else {
                        nextIsNegative = true;
                    }
                    continue;
                } else {
                    throw new TokenCompileError("Was not expecting symbol", expToken);
                }
            } else if(expToken instanceof IntToken) {
                int value = ((IntToken)expToken).getValue();
                if(nextIsNegative) {
                    value = -value;
                    nextIsNegative = false;
                }
                offset += value;
            } else {
                if(nextIsNegative) {
                    throw new TokenCompileError("You can not subtract the value of a label", expToken);
                }
                try {
                    ValueType register = ValueType.valueOf(expTokenS);
                    throw new TokenCompileError("Can not add register in literal", expToken);
                } catch (IllegalArgumentException e) {
                    // This token is a label and not a register.
                    if(!ASMTokenizer.is_legal_label(expTokenS))
                        throw new TokenCompileError("Is not a legal label name", expToken);
                    if(labelRef != null)
                        throw new TokenCompileError("Can not have multiple labels in expression", expToken);
                    labelRef = expTokenS;
                }
            }
            // Now we have either a '+', ',' or '\n' coming up.
            Token sepToken = tokensI.next();
            String sepTokenS = sepToken.getText();
            if(sepTokenS.equals(",") || sepTokenS.equals("\n")) {
                // Put that token back for the caller to process.
                tokensI.previous();
                break;
            } else if(sepTokenS.equals("+")) {
                nextIsNegative = false;
                continue;
            } else if(sepTokenS.equals("-")) {
                nextIsNegative = true;
                continue;
            } else {
                throw new TokenCompileError("Expected a '+', '-', ',' or '\\n'", sepToken);
            }
        }
        if(labelRef == null) {
            checkIntWordRange(firstToken, offset);
            return new UnresolvedData(firstToken, offset & 0xffff);
        } else {
            return new UnresolvedOffset(firstToken, labelRef, offset);
        }
    }

    // Returns the value that must be passed to Instruction.setValueA or B
    private static Value parseValueTokens(ListIterator<Token> tokensI)
        throws TokenCompileError {
        Token first = tokensI.next();
        if(!first.getText().equals("[")) {
            // We're about to process some expression that is the name
            // of a register, or is an arbitrary amount of numbers
            // added together optionally added to a label.
            String firstS = first.getText();
            try {
                ValueType register = ValueType.valueOf(firstS);
                return new Value(register);
            } catch (IllegalArgumentException e) {
                // If it wasn't a register, then it's some literal
                // expression.
                tokensI.previous();
                UnresolvedData data = parseLiteralExpression(tokensI);
                return new Value(ValueType.LITERAL, data);
            }
        } else {
            // We're about to process some dereference expression like
            // [--SP], [SP++], [B], [B+3], [somelabel+B],
            // [somelabel+1], [somelabel+B+1], etc. Note that we don't
            // use parseLiteralExpression because this expression can
            // have a single register in it.
            int offset = 0;
            ValueType register = null;
            String labelRef = null;
            boolean nextIsNegative = false;
            while(true) {
                Token expToken = tokensI.next();
                String expTokenS = expToken.getText();

                if(expTokenS.equals("\n")) {
                    throw new TokenCompileError("Was not expecting newline", expToken);
                } else if(expToken instanceof IntToken) {
                    int value = ((IntToken)expToken).getValue();
                    if(nextIsNegative) {
                        value = -value;
                        nextIsNegative = false;
                    }
                    offset += value;
                } else if(expTokenS.equals("+")) {
                    if(register == ValueType.SP) {
                        register = ValueType.POP;
                    } else {
                        throw new TokenCompileError("Invalid dereference expression", expToken);
                    }
                } else if(expTokenS.equals("-")) {
                    if(nextIsNegative) {
                        // We have two '-' symbols in a row. This is only legal if it precedes "SP".
                        if(register != null)
                            throw new TokenCompileError("Invalid dereference expression", expToken);

                        expToken = tokensI.next();
                        expTokenS = expToken.getText();
                        try {
                            register = ValueType.valueOf(expTokenS);
                        } catch (IllegalArgumentException e) {
                            throw new TokenCompileError("Multiple '-' symbols may not be chained here", expToken);
                        }
                        if(register != ValueType.SP)
                            throw new TokenCompileError("Can not decrement non-SP register", expToken);
                        register = ValueType.PUSH;
                    } else {
                        nextIsNegative = true;
                        continue;
                    }
                } else {
                    // This token is either a label or register
                    // name. Note that we can only have up to one
                    // label per deref expression, and we can only
                    // have up to one register per deref expression.
                    if(nextIsNegative) {
                        throw new TokenCompileError("You can not subtract the value of a label or register", expToken);
                    }
                    try {
                        ValueType temp = ValueType.valueOf(expTokenS);
                        if(register != null)
                            throw new TokenCompileError("Can not have multiple registers in dereference expression", expToken);
                        register = temp;
                    } catch (IllegalArgumentException e) {
                        // This token is a label and not a register.
                        if(!ASMTokenizer.is_legal_label(expTokenS))
                            throw new TokenCompileError("Is not a legal label name", expToken);
                        if(labelRef != null)
                            throw new TokenCompileError("Can not have multiple labels in dereference expression", expToken);
                        labelRef = expTokenS;
                    }
                }

                // Now we have either a + or ] coming up.
                Token sepToken = tokensI.next();
                String sepTokenS = sepToken.getText();
                if(sepTokenS.equals("]")) {
                    break;
                } else if(sepTokenS.equals("+")) {
                    nextIsNegative = false;
                    continue;
                } else if(sepTokenS.equals("-")) {
                    nextIsNegative = true;
                    continue;
                } else {
                    throw new TokenCompileError("Expected a ']', '+', or '-'", sepToken);
                }
            }
            if(register == null) {
                if(labelRef == null) {
                    checkIntWordRange(first, offset);
                    return new Value(ValueType.DN, new UnresolvedData(first, offset & 0xffff));
                } else {
                    return new Value(ValueType.DN, new UnresolvedOffset(first, labelRef, offset));
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
                        checkIntWordRange(first, offset);
                        return new Value(register, new UnresolvedData(first, offset & 0xffff));
                    } else {
                        return new Value(register, new UnresolvedOffset(first, labelRef, offset));
                    }
                }
            }
        }
    }

    private static void checkIntWordRange(Token token, int value)
        throws TokenCompileError {
        if((value >= 0 && (value & 0xffff0000) != 0)
           || (value < 0 && (value & 0xffff8000) != 0xffff8000)) {
            throw new TokenCompileError("Value can not fit in 16 bits", token);
        }
    }
}

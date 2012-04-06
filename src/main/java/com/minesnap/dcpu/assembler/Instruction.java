package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class Instruction implements Resolvable {
    private final Opcode opcode;
    private Value valueA = null;
    private Value valueB = null;

    public Instruction(Opcode opcode) {
        if(opcode==null)
            throw new IllegalArgumentException("Opcode must not be null");
        if(opcode.getCode()==null)
            throw new IllegalArgumentException("Can't make Instruction object for fake Opcode");

        this.opcode = opcode;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public void setValueA(Value value) {
        this.valueA = value;
    }

    public void setValueB(Value value) {
        if(!opcode.isBasic())
            throw new IllegalArgumentException("Can not setValueB on non-basic opcode");
        this.valueB = value;
    }

    public Value getValueA() {
        return valueA;
    }

    public Value getValueB() {
        return valueB;
    }

    @Override
    public int wordCount() {
        int count = 1;
        if(valueA.hasNextWord())
            count++;
        if(opcode.isBasic() && valueB.hasNextWord())
            count++;
        return count;
    }

    @Override
    public void writeTo(OutputStream out, Map<String, Integer> refmap)
        throws SymbolLookupError, IOException {
        int opword = opcode.getCode();
        if(opcode.isBasic()) {
            opword |= (valueA.evaluate() << 4) | (valueB.evaluate() << 10);
        } else {
            opword |= (valueA.evaluate() << 10);
        }
        out.write(opword & 0x00ff);
        out.write((opword & 0xff00) >> 8);
        if(valueA.hasNextWord()) {
            int aData = valueA.getData().evaluate(refmap);
            out.write(aData & 0x00ff);
            out.write((aData & 0xff00) >> 8);
        }
        if(opcode.isBasic() && valueB.hasNextWord()) {
            int bData = valueB.getData().evaluate(refmap);
            out.write(bData & 0x00ff);
            out.write((bData & 0xff00) >> 8);
        }
    }

    @Override
    public String toString() {
        return opcode.toString();
    }
}

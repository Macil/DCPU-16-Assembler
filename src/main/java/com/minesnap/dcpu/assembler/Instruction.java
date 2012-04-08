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
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws SymbolLookupError {
        if(valueA.getData() != null)
            valueA.getData().evaluateLabels(labelValues, position);
        if(opcode.isBasic() && valueB.getData() != null)
            valueB.getData().evaluateLabels(labelValues, position);
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
    public void writeTo(OutputStream out)
        throws IOException {
        int opword = opcode.getCode();
        if(opcode.isBasic()) {
            opword |= (valueA.evaluate() << 4) | (valueB.evaluate() << 10);
        } else {
            opword |= (valueA.evaluate() << 10);
        }

        out.write(opword & 0x00ff);
        out.write((opword & 0xff00) >> 8);

        if(valueA.hasNextWord())
            valueA.getData().writeTo(out);
        if(opcode.isBasic() && valueB.hasNextWord())
            valueB.getData().writeTo(out);
    }

    @Override
    public String toString() {
        if(opcode.isBasic())
            return opcode.toString()+" "+valueA.toString()+", "+valueB.toString();
        else
            return opcode.toString()+" "+valueA.toString();
    }
}

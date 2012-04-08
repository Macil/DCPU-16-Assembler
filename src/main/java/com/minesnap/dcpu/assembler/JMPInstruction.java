package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class JMPInstruction implements Resolvable {
    private final UnresolvedData data;
    private Instruction realInstruction = null;

    public JMPInstruction(UnresolvedData data) {
        this.data = data;
    }

    public UnresolvedData getData() {
        return data;
    }

    @Override
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws SymbolLookupError {
        data.evaluateLabels(labelValues, position);
        int dest = data.getUnresolvedWord();
        int delta = dest - (position+wordCount());

        System.out.println("delta = "+delta);
        if(delta == 0) {
            // JMP to the next instruction is NOP so don't do
            // anything.
            realInstruction = null;
        } else if(delta > 0 && delta <= 0x1f) {
            realInstruction = new Instruction(Opcode.ADD);
            realInstruction.setValueA(new Value(ValueType.PC));
            realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(delta)));
        } else if(delta < 0 && delta >= -0x1f) {
            realInstruction = new Instruction(Opcode.SUB);
            realInstruction.setValueA(new Value(ValueType.PC));
            realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(-delta)));
        } else {
            realInstruction = new Instruction(Opcode.SET);
            realInstruction.setValueA(new Value(ValueType.PC));
            realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(dest)));
        }
    }

    @Override
    public int wordCount() {
        if(realInstruction == null)
            return 0;
        return realInstruction.wordCount();
    }

    @Override
    public void writeTo(OutputStream out)
        throws IOException {
        if(realInstruction != null)
            realInstruction.writeTo(out);
    }

    @Override
    public String toString() {
        return "JMP "+data;
    }
}

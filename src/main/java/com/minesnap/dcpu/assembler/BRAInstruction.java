package com.minesnap.dcpu.assembler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

// Relative BRAnch instruction. Like JMP, but never compiles to SET.

public class BRAInstruction implements Resolvable {
    private final UnresolvedData data;
    private Instruction realInstruction = null;

    public BRAInstruction(UnresolvedData data) {
        this.data = data;
    }

    public UnresolvedData getData() {
        return data;
    }

    @Override
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws TokenCompileError {
        data.evaluateLabels(labelValues, position);
        int dest = data.getUnresolvedWord();

        int delta = dest - position;
        // Q: If the instruction's size might be 2 words, then why are
        // we only correcting by 1 word? A: The first operand, PC, is
        // read before the second word causes PC to increment.
        if(realInstruction != null)
            delta--;

        if(delta > 0) {
            realInstruction = new Instruction(Opcode.get(OpcodeType.ADD));
            realInstruction.setValueA(new Value(ValueType.PC));
            realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(null, delta)));
        } else if(delta < 0) {
            realInstruction = new Instruction(Opcode.get(OpcodeType.SUB));
            realInstruction.setValueA(new Value(ValueType.PC));
            realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(null, -delta)));
        } else /* if(delta == 0) */ {
            // JMP to the next instruction is NOP so don't do
            // anything.
            assert(delta == 0);
            realInstruction = null;
        }
    }

    @Override
    public int wordCount() {
        if(realInstruction == null)
            return 0;
        return realInstruction.wordCount();
    }

    @Override
    public void writeTo(WordWriter out)
        throws IOException {
        if(realInstruction != null)
            realInstruction.writeTo(out);
    }

    @Override
    public String toString() {
        return "BRA "+data;
    }
}

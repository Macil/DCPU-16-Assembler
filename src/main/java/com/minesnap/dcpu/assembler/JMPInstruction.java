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

    private final static int maxliteral = 0x1f;

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

        if(delta == 0) {
            // JMP to the next instruction is NOP so don't do
            // anything.
            realInstruction = null;
        } else {
            // Prefer using SET with a short form literal if possible
            // as it takes the fewest cycles. If we can't use the
            // short forms of ADD or SUB, then use SET.
            if(dest <= maxliteral || delta > maxliteral || delta < -maxliteral) {
                realInstruction = new Instruction(Opcode.get(OpcodeType.SET));
                realInstruction.setValueA(new Value(ValueType.PC));
                realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(data.getToken(), dest)));
            } else if(delta > 0) {
                assert(delta <= maxliteral);
                realInstruction = new Instruction(Opcode.get(OpcodeType.ADD));
                realInstruction.setValueA(new Value(ValueType.PC));
                realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(null, delta)));
            } else if(delta < 0) {
                assert(-delta <= maxliteral);
                realInstruction = new Instruction(Opcode.get(OpcodeType.SUB));
                realInstruction.setValueA(new Value(ValueType.PC));
                realInstruction.setValueB(new Value(ValueType.LITERAL, new UnresolvedData(null, -delta)));
            } else {
                throw new IllegalStateException("This shouldn't happen");
            }
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
        return "JMP "+data;
    }
}

package com.minesnap.dcpu.assembler;

import java.util.Map;
import java.util.HashMap;

public enum Opcode {
    SET(0x1),
        ADD(0x2),
        SUB(0x3),
        MUL(0x4),
        DIV(0x5),
        MOD(0x6),
        SHL(0x7),
        SHR(0x8),
        AND(0x9),
        BOR(0xa),
        XOR(0xb),
        IFE(0xc),
        IFN(0xd),
        IFG(0xe),
        IFB(0xf),

        DAT,
        JSR(0x10);

    private final Integer code;
    private final static Map<Integer, Opcode> BY_DATA = new HashMap<Integer, Opcode>();

    private Opcode() {
        this(null);
    }

    private Opcode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public boolean isBasic() {
        return (code & 0xf) != 0;
    }

    public static Opcode getByData(Integer data) {
        return BY_DATA.get(data);
    }

    static {
        for (Opcode opcode : values()) {
            BY_DATA.put(opcode.code, opcode);
        }
    }
}

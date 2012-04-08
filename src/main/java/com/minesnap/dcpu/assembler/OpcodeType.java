package com.minesnap.dcpu.assembler;

public enum OpcodeType {
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
        JMP,
        BRK,

        JSR(0x10),

        CUSTOM(0x0);

    private final Integer code;

    private OpcodeType() {
        this(null);
    }

    private OpcodeType(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}

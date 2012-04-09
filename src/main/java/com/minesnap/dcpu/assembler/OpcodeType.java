package com.minesnap.dcpu.assembler;

import java.util.List;
import java.util.Collections;
import java.util.Arrays;

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
        TIMES(null, "DUP"),
        DS(null, ".DS"),

        JSR(0x10),

        CUSTOM(0x0);

    private final Integer code;
    private final List<String> altNames;

    private OpcodeType() {
        this(null);
    }

    private OpcodeType(Integer code) {
        this(code, null);
    }

    private OpcodeType(Integer code, String... altNames) {
        this.code = code;
        if(altNames == null)
            this.altNames = Collections.EMPTY_LIST;
        else
            this.altNames = Collections.unmodifiableList(Arrays.asList(altNames));
    }

    public Integer getCode() {
        return code;
    }

    public List<String> getAltNames() {
        return altNames;
    }
}

package com.minesnap.dcpu.assembler;

import java.util.Map;
import java.util.HashMap;

public enum ValueType {
    A(0x0),
        B(0x1),
        C(0x2),
        X(0x3),
        Y(0x4),
        Z(0x5),
        I(0x6),
        J(0x7),

        // Dereference
        D_A(0x8),
        D_B(0x9),
        D_C(0xa),
        D_X(0xb),
        D_Y(0xc),
        D_Z(0xd),
        D_I(0xe),
        D_J(0xf),

        // Dereference plus next
        DPN_A(0x10, true),
        DPN_B(0x11, true),
        DPN_C(0x12, true),
        DPN_X(0x13, true),
        DPN_Y(0x14, true),
        DPN_Z(0x15, true),
        DPN_I(0x16, true),
        DPN_J(0x17, true),

        POP(0x18),
        PEEK(0x19),
        PUSH(0x1a),
        SP(0x1b),
        PC(0x1c),
        O(0x1d),
        // Dereference next
        DN(0x1e, true),
        // Just next
        LITERAL(0x1f, true);

    private final byte code;
    private final boolean literal;
    private final static Map<Byte, ValueType> BY_CODE = new HashMap<Byte, ValueType>();

    private ValueType(int code) {
        this(code, false);
    }

    private ValueType(int code, boolean literal) {
        this.code = (byte)code;
        this.literal = literal;
    }

    public byte getCode() {
        return code;
    }

    // Returns true if this value refers to a numerical literal
    // (such as an unresolved address) (or a literal-in-source
    // that may or may not be compiled in the next word).
    public boolean hasLiteral() {
        return literal;
    }

    public ValueType dereference() {
        if(code >= A.code && code <= J.code)
            return getByCode((byte)(code+0x8));
        if(this == ValueType.SP)
            return ValueType.PEEK;
        throw new IllegalStateException("Can not dereference type "+this);
    }

    public ValueType dereferenceNextPlus() {
        if(code >= A.code && code <= J.code)
            return getByCode((byte)(code+0x10));
        throw new IllegalStateException("Can not dereference+plus type "+this);
    }

    public static ValueType getByCode(byte code) {
        return BY_CODE.get(code);
    }

    static {
        for (ValueType value : values()) {
            BY_CODE.put(value.code, value);
        }
    }
}

package com.minesnap.dcpu.assembler;

public class Value {
    private final ValueType type;

    // Holds a number or unresolved symbol
    private final UnresolvedData data;

    public Value(ValueType type) {
        this(type, null);
    }

    public Value(ValueType type, UnresolvedData data) {
        if(data == null && type.hasLiteral())
            throw new IllegalArgumentException("Given ValueType requires data");
        if(data != null && !type.hasLiteral())
            throw new IllegalArgumentException("Given ValueType does not support data");

        this.type = type;
        this.data = data;
    }

    public ValueType getType() {
        return type;
    }

    public UnresolvedData getData() {
        return data;
    }

    public boolean hasNextWord() {
        // If the data is not null and has a value above 0x1f, then
        // the answer is yes. Note that it's assumed that unresolved
        // data will be small enough to fit in the current word.
        if(data == null)
            return false;
        return data.getUnresolvedWord() > 0x1f;
    }

    public byte evaluate() {
        if(!type.hasLiteral()) {
            return type.getCode();
        } else {
            assert(data.checkResolved());
            if(hasNextWord()) {
                return type.getCode();
            } else {
                assert(type == ValueType.LITERAL);
                return (byte)(0x20+data.getUnresolvedWord());
            }
        }
    }

    @Override
    public String toString() {
        return type.toString();
    }
}

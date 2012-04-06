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
        // If the data is a reference or has a value above 0x1f, then
        // the answer is yes. We need to be able to decide whether or
        // not this Value will use the next word before we can resolve
        // address symbols, so unresolved symbols are forced into
        // their own word regardless of whether they end up being
        // small enough to be in the current word.
        if(data == null)
            return false;
        if(data.getReference() != null)
            return true;
        return data.getUnresolvedWord() > 0x1f;
    }

    public byte evaluate() {
        if(!type.hasLiteral()) {
            return type.getCode();
        } else {
            if(hasNextWord()) {
                return type.getCode();
            } else {
                assert(type == ValueType.LITERAL);
                return (byte)(0x20+data.getUnresolvedWord());
            }
        }
    }
}

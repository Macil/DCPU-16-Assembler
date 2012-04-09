package com.minesnap.dcpu.assembler;

import java.util.Map;
import java.util.HashMap;

public class Opcode {
    private final String name;
    private final Integer code;
    private final OpcodeType type;

    private final static Map<String, Opcode> BY_NAME = new HashMap<String, Opcode>();
    private final static Map<OpcodeType, Opcode> BY_TYPE = new HashMap<OpcodeType, Opcode>();

    static {
        for(OpcodeType type : OpcodeType.values()) {
            if(type == OpcodeType.CUSTOM)
                continue;

            Opcode opcode = new Opcode(type);
            BY_NAME.put(type.toString(), opcode);
            BY_TYPE.put(type, opcode);

            for(String name : type.getAltNames()) {
                Opcode alt = new Opcode(name, type.getCode(), type);
                BY_NAME.put(name, alt);
            }
        }
    }

    private Opcode(OpcodeType type) {
        this(type.toString(), type.getCode(), type);
    }

    private Opcode(String name, Integer code, OpcodeType type) {
        this.name = name;
        this.code = code;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Integer getCode() {
        return code;
    }

    public OpcodeType getType() {
        return type;
    }

    public boolean isBasic() {
        return (code & 0xf) != 0;
    }

    public static Opcode get(OpcodeType type) {
        return BY_TYPE.get(type);
    }

    public static Opcode getByName(String name, Map<String, Integer> newNBOpcodes) {
        Opcode opcode = BY_NAME.get(name);
        if(opcode == null) {
            Integer number = newNBOpcodes.get(name);
            if(number != null) {
                opcode = new Opcode(name, number<<4, OpcodeType.CUSTOM);
            }
        }
        return opcode;
    }
}

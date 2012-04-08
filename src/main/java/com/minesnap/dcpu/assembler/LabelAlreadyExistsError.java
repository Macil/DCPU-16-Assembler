package com.minesnap.dcpu.assembler;

public class LabelAlreadyExistsError extends Exception {
    private final String name;

    public LabelAlreadyExistsError(String name) {
        super("Label already exists: "+name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

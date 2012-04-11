package com.minesnap.dcpu.assembler;

import java.io.InputStream;
import java.io.IOException;

public class WordReader {
    private final InputStream in;
    private final boolean littleEndian;

    public WordReader(InputStream in, boolean littleEndian) {
        this.in = in;
        this.littleEndian = littleEndian;
    }

    public int readWord()
        throws IOException {
        int read1 = in.read();
        if(read1 == -1)
            return -1;
        int read2 = in.read();
        if(read2 == -1)
            throw new IOException("EOF in middle of 16 bit word");

        if(littleEndian) {
            return (read2 << 8) | read1;
        } else {
            return (read1 << 8) | read2;
        }
    }

    public void close()
        throws IOException {
        in.close();
    }
}

package com.minesnap.dcpu.assembler;

import java.io.OutputStream;
import java.io.IOException;

public class WordWriter {
    private final OutputStream out;
    private final boolean littleEndian;

    private long wordsWritten;

    public WordWriter(OutputStream out, boolean littleEndian) {
        this.out = out;
        this.littleEndian = littleEndian;
        wordsWritten = 0;
    }

    public void writeWord(int word)
        throws IOException {
        if((word & 0xffff) != word) {
            throw new IllegalArgumentException("Argument is not a 16 bit word: "+word);
        }

        if(littleEndian) {
            out.write(word & 0x00ff);
            out.write((word & 0xff00) >> 8);
        } else {
            out.write((word & 0xff00) >> 8);
            out.write(word & 0x00ff);
        }
        wordsWritten++;
    }

    public long getWordsWritten() {
        return wordsWritten;
    }

    public void close()
        throws IOException {
        out.close();
    }
}

package com.minesnap.dcpu.assembler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class BinInclude implements Resolvable {
    private final File file;
    private final boolean littleEndian;
    private final int wordsize;

    public BinInclude(File file, boolean littleEndian)
        throws IllegalIncludeException {
        if(!file.exists()) {
            throw new IllegalIncludeException("File "+file+" does not exist");
        }

        int bytesize = (int)file.length();
        if(bytesize % 2 != 0) {
            throw new IllegalIncludeException("File size of "+file+" must be an even number of bytes");
        }

        this.file = file;
        this.littleEndian = littleEndian;
        this.wordsize = bytesize / 2;
    }

    @Override
    public void evaluateLabels(Map<String, Integer> labelValues, int position)
        throws SymbolLookupError {
    }

    @Override
    public int wordCount() {
        return wordsize;
    }

    @Override
    public void writeTo(WordWriter out)
        throws IOException {
        WordReader in = new WordReader(new FileInputStream(file), littleEndian);
        int wordsWritten = 0;
        try {
            int word;
            while(true) {
                word = in.readWord();
                if(word == -1)
                    break;
                out.writeWord(word);
                wordsWritten++;
            }
        } finally {
            in.close();
        }
        if(wordsWritten != wordsize) {
            throw new IOException("File size of "+file+" changed unexpectedly");
        }
    }
}

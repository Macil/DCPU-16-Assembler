package com.minesnap.dcpu.assembler;

import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.lang.StringBuilder;

public class ASMTokenizer {
    private ASMTokenizer() {}

    // These characters separate tokens but aren't tokens themselves.
    private static final String spaces = " \t\r\n";

    // These characters separate tokens and are tokens themselves.
    private static final String borders = "[],+*";

    private static boolean is_space(char c) {
        return spaces.indexOf(c) != -1;
    }

    private static boolean is_border(char c) {
        return borders.indexOf(c) != -1;
    }

    public static List<Token> tokenize(Scanner input, String filename) {
        List<Token> tokens = new ArrayList<Token>();
        int lineNumber = 1;
        // Let the Scanner handle file encoding.
        while(input.hasNextLine()) {
            String line = input.nextLine();
            StringBuilder tokenBuilder = null;
            boolean inDoubleQuotes = false;
            boolean ignoreNextSymbol = false;
            for(int i=0; i<line.length(); i++) {
                char c = line.charAt(i);
                if(c == '\'' && !ignoreNextSymbol && !inDoubleQuotes) {
                        if(tokenBuilder != null)
                            tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
                        tokenBuilder = null;
                        c = line.charAt(++i);
                        if(c == '\\') {
                            c = line.charAt(++i);
                        }
                        tokens.add(new Token(Integer.toString((int)c), filename, lineNumber));
                        c = line.charAt(++i);
                        assert(c == '\'');
                        continue;
                }
                if(c == '"' && !ignoreNextSymbol) {
                    if(!inDoubleQuotes) {
                        if(tokenBuilder != null)
                            tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
                        tokenBuilder = new StringBuilder();
                        inDoubleQuotes = true;
                    } else {
                        inDoubleQuotes = false;
                        tokenBuilder.append(c);
                        tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
                        tokenBuilder = null;
                        continue;
                    }
                }
                if(c == '\\' && !ignoreNextSymbol) {
                    ignoreNextSymbol = true;
                    continue;
                } else {
                    ignoreNextSymbol = false;
                }
                if(inDoubleQuotes) {
                    tokenBuilder.append(c);
                    continue;
                }
                if(c == ';')
                    break;
                if(is_space(c) || is_border(c)) {
                    if(tokenBuilder != null) {
                        tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
                        tokenBuilder = null;
                    }
                }
                if(!is_space(c)) {
                    if(tokenBuilder==null)
                        tokenBuilder = new StringBuilder(8);
                    tokenBuilder.append(c);
                }
                if(is_border(c)) {
                    tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
                    tokenBuilder = null;
                }
            }
            if(tokenBuilder != null)
                tokens.add(new Token(tokenBuilder.toString(), filename, lineNumber));
            tokens.add(new Token("\n", filename, lineNumber));
            lineNumber++;
        }
        return tokens;
    }
}

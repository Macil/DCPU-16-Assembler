package com.minesnap.dcpu.assembler;

import java.util.List;
import java.util.LinkedList;
import java.io.Reader;
import java.io.IOException;
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

    public static Token createToken(String s, String sourceFile, int lineNumber) {
        assert(s.charAt(0) != '"');
        if(s.charAt(0) == ':' || s.charAt(s.length()-1) == ':') {
            String name;
            if(s.charAt(0) == ':') {
                name = s.substring(1);
            } else {
                name = s.substring(0, s.length()-1);
            }
            return new LabelToken(s, name, sourceFile, lineNumber);
        }
        return new Token(s, sourceFile, lineNumber);
    }

    // Returns true if a token was added
    private static boolean clearBuilder(StringBuilder builder, List<Token> tokens, String sourceFile, int lineNumber) {
        if(builder.length() > 0) {
            tokens.add(createToken(builder.toString(), sourceFile, lineNumber));
            return true;
        } else {
            return false;
        }
    }

    public static List<Token> tokenize(Reader input, String filename)
        throws TokenizeError, IOException {
        List<Token> tokens = new LinkedList<Token>();
        int lineNumber = 1;
        StringBuilder tokenBuilder = new StringBuilder();

        while(true) {
	    int read = input.read();
	    if(read == -1) {
                // EOF
                if(clearBuilder(tokenBuilder, tokens, filename, lineNumber))
                    tokenBuilder = new StringBuilder();
                tokens.add(new SymbolToken("\n", filename, lineNumber));
                break;
	    }
	    char c = (char)read;

	    if(c == ';') {
                if(clearBuilder(tokenBuilder, tokens, filename, lineNumber))
                    tokenBuilder = new StringBuilder();
                while(true) {
                    read = input.read();
                    if(read == -1 || read == '\n') {
                        tokens.add(new SymbolToken("\n", filename, lineNumber));
                        lineNumber++;
                        break;
                    }
                }
            } else if(is_space(c)) {
                if(clearBuilder(tokenBuilder, tokens, filename, lineNumber))
                    tokenBuilder = new StringBuilder();
                if(c == '\n') {
                    tokens.add(new SymbolToken("\n", filename, lineNumber));
                    lineNumber++;
                }
            } else if(is_border(c)) {
                if(clearBuilder(tokenBuilder, tokens, filename, lineNumber))
                    tokenBuilder = new StringBuilder();
                tokens.add(new SymbolToken(Character.toString(c), filename, lineNumber));
            } else if(c == '"') {
                if(tokenBuilder.length() != 0) {
                    throw new TokenizeError("String may not begin inside token", filename, lineNumber);
                }

                tokenBuilder.append(c);

                int quoteStartLine = lineNumber;
                boolean nextCharEscaped = false;
                StringBuilder valueBuilder = new StringBuilder();

                while(true) {
                    read = input.read();
                    if(read == -1) {
                        throw new TokenizeError("Unexpected EOF", filename, lineNumber);
                    }

                    c = (char)read;
                    tokenBuilder.append(c);
                    if(!nextCharEscaped) {
                        if(c == '"') {
                            break;
                        } else if(c == '\n') {
                            throw new TokenizeError("Unexpected newline (should it be escaped?)", filename, lineNumber);
                        } else if(c == '\\') {
                            nextCharEscaped = true;
                        } else {
                            valueBuilder.append(c);
                        }
                    } else {
                        nextCharEscaped = false;
                        switch(c) {
                        case '\n':
                            lineNumber++;
                            // Fall through
                        case '"':
                        case '\'':
                        case '\\':
                            valueBuilder.append(c);
                            break;
                        case 'n':
                            valueBuilder.append('\n');
                            break;
                        case 't':
                            valueBuilder.append('\t');
                            break;
                        case 'r':
                            valueBuilder.append('\r');
                            break;
                        case '0':
                            valueBuilder.append('\0');
                            break;
                        default:
                            throw new TokenizeError("Unknown escape code", filename, lineNumber);
                        }
                    }
                }
                tokens.add(new StringToken(tokenBuilder.toString(), valueBuilder.toString(), filename, quoteStartLine));
                tokenBuilder = new StringBuilder();
            } else {
                tokenBuilder.append(c);
            }
        }
        return tokens;
    }
}

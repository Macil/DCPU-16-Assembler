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
    private static boolean is_space(char c) {
        return spaces.indexOf(c) != -1;
    }

    // These characters separate tokens and are tokens themselves.
    private static final String borders = "[],+*";
    private static boolean is_border(char c) {
        return borders.indexOf(c) != -1;
    }

    private static final String digits = "0123456789";
    private static boolean is_digit(char c) {
        return digits.indexOf(c) != -1;
    }

    private static final String label_bad_chars = "+*-/\\,:; \r\t\n[](){}\"`'";
    public static boolean is_legal_label(String label) {
        if(label == null)
            throw new IllegalArgumentException("Label can't be null");

        int len = label.length();
        for(int i=0; i<len; i++) {
            if(label_bad_chars.indexOf(label.charAt(i)) != -1)
                return false;
        }
        return true;
    }

    public static Token createToken(String s, String sourceFile, int lineNumber)
        throws TokenizeError {
        assert(s.charAt(0) != '"');
        char c = s.charAt(0);
        if(c == '\'') {
            if(s.charAt(s.length()-1) != '\'') {
                throw new TokenizeError("Invalid single character string", sourceFile, lineNumber);
            }
            c = s.charAt(1);
            int value;
            if(c != '\\') {
                if(s.length() != 3) {
                    throw new TokenizeError("Invalid single character string", sourceFile, lineNumber);
                }
                value = c;
            } else {
                if(s.length() != 4) {
                    throw new TokenizeError("Invalid single character string", sourceFile, lineNumber);
                }
                c = s.charAt(2);
                Character escaped = escapeChar(c);
                if(escaped == null) {
                    throw new TokenizeError("Invalid escape code", sourceFile, lineNumber);
                }
                value = escaped;
            }
            return new IntToken(s, value, sourceFile, lineNumber);
        }

        s = s.toUpperCase();

        if(c == ':' || s.charAt(s.length()-1) == ':') {
            String name;
            if(s.charAt(0) == ':') {
                name = s.substring(1);
            } else {
                name = s.substring(0, s.length()-1);
            }
            if(!is_legal_label(name)) {
                throw new TokenizeError("Invalid label name", sourceFile, lineNumber);
            }
            return new LabelToken(s, name, sourceFile, lineNumber);
        }
        if(c == '-' || is_digit(c)) {
            if(s.equals("--SP")) {
                return new NameToken(s, sourceFile, lineNumber);
            }
            boolean negative = false;
            if(c == '-') {
                negative = true;
                s = s.substring(1);
            }
            int value;
            try {
                if(s.startsWith("0X") || s.startsWith("0H")) {
                    value = Integer.parseInt(s.substring(2), 16);
                } else if(s.startsWith("0B")) {
                    value = Integer.parseInt(s.substring(2), 2);
                } else if(s.startsWith("0O")) {
                    value = Integer.parseInt(s.substring(2), 8);
                } else {
                    value = Integer.parseInt(s, 10);
                }
            } catch (NumberFormatException e) {
                throw new TokenizeError("Invalid integer", sourceFile, lineNumber);
            }
            if(negative) {
                value = -value;
            }
            int hibyte = value & 0xffff0000;
            if(hibyte != 0) {
                // We need to make sure that number isn't just negative
                // and within bounds.
                if(hibyte != 0xffff0000 || (value & 0x8000) != 0x8000) {
                    throw new TokenizeError("Number can't fit in two byte word", sourceFile, lineNumber);
                }
            }
            value &= 0xffff;
            return new IntToken(s, value, sourceFile, lineNumber);
        }
        return new NameToken(s, sourceFile, lineNumber);
    }

    // Returns true if a token was added
    private static boolean clearBuilder(StringBuilder builder, List<Token> tokens,
                                        String sourceFile, int lineNumber)
        throws TokenizeError {
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
                        if(c == '\n') {
                            lineNumber++;
                        }
                        nextCharEscaped = false;
                        Character value = escapeChar(c);
                        if(value == null)
                            throw new TokenizeError("Invalid escape code", filename, lineNumber);
                        valueBuilder.append(value);
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

    public static Character escapeChar(char c) {
        switch(c) {
        case '\n':
        case '"':
        case '\'':
        case '\\':
            return c;
        case 'n':
            return '\n';
        case 't':
            return '\t';
        case 'r':
            return '\r';
        case '0':
            return '\0';
        default:
            return null;
        }
    }
}

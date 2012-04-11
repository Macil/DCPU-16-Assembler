package com.minesnap.dcpu.assembler;

import java.lang.StringBuilder;
import java.io.IOException;
import java.io.OutputStream;

public class Util {
    private Util() {}

    public static String escapeJava(String s) {
        int length = s.length();
        StringBuilder sb = new StringBuilder(length*2);

        for(int i=0; i<length; i++) {
            char c = s.charAt(i);
            if(c > 0xfff) {
                sb.append("\\u"+hex(c));
            } else if(c > 0xff) {
                sb.append("\\u0"+hex(c));
            } else if(c > 0x7f) {
                sb.append("\\u00"+hex(c));
            } else if(c < 32) {
                switch(c) {
                case '\b':
                    sb.append("\\b");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c > 0xf)
                        sb.append("\\u00"+hex(c));
                    else
                        sb.append("\\u000"+hex(c));
                }
            } else {
                switch(c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public static String hex(char c) {
        return Integer.toHexString(c).toUpperCase();
    }
}

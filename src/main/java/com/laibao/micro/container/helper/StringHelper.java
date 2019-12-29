package com.laibao.micro.container.helper;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * StringHelper
 * @since 1.0
 */
public interface StringHelper {

    /**
     * Get the String of Throwable, like the output of {@link Throwable#printStackTrace()}.
     *
     * @param throwable the input throwable.
     * @return String
     */
    static String toString(Throwable throwable) {
        return toString(null, throwable);
    }

    /**
     * Get the String of Throwable, like the output of {@link Throwable#printStackTrace()}.
     *
     * @param head      the head line of message.
     * @param throwable the input throwable.
     * @return String
     */
    static String toString(String head, Throwable throwable) {
        StringWriter w = new StringWriter(1024);
        if (head != null) w.write(head + "\n");
        PrintWriter p = new PrintWriter(w);
        try {
            throwable.printStackTrace(p);
            return w.toString();
        } finally {
            p.close();
        }
    }

    /**
     *
     * @param str
     * @return boolean
     */
    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    /**
     *
     * @param str
     * @return boolean
     */
    static boolean isBlank(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * convert CamelCase string to dot-split lowercase string.
     * <p/>
     * eg: convert <code>LessIsMore</code> to <code>less.is.more</code>
     * @param input
     * @return String
     */
    static String toDotSpiteString(String input) {
        char[] charArray = input.toCharArray();
        StringBuilder sb = new StringBuilder(128);
        for (int i = 0; i < charArray.length; i++) {
            if (Character.isUpperCase(charArray[i])) {
                if (i != 0 && charArray[i - 1] != '.') {
                    sb.append('.');
                }
                sb.append(Character.toLowerCase(charArray[i]));
            } else {
                sb.append(charArray[i]);
            }
        }
        return sb.toString();
    }

    /**
     * name => getName
     * @param attribute
     * @return String
     */
    static String attribute2Getter(String attribute) {
        return "get" + attribute.substring(0, 1).toUpperCase() + attribute.substring(1);
    }
}

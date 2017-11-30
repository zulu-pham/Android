package com.viettel.bss.viettelpos.vtc.utils;

import android.util.Log;

import com.viettel.bss.viettelpos.vtc.BuildConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by anpd on 11/10/2017.
 */

public class DebugLog {

    private static final int CALL_STACK_INDEX = 1;
    private static final Pattern ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$");

    /**
     * Write log with tag, message
     */
    public static void d(String tag, String message) {
        if (BuildConfig.DEBUG == true) {
            if (tag != null && tag.length() > 0 && message != null && message.length() > 0) {
                Log.d(tag, message);
            }
        }
    }

    /**
     * Write log with ability go to line source in class
     */
    public static void d(String message) {
        if (BuildConfig.DEBUG == true && message != null && message.length() > 0) {
            StackTraceElement[] stackTrace = new Throwable().getStackTrace();
            if (stackTrace.length <= CALL_STACK_INDEX) {
                throw new IllegalStateException("Synthetic stacktrace didn't have enough elements: are you using proguard?");
            }
            String clazz = extractClassName(stackTrace[CALL_STACK_INDEX]);
            int lineNumber = stackTrace[CALL_STACK_INDEX].getLineNumber();

            String tag = ".(" + clazz + ".java:" + lineNumber + ")";
            Log.d(tag, message);
        }
    }

    private static String extractClassName(StackTraceElement element) {
        String tag = element.getClassName();
        Matcher m = ANONYMOUS_CLASS.matcher(tag);
        if (m.find()) {
            tag = m.replaceAll("");
        }
        return tag.substring(tag.lastIndexOf('.') + 1);
    }
}

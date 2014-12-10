package cz.muni.fi.ctl.util;

/**
 * Simple logger for most basic message handling
 */
@SuppressWarnings("UnusedDeclaration")
public class Log {
    public static void d(String message) {
        System.out.println(message);
    }
    public static void i(String message) {
        System.out.println(message);
    }
    public static void e(String message) {
        System.err.println(message);
    }
    public static void w(String message) {
        System.err.println(message);
    }
}

package com.dell.gumshoe.util;

import java.util.Properties;


public class Output {
    private static boolean verbose;

    public static void setVerbose(boolean value) { verbose = value; }
    public static void configure(Configuration config) {
        setVerbose(config.isTrue("gumshoe.verbose", false));
    }
    public static void configure(Properties p) {
        setVerbose(Boolean.valueOf(p.getProperty("gumshoe.verbose", "false")));
    }

    /////

    public static void print(String before, String... message) {
        final StringBuilder msg = new StringBuilder();
        msg.append(before);
        for(String part : message) {
            msg.append(" ").append(part);
        }
        System.out.println(msg.toString());
    }

    public static void error(Throwable t, String... message) {
        print("GUMSHOE ERROR!", message);
        t.printStackTrace();
    }

    public static void error(String... message) {
        print("GUMSHOE ERROR!", message);
    }

    public static void debug(String... message) {
        if(verbose) {
            print("GUMSHOE:", message);
        }
    }
}

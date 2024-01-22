package io.polyapi.knative.function.log;

import java.io.OutputStream;
import java.io.PrintStream;

public class PolyLogStream extends PrintStream {
    private final String logLevel;
    private final boolean enabled;

    public PolyLogStream(OutputStream out, String logLevel, boolean enabled) {
        super(out);
        this.logLevel = logLevel;
        this.enabled = enabled;
    }

    @Override
    public void print(boolean b) {
        print(String.valueOf(b));
    }

    @Override
    public void print(char c) {
        print(String.valueOf(c));
    }

    @Override
    public void print(int i) {
        print(String.valueOf(i));
    }

    @Override
    public void print(long l) {
        print(String.valueOf(l));
    }

    @Override
    public void print(float f) {
        print(String.valueOf(f));
    }

    @Override
    public void print(double d) {
        print(String.valueOf(d));
    }

    @Override
    public void print(char[] s) {
        print(String.valueOf(s));
    }

    @Override
    public void print(Object obj) {
        print(String.valueOf(obj));
    }

    @Override
    public void print(String s) {
        if (enabled) {
            super.print(formatMessage(s));
        }
    }

    private String formatMessage(String message) {
        return String.format("[%s]%s[/%s]", logLevel, message, logLevel);
    }
}

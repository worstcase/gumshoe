package com.dell.gumshoe.file.io;

public interface FileMatcher {
    boolean matches(String path);

    public static final FileMatcher ANY = new FileMatcher() {
        @Override
        public boolean matches(String path) {
            return true;
        }

    };
}
